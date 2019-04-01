package com.upyun.library.common;

import com.upyun.library.exception.UpYunException;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ParallelUploader extends BaseUploader {

    private volatile int blockProgress;

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    //并行式断点并行数
    private int parallel = 4;

    //分块上传状态 1 成功 2 上传中 3 上传失败
    private int[] status;


    /**
     * 断点续传
     *
     * @param uuid   上传任务 uuid
     * @param status 分块上传状态
     * @return
     * @throws IOException
     */
    public boolean resume(String uuid, int[] status) throws IOException, UpYunException {

        this.uuid = uuid;
        this.status = status;

        if (uuid == null || status == null || status.length != totalBlock - 2) {
            throw new UpYunException("uuid or status is wrong, please restart!");
        } else {
            this.paused = false;
            return startUpload();
        }
    }


    /**
     * 初始化 ParallelUploader
     *
     * @param bucketName 空间名称
     * @param userName   操作员名称
     * @param password   密码，需要MD5加密
     * @return SerialUploader object
     */
    public ParallelUploader(String bucketName, String userName, String password) {
        super(bucketName, userName, password);
    }

    /**
     * 初始化 ParallelUploader
     */
    public ParallelUploader() {
        super();
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

        init(file, uploadPath, params);

        if (status == null || status.length != totalBlock - 2 || uuid == null) {
            status = new int[totalBlock - 2];
        }

        this.params.put(X_UPYUN_MULTI_DISORDER, "true");

        return startUpload();
    }


    /**
     * 开始上传(服务器签名)
     *
     * @param file      本地上传文件
     * @param uri       请求路径
     * @param date      请求日期时间
     * @param signature 签名
     * @param params    通用上传参数（见 rest api 文档）
     * @return 是否上传成功
     * @throws IOException
     */
    @Override
    public boolean upload(File file, String uri, String date, String signature, Map<String, String> params) throws IOException, UpYunException {

        init(file, uri, date, signature, params);

        if (status == null || status.length != totalBlock - 2 || uuid == null) {
            status = new int[totalBlock - 2];
        }

        this.params.put(X_UPYUN_MULTI_DISORDER, "true");

        return startUpload();
    }

    /**
     * 获取分块状态
     *
     * @return
     */
    public int[] getStatus() {
        return status;
    }

    /**
     * 设置分块状态
     *
     * @param status
     */
    public void setStatus(int[] status) {
        this.status = status;
    }

    boolean processUpload() throws IOException, UpYunException {

        blockProgress = 0;

        ExecutorService uploadExecutor = Executors.newFixedThreadPool(parallel);

        for (int i = 0; i < totalBlock - 2; i++) {

            Future future = uploadExecutor.submit(uploadBlock(i));

            try {
                future.get();
            } catch (Exception e) {
                uploadExecutor.shutdown();
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                    randomAccessFile = null;
                }
                throw new UpYunException(e.getMessage());
            }
        }

        uploadExecutor.shutdown();

        try {//等待直到所有任务完成
            uploadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return completeUpload();
    }

    private Runnable uploadBlock(final int index) {
        return new Runnable() {
            public void run() {

                try {
                    if (paused) {
                        throw new UpYunException("upload paused");
                    }
                    if (status[index] == 1) {
                        if (onProgressListener != null) {
                            onProgressListener.onRequestProgress(blockProgress + 2, totalBlock);
                        }
                        blockProgress++;
                        return;
                    } else if (status[index] == 2) {
                        return;
                    }

                    status[index] = 2;

                    byte[] data = readBlockByIndex(index);

                    RequestBody requestBody = RequestBody.create(null, data);

                    String md5 = null;

                    if (checkMD5) {
                        md5 = UpYunUtils.md5(data);
                    }

                    if (!signed) {
                        date = getGMTDate();
                        signature = UpYunUtils.sign("PUT", date, uri, userName, password, md5).trim();
                    }

                    Request.Builder builder = new Request.Builder()
                            .url(url)
                            .header(DATE, date)
                            .header(AUTHORIZATION, signature)
                            .header(X_UPYUN_MULTI_STAGE, "upload")
                            .header(X_UPYUN_MULTI_UUID, uuid)
                            .header(X_UPYUN_PART_ID, index + "")
                            .header("User-Agent", UpYunUtils.VERSION)
                            .put(requestBody);

                    if (md5 != null) {
                        builder.header(CONTENT_MD5, md5);
                    }

                    Response response = uploadRequest(builder);

                    uuid = response.header(X_UPYUN_MULTI_UUID, "");
                    status[index] = 1;
                } catch (Exception e) {
                    status[index] = 3;
                    throw new RuntimeException(e.getMessage());
                }
            }
        };

    }

    private Response uploadRequest(Request.Builder builder) {

        try {
            Response response = mClient.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                uuid = null;
                throw new RuntimeException(response.body().string());
            } else {
                if (onProgressListener != null) {
                    onProgressListener.onRequestProgress(blockProgress + 2, totalBlock);
                }
                blockProgress++;
            }
            return response;

        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    boolean completeUpload() throws IOException, UpYunException {
        completeRequest();
        status = null;
        uuid = null;
        return true;
    }
}
