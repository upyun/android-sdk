package com.upyun.library.common;

import android.util.Log;

import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.AsyncRun;
import com.upyun.library.utils.Base64Coder;
import com.upyun.library.utils.UpYunUtils;

import okhttp3.*;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResumeUploader {

    private static final String AUTHORIZATION = "Authorization";
    private static final int BLOCK_SIZE = 1024 * 1024;

    private final String DATE = "Date";

    private static final String CONTENT_MD5 = "Content-MD5";
    private static final String CONTENT_TYPE = "CContent-Type";
    private static final String CONTENT_SECRET = "Content-Secret";
    private static final String X_Upyun_Meta_X = "X-Upyun-Meta-X";

    private static final String X_UPYUN_MULTI_STAGE = "X-Upyun-Multi-Stage";
    private static final String X_UPYUN_MULTI_TYPE = "X-Upyun-Multi-Type";
    private static final String X_UPYUN_MULTI_LENGTH = "X-Upyun-Multi-Length";
    private static final String X_UPYUN_META_X = "X-Upyun-Meta-X";
    private static final String X_UPYUN_MULTI_UUID = "X-Upyun-Multi-UUID";
    private static final String X_UPYUN_PART_ID = "X-Upyun-Part-ID";
    private static final String X_UPYUN_NEXT_PART_ID = "X-Upyun-Next-Part-ID";
    private static final String HOST = "http://v0.api.upyun.com";

    private String uuid;
    private String uploadPath;
    private OkHttpClient mClient;
    private File mFile;
    private int nextPartIndex;
    private RandomAccessFile randomAccessFile;

    private boolean checkMD5;

    // 空间名
    protected String bucketName = null;
    // 操作员名
    protected String userName = null;
    // 操作员密码
    protected String password = null;
    //超时设置(s)
    private int timeout = 20;

    private String url;

    private Call currentCall;

    private UpProgressListener onProgressListener;

    private OnInterruptListener onInterruptListener;

    private int totalBlock;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 断点续传
     *
     * @return 是否上传成功
     * @throws IOException
     */
    public boolean resume() throws IOException, UpYunException {
        if (uuid == null) {
            throw new UpYunException("uuid is null, please restart!");
        } else {
            return processUpload();
        }
    }

    /**
     * 断点续传
     *
     * @param uuid           上传任务 uuid
     * @param newxtPartIndex 下一个上传分块 index
     * @return
     * @throws IOException
     */
    public boolean resume(String uuid, int newxtPartIndex) throws IOException, UpYunException {

        this.uuid = uuid;
        this.nextPartIndex = newxtPartIndex;

        if (uuid == null) {
            throw new UpYunException("uuid is null, please restart!");
        } else {
            return processUpload();
        }
    }

    /**
     * 初始化 ResumeUploader
     *
     * @param bucketName 空间名称
     * @param userName   操作员名称
     * @param password   密码，需要MD5加密
     * @return ResumeUploader object
     */
    public ResumeUploader(String bucketName, String userName, String password) {
        this.bucketName = bucketName;
        this.userName = userName;
        this.password = password;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 开始上传
     *
     * @param file       本地上传文件
     * @param uploadPath 上传服务器路径
     * @param params     通用上传参数（见 rest api 文档）
     * @return 是否上传成功
     * @throws IOException
     */
    public boolean upload(File file, String uploadPath, Map<String, String> params) throws IOException, UpYunException {

        this.mFile = file;

        this.totalBlock = (int) Math.ceil(mFile.length() / (double) BLOCK_SIZE + 2);

        this.randomAccessFile = new RandomAccessFile(mFile, "r");

        this.uploadPath = uploadPath;

        this.url = HOST + "/" + bucketName + uploadPath;

        this.mClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();

        return startUpload(params);
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
            public void onComplete(final boolean isSuccess, final String result) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (completeListener != null) {
                            completeListener.onComplete(isSuccess, result);
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

                    String sign = sign("POST", date, "/pretreatment/", userName, password, null);

                    processParam.put(Params.TASKS, Base64Coder.encodeString(processParam.get(Params.TASKS).toString()));

                    FormBody.Builder builder = new FormBody.Builder();

                    for (Map.Entry<String, Object> mapping : processParam.entrySet()) {
                        builder.addEncoded(mapping.getKey(), mapping.getValue().toString());
                    }

                    String processURL = "http://p0.api.upyun.com/pretreatment/";
                    Request request = new Request.Builder()
                            .url(processURL)
                            .post(builder.build())
                            .header(DATE, date)
                            .header(AUTHORIZATION, sign)
                            .header("User-Agent", UpYunUtils.VERSION)
                            .build();

                    Response response = mClient.newCall(request).execute();

                    if (!response.isSuccessful()) {
                        uiCompleteListener.onComplete(false, response.body().string());
                    } else {
                        uiCompleteListener.onComplete(true, response.body().string());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, e.toString());
                } catch (UpYunException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, e.toString());
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
            public void onComplete(final boolean isSuccess, final String result) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (completeListener != null) {
                            completeListener.onComplete(isSuccess, result);
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
                    uiCompleteListener.onComplete(true, null);
                } catch (IOException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, e.toString());
                } catch (UpYunException e) {
                    e.printStackTrace();
                    uiCompleteListener.onComplete(false, e.toString());
                }
            }
        });
    }

    /**
     * 获取 uuid
     *
     * @return uuid
     */
    public String getUuid() {
        return uuid;
    }


    /**
     * 获取下一个上传分块 index
     *
     * @return index
     */
    public int getNextPartIndex() {
        return nextPartIndex;
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

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setNextPartIndex(int nextPartIndex) {
        this.nextPartIndex = nextPartIndex;
    }

    private boolean startUpload(Map<String, String> params) throws IOException, UpYunException {

        if (uuid != null) {
            return processUpload();
        }

        RequestBody requestBody = RequestBody.create(null, "");

        String date = getGMTDate();

        String md5 = null;

        if (checkMD5) {
            md5 = UpYunUtils.md5("");
        }

        String sign = sign("PUT", date, "/" + bucketName + uploadPath, userName, password, md5);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header(DATE, date)
                .header(AUTHORIZATION, sign)
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
        callRequest(builder.build());

        if (onProgressListener != null) {
            onProgressListener.onRequestProgress(1, totalBlock);
        }

        return processUpload();
    }

    private boolean processUpload() throws IOException, UpYunException {
        byte[] data = new byte[0];
        while (nextPartIndex >= 0) {

            data = readBlockByIndex(nextPartIndex);

            RequestBody requestBody = RequestBody.create(null, data);

            String date = getGMTDate();

            String md5 = null;

            if (checkMD5) {
                md5 = UpYunUtils.md5(data);
            }

            String sign = sign("PUT", date, "/" + bucketName + uploadPath, userName, password, md5);

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header(DATE, date)
                    .header(AUTHORIZATION, sign)
                    .header(X_UPYUN_MULTI_STAGE, "upload")
                    .header(X_UPYUN_MULTI_UUID, uuid)
                    .header(X_UPYUN_PART_ID, nextPartIndex + "")
                    .header("User-Agent", UpYunUtils.VERSION)
                    .put(requestBody);

            if (md5 != null) {
                builder.header(CONTENT_MD5, md5);
            }

            if (onProgressListener != null) {
                onProgressListener.onRequestProgress(nextPartIndex + 2, totalBlock);
            }
            callRequest(builder.build());
        }

        return completeUpload();
    }

    private boolean completeUpload() throws IOException, UpYunException {

        RequestBody requestBody = RequestBody.create(null, "");

        String date = getGMTDate();

        String md5 = null;

        if (checkMD5) {
            md5 = UpYunUtils.md5("");
        }

        String sign = sign("PUT", date, "/" + bucketName + uploadPath, userName, password, md5);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .header(DATE, date)
                .header(AUTHORIZATION, sign)
                .header(X_UPYUN_MULTI_STAGE, "complete")
                .header(X_UPYUN_MULTI_UUID, uuid)
                .header("User-Agent", UpYunUtils.VERSION)
                .put(requestBody);

        if (md5 != null) {
            builder.header(CONTENT_MD5, md5);
        }

        callRequest(builder.build());

        if (onProgressListener != null) {
            onProgressListener.onRequestProgress(totalBlock, totalBlock);
        }

        uuid = null;
        return true;
    }

    private void callRequest(Request request) throws IOException, UpYunException {

        currentCall = mClient.newCall(request);

        Response response = currentCall.execute();
        if (!response.isSuccessful()) {
            int x_error_code = Integer.parseInt(response.header("X-Error-Code", "-1"));
            Log.e("x_error_code", "::" + x_error_code);
            if (x_error_code != 40011061 && x_error_code != 40011059) {
                uuid = null;
                throw new UpYunException(response.body().string());
            } else {
                nextPartIndex = Integer.parseInt(response.header(X_UPYUN_NEXT_PART_ID, "-2"));
                return;
            }
        }

        uuid = response.header(X_UPYUN_MULTI_UUID, "");
        nextPartIndex = Integer.parseInt(response.header(X_UPYUN_NEXT_PART_ID, "-2"));
    }

    /**
     * 获取 GMT 格式时间戳
     *
     * @return GMT 格式时间戳
     */
    private String getGMTDate() {
        SimpleDateFormat formater = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        formater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formater.format(new Date());
    }

    private String sign(String method, String date, String path, String userName, String password, String md5) throws UpYunException {

        StringBuilder sb = new StringBuilder();
        String sp = "&";
        sb.append(method);
        sb.append(sp);
//        sb.append("/" + bucket + path);
        sb.append(path);

        sb.append(sp);
        sb.append(date);

        if (md5 != null) {
            sb.append(sp);
            sb.append(md5);
        }
        String raw = sb.toString().trim();
        byte[] hmac = null;
        try {
            hmac = UpYunUtils.calculateRFC2104HMACRaw(password, raw);
        } catch (Exception e) {
            throw new UpYunException("calculate SHA1 wrong.");
        }

        if (hmac != null) {
            return "UPYUN " + userName + ":" + Base64Coder.encodeLines(hmac);
        }

        return null;
    }

    private byte[] readBlockByIndex(int index) throws IOException {
        byte[] block = new byte[BLOCK_SIZE];
        int readedSize = 0;
        int offset = index * BLOCK_SIZE;
        randomAccessFile.seek(offset);
        readedSize = randomAccessFile.read(block, 0, BLOCK_SIZE);

        // read last block, adjust byte size
        if (readedSize < BLOCK_SIZE) {
            byte[] notFullBlock = new byte[readedSize];
            System.arraycopy(block, 0, notFullBlock, 0, readedSize);
            return notFullBlock;
        }
        return block;
    }

    public interface OnInterruptListener {
        void OnInterrupt(boolean interrupted);
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
