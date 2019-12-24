package com.upyun.library.common;

import com.upyun.library.exception.RespException;
import com.upyun.library.exception.UpYunException;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SerialUploader extends BaseUploader {

    /**
     * 断点续传
     *
     * @param uuid          上传任务 uuid
     * @param nextPartIndex 下一个上传分块 index
     * @return 服务器返回结果
     * @throws IOException
     * @throws UpYunException
     */
    public Response resume(String uuid, int nextPartIndex) throws IOException, UpYunException {

        this.uuid = uuid;
        this.nextPartIndex = nextPartIndex;

        if (uuid == null) {
            throw new UpYunException("uuid is null, please restart!");
        } else {
            this.paused = false;
            return startUpload();
        }
    }

    /**
     * 初始化 SerialUploader
     *
     * @param bucketName 空间名称
     * @param userName   操作员名称
     * @param password   密码，不需要MD5加密
     */
    public SerialUploader(String bucketName, String userName, String password) {
        super(bucketName, userName, password);
    }

    public SerialUploader() {
    }

    /**
     * 开始上传
     *
     * @param file       本地上传文件路径
     * @param uploadPath 上传服务器路径
     * @param params     通用上传参数（见 rest api 文档）
     * @return 服务器返回结果
     * @throws IOException
     * @throws UpYunException
     */

    @Override
    public Response upload(File file, String uploadPath, Map<String, String> params) throws IOException, UpYunException {
        init(file, uploadPath, params);
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
     * @return 服务器返回结果
     * @throws IOException
     */
    @Override
    public Response upload(File file, String uri, String date, String signature, Map<String, String> params) throws IOException, UpYunException {
        init(file, uri, date, signature, params);
        return startUpload();
    }

    /**
     * 获取下一个上传分块 index
     *
     * @return index
     */
    public int getNextPartIndex() {
        return nextPartIndex;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setNextPartIndex(int nextPartIndex) {
        this.nextPartIndex = nextPartIndex;
    }

    Response processUpload() throws IOException, UpYunException {
        byte[] data;

        while (nextPartIndex >= 0) {

            if (paused) {
                throw new UpYunException("upload paused");
            }

            data = readBlockByIndex(nextPartIndex);

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
                    .header(X_UPYUN_PART_ID, nextPartIndex + "")
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

            if (onProgressListener != null) {
                onProgressListener.onRequestProgress(nextPartIndex + 2, totalBlock);
            }
            callProcessRequest(builder.build());
        }

        return completeUpload();
    }

    Response completeUpload() throws IOException, UpYunException {
        Response response = completeRequest();
        uuid = null;
        if (!response.isSuccessful()) {
            throw new RespException(response.code(), response.body().string());
        }
        nextPartIndex = 0;
        return response;
    }

    private void callProcessRequest(Request request) throws IOException, UpYunException {

        Response response = mClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            uuid = null;
            throw new RespException(response.code(), response.body().string());
        } else {
            uuid = response.header(X_UPYUN_MULTI_UUID, "");
            nextPartIndex = Integer.parseInt(response.header(X_UPYUN_NEXT_PART_ID, "-2"));
        }
    }
}