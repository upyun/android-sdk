package com.upyun.library.common;

import com.upyun.library.exception.UpYunException;
import com.upyun.library.utils.Base64Coder;
import com.upyun.library.utils.UpYunUtils;

import okhttp3.*;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

    private boolean interrupt;
    private Call currentCall;

    private OnProgressListener onProgressListener;

    private OnInterruptListener onInterruptListener;

    private int totalBlock;

    private boolean executed;

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
            this.interrupt = false;
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
            this.interrupt = false;
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
     * 中断上传
     */
    public void interrupt(OnInterruptListener listener) {

        this.onInterruptListener = listener;
        interrupt = true;
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

        this.interrupt = false;

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
    public void setOnProgressListener(OnProgressListener onProgressListener) {
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

        String sign = sign("PUT", date, uploadPath, bucketName, userName, password, md5);

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
            onProgressListener.onProgress(1, totalBlock);
        }

        return processUpload();
    }

    private boolean processUpload() throws IOException, UpYunException {
        byte[] data = new byte[0];
        while (nextPartIndex >= 0) {

            if (interrupt && onInterruptListener != null) {
                onInterruptListener.OnInterrupt(true);
                onInterruptListener = null;
                return false;
            }

            data = readBlockByIndex(nextPartIndex);

            RequestBody requestBody = RequestBody.create(null, data);

            String date = getGMTDate();

            String md5 = null;

            if (checkMD5) {
                md5 = UpYunUtils.md5(data);
            }

            String sign = sign("PUT", date, uploadPath, bucketName, userName, password, md5);

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
                onProgressListener.onProgress(nextPartIndex + 2, totalBlock);
            }
            callRequest(builder.build());
        }

        return completeUpload();
    }

    private boolean completeUpload() throws IOException, UpYunException {

        if (interrupt && onInterruptListener != null) {
            onInterruptListener.OnInterrupt(true);
            onInterruptListener = null;
            return false;
        }

        RequestBody requestBody = RequestBody.create(null, mFile);

        String date = getGMTDate();

        String md5 = null;

        if (checkMD5) {
            md5 = UpYunUtils.md5(mFile, BLOCK_SIZE);
        }

        String sign = sign("PUT", date, uploadPath, bucketName, userName, password, md5);

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
            onProgressListener.onProgress(totalBlock, totalBlock);
        }

        uuid = null;
        return true;
    }

    private void callRequest(Request request) throws IOException, UpYunException {

        currentCall = mClient.newCall(request);

        Response response = currentCall.execute();
        if (!response.isSuccessful()) {
            uuid = null;
            throw new UpYunException(response.body().string());
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

    private String sign(String method, String date, String path, String bucket, String userName, String password, String md5) throws UpYunException {

        StringBuilder sb = new StringBuilder();
        String sp = "&";
        sb.append(method);
        sb.append(sp);
        sb.append("/" + bucket + path);

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
            return "UpYun " + userName + ":" + Base64Coder.encodeLines(hmac);
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

    public interface OnProgressListener {
        void onProgress(int index, int total);
    }

    public interface OnInterruptListener {
        void OnInterrupt(boolean interrupted);
    }
}
