package com.upyun.library.common;

import com.upyun.library.exception.RespException;
import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.AsyncRun;
import com.upyun.library.utils.Base64Coder;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class BaseUploader {

    int nextPartIndex;
    static final String AUTHORIZATION = "Authorization";
    final static String BACKSLASH = "/";
    static final int BLOCK_SIZE = 1024 * 1024;

    final String DATE = "Date";

    static final String CONTENT_MD5 = "Content-MD5";
    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_SECRET = "Content-Secret";
    static final String X_Upyun_Meta_X = "X-Upyun-Meta-X";

    static final String X_UPYUN_MULTI_DISORDER = "X-Upyun-Multi-Disorder";
    static final String X_UPYUN_MULTI_STAGE = "X-Upyun-Multi-Stage";
    static final String X_UPYUN_MULTI_TYPE = "X-Upyun-Multi-Type";
    static final String X_UPYUN_MULTI_LENGTH = "X-Upyun-Multi-Length";
    static final String X_UPYUN_MULTI_UUID = "X-Upyun-Multi-UUID";
    static final String X_UPYUN_PART_ID = "X-Upyun-Part-ID";
    static final String X_UPYUN_NEXT_PART_ID = "X-Upyun-Next-Part-ID";

    static final String HOST = "https://v0.api.upyun.com";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    Map<String, String> params;

    volatile boolean paused;

    //服务端签名
    boolean signed;

    String uuid;
    String uri;
    OkHttpClient mClient;
    private File mFile;
    RandomAccessFile randomAccessFile;

    boolean checkMD5;

    // 空间名
    String bucketName;
    // 操作员名
    String userName;
    // 操作员密码
    String password;
    //超时设置(s)
    int timeout = 20;

    String url;

    UpProgressListener onProgressListener;

    int totalBlock;

    // 请求日期时间
    String date = null;
    // Authorization 签名
    String signature = null;

    /**
     * 断点续传
     *
     * @return 服务器返回结果
     * @throws IOException
     * @throws UpYunException
     */
    public Response resume() throws IOException, UpYunException {
        this.paused = false;
        return startUpload();
    }


    /**
     * 初始化 SerialUploader
     *
     * @param bucketName 空间名称
     * @param userName   操作员名称
     * @param password   密码，需要MD5加密
     */
    BaseUploader(String bucketName, String userName, String password) {
        this.bucketName = bucketName;
        this.userName = userName;
        this.password = password;
    }

    BaseUploader() {
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void pause() {
        this.paused = true;
    }

    void init(File file, String uploadPath, Map<String, String> params) {

        this.signed = false;

        this.paused = false;

        if (params == null) {
            params = new HashMap<>();
        }

        this.params = params;

        this.mFile = file;

        this.totalBlock = (int) Math.ceil(mFile.length() / (double) BLOCK_SIZE + 2);

        if (uploadPath.startsWith(BACKSLASH)) {
            this.uri = BACKSLASH + bucketName + BACKSLASH + URLEncoder.encode(uploadPath.substring(1));
        } else {
            this.uri = BACKSLASH + bucketName + BACKSLASH + URLEncoder.encode(uploadPath);
        }

        this.url = HOST + uri;

        this.mClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
//                .addInterceptor(new LogInterceptor())
                .build();
    }

    void init(File file, String uri, String date, String signature, Map<String, String> params) {

        this.signed = true;

        this.uri = uri;

        this.signature = signature;

        this.paused = false;

        if (params == null) {
            params = new HashMap<>();
        }

        this.params = params;

        this.mFile = file;

        this.date = date;

        this.totalBlock = (int) Math.ceil(mFile.length() / (double) BLOCK_SIZE + 2);

        this.url = HOST + uri;

        this.mClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 开始上传
     *
     * @param file       本地上传文件
     * @param uploadPath 上传服务器路径
     * @param params     通用上传参数（见 rest api 文档）
     * @return 服务器返回结果
     * @throws IOException
     * @throws UpYunException
     */
    abstract Response upload(File file, String uploadPath, Map<String, String> params) throws IOException, UpYunException;

    /**
     * 开始上传(服务器签名)
     *
     * @param file      本地上传文件
     * @param uri       请求路径
     * @param date      请求日期时间
     * @param signature 签名
     * @param params    通用上传参数（见 rest api 文档）
     * @return 服务器返回结果
     * @throws IOException
     */
    abstract Response upload(File file, String uri, String date, String signature, Map<String, String> params) throws IOException, UpYunException, ExecutionException, InterruptedException;

    /**
     * 获取 uuid
     *
     * @return uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * 设置 uuid
     *
     * @param uuid
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * 设置是否 MD5 校验
     *
     * @param checkMD5 是否 MD5 校验
     */
    public void setCheckMD5(boolean checkMD5) {
        this.checkMD5 = checkMD5;
    }

    /**
     * 设置上传进度监听
     *
     * @param onProgressListener 上传进度 listener
     */
    public void setOnProgressListener(UpProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    Response startUpload() throws IOException, UpYunException {

        if (paused) {
            throw new UpYunException("upload paused");
        }

        if (uuid == null) {
            RequestBody requestBody = RequestBody.create(null, "");

//            String date = getGMTDate();

            String md5 = null;

            if (checkMD5) {
                md5 = UpYunUtils.md5("");
            }

//            String sign = UpYunUtils.sign("PUT", date, uri, userName, password, md5).trim();

            if (!signed) {
                date = getGMTDate();
                signature = UpYunUtils.sign("PUT", date, uri, userName, password, md5).trim();
            }

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header(DATE, date)
                    .header(AUTHORIZATION, signature)
                    .header(X_UPYUN_MULTI_STAGE, "initiate")
                    .header(X_UPYUN_MULTI_TYPE, "application/octet-stream")
                    .header(X_UPYUN_MULTI_LENGTH, mFile.length() + "")
                    .header("User-Agent", UpYunUtils.VERSION)
                    .put(requestBody);

            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }

            if (md5 != null) {
                builder.header(CONTENT_MD5, md5);
            }
            callRequest(builder.build(), 1);
        }

        if (this.randomAccessFile == null) {
            this.randomAccessFile = new RandomAccessFile(mFile, "r");
        }

        return processUpload();
    }

    abstract Response processUpload() throws IOException, UpYunException;

    abstract Response completeUpload() throws IOException, UpYunException;

    Response completeRequest() throws UpYunException, IOException {

        if (randomAccessFile != null) {
            randomAccessFile.close();
            randomAccessFile = null;
        }

        RequestBody requestBody = RequestBody.create(null, "");

        String md5 = null;

        if (checkMD5) {
            md5 = UpYunUtils.md5("");
        }

        if (!signed) {
            date = getGMTDate();
            signature = UpYunUtils.sign("PUT", date, uri, userName, password, md5).trim();
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header(DATE, date)
                .header(AUTHORIZATION, signature)
                .header(X_UPYUN_MULTI_STAGE, "complete")
                .header(X_UPYUN_MULTI_UUID, uuid)
                .header("User-Agent", UpYunUtils.VERSION)
                .put(requestBody);

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (md5 != null) {
            builder.header(CONTENT_MD5, md5);
        }
        return callRequest(builder.build(), totalBlock);
    }

    private Response callRequest(Request request, int index) throws IOException, UpYunException {

        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            uuid = null;
            throw new RespException(response.code(), response.body().string());
        } else {
            if (onProgressListener != null) {
                onProgressListener.onRequestProgress(index, totalBlock);
            }
        }
        uuid = response.header(X_UPYUN_MULTI_UUID, "");
        nextPartIndex = Integer.parseInt(response.header(X_UPYUN_NEXT_PART_ID, "-2"));
        return response;
    }

    /**
     * 获取 GMT 格式时间戳
     *
     * @return GMT 格式时间戳
     */
    String getGMTDate() {
        SimpleDateFormat format = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date());
    }

    byte[] readBlockByIndex(int index) throws IOException {
        byte[] block = new byte[BLOCK_SIZE];
        int readSize = 0;
        int offset = index * BLOCK_SIZE;
        randomAccessFile.seek(offset);
        readSize = randomAccessFile.read(block, 0, BLOCK_SIZE);

        // read last block, adjust byte size
        if (readSize < BLOCK_SIZE) {
            byte[] notFullBlock = new byte[readSize];
            System.arraycopy(block, 0, notFullBlock, 0, readSize);
            return notFullBlock;
        }
        return block;
    }

    /**
     * 异步上传（添加视频处理参数）
     *
     * @param file             本地上传文件
     * @param uploadPath       上传服务器路径
     * @param restParams       通用上传参数（见 rest api 文档）
     * @param processParam     异步处理上传参数 （见 云处理 api 文档）
     * @param completeListener 成功失败回调
     */
    public void upload(final File file, final String uploadPath, final Map<String, String> restParams, final Map<String, Object> processParam, final UpCompleteListener completeListener) {

        if (processParam == null) {
            upload(file, uploadPath, restParams, completeListener);
            return;
        }

        final UpCompleteListener uiCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(final boolean isSuccess, final Response response, final Exception error) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (completeListener != null) {
                            completeListener.onComplete(isSuccess, response, error);
                        }
                    }
                });
            }
        };

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    upload(file, uploadPath, restParams);

                    String date = getGMTDate();

                    String sign = UpYunUtils.sign("POST", date, "/pretreatment/", userName, password, null);

                    processParam.put(Params.TASKS, Base64Coder.encodeString(processParam.get(Params.TASKS).toString()));

                    FormBody.Builder builder = new FormBody.Builder();

                    for (Map.Entry<String, Object> mapping : processParam.entrySet()) {
                        builder.addEncoded(mapping.getKey(), mapping.getValue().toString());
                    }

                    String processURL = "https://p0.api.upyun.com/pretreatment/";
                    Request request = new Request.Builder()
                            .url(processURL)
                            .post(builder.build())
                            .header(DATE, date)
                            .header(AUTHORIZATION, sign)
                            .header("User-Agent", UpYunUtils.VERSION)
                            .build();

                    Response response = mClient.newCall(request).execute();

                    if (!response.isSuccessful()) {
                        uiCompleteListener.onComplete(false, null, new RespException(response.code(), response.body().string()));
                    } else {
                        uiCompleteListener.onComplete(true, response, null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                } catch (UpYunException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                }
            }
        });
    }

    /**
     * 异步上传
     *
     * @param file             本地上传文件
     * @param uploadPath       上传服务器路径
     * @param restParams       通用上传参数（见 rest api 文档）
     * @param completeListener 成功失败回调
     */
    public void upload(final File file, final String uploadPath, final Map<String, String> restParams, final UpCompleteListener completeListener) {

        final UpCompleteListener uiCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(final boolean isSuccess, final Response response, final Exception e) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (completeListener != null) {
                            completeListener.onComplete(isSuccess, response, e);
                        }
                    }
                });
            }
        };

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    uiCompleteListener.onComplete(true, upload(file, uploadPath, restParams), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                } catch (UpYunException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                }
            }
        });
    }

    /**
     * 异步上传(服务器签名)
     *
     * @param file             本地上传文件
     * @param uri              请求路径(带空间名)
     * @param date             请求日期时间
     * @param signature        签名
     * @param restParams       通用上传参数（见 rest api 文档）
     * @param completeListener 成功失败回调
     */
    public void upload(final File file, final String uri, final String date, final String signature, final Map<String, String> restParams, final UpCompleteListener completeListener) {

        final UpCompleteListener uiCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(final boolean isSuccess, final Response response, final Exception error) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (completeListener != null) {
                            completeListener.onComplete(isSuccess, response, error);
                        }
                    }
                });
            }
        };

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    uiCompleteListener.onComplete(true, upload(file, uri, date, signature, restParams), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                } catch (UpYunException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, null, e);
                }
            }
        });
    }


    public class Params {
        /**
         * 请求参数
         * <p>
         * bucket_name string  是	文件所在服务名称（空间名称）
         * notify_url	string	是	回调通知地址
         * source  string  是	待处理文件路径
         * tasks	string	是	处理任务信息，详见下
         * accept	string	是	必须指定为 json
         */
        public final static String BUCKET_NAME = "bucket_name";
        public final static String NOTIFY_URL = "notify_url";
        public final static String SOURCE = "source";
        public final static String TASKS = "tasks";
        public final static String ACCEPT = "accept";


        /**
         * 回调通知参数
         * <p>
         * status_code	integer	处理结果状态码，200 表示成功处理
         * path	array	输出文件保存路径
         * description	string	处理结果描述
         * task_id	string	任务对应的 task_id
         * info	string	视频文件的元数据信息。经过 base64 处理过之后的 JSON 字符串，仅当 type 为 video且 return_info 为 true 时返回
         * signature	string	回调验证签名，用户端程序可以通过校验签名，判断回调通知的合法性
         * timestamp	integer	服务器回调此信息时的时间戳
         */
        public final static String STATUS_CODE = "status_code";
        public final static String PATH = "path";
        public final static String DESCRIPTION = "description";
        public final static String TASK_ID = "task_id";
        public final static String INFO = "info";
        public final static String SIGNATURE = "signature";
        public final static String TIMESTAMP = "timestamp";

        /**
         * 查询参数
         * <p>
         * task_ids	string	任务 id 以 , 作为分隔符，最多 20 个
         */
        public final static String TASK_IDS = "task_ids";

        /**
         * 处理通用参数
         * <p>
         * type	string	音视频处理类型。不同的处理任务对应不同的 type，详见下方各处理任务说明
         * save_as	string	输出文件保存路径（同一个空间下），如果没有指定，系统自动生成在同空间同目录下
         * return_info	boolean	是否返回 JSON 格式元数据，默认 false。支持 type 值为 video 功能
         * avopts	string	音视频处理参数, 格式为 /key/value/key/value/...
         */
        public final static String TYPE = "type";
        public final static String SAVE_AS = "save_as";
        public final static String RETURN_INFO = "return_info";
        public final static String AVOPTS = "avopts";

    }
}