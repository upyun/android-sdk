package com.upyun.library.common;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.upyun.library.exception.RespException;
import com.upyun.library.listener.UpProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UploadClient {

    private static final String TAG = "UploadClient";
    private OkHttpClient client;

    public UploadClient() {
        client = new OkHttpClient();
        client.setConnectTimeout(UpConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS);
        client.setReadTimeout(UpConfig.READ_TIMEOUT, TimeUnit.SECONDS);
        client.setWriteTimeout(UpConfig.WRITE_TIMEOUT, TimeUnit.SECONDS);
        client.setFollowRedirects(true);
    }

    public String fromUpLoad(File file, String url, String policy, String signature, UpProgressListener listener) throws IOException, RespException {

        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(null, file))
                .addFormDataPart("policy", policy)
                .addFormDataPart("signature", signature)
                .build();

        if (listener != null) {
            requestBody = ProgressHelper.addProgressListener(requestBody, listener);
        }
        Request request = new Request.Builder()
                .addHeader("x-upyun-api-version ", "2")
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RespException(response.code(), response.body().string());
        } else {
            return response.body().string();
        }
    }

    public String post(String url, final Map<String, String> requestParams) throws IOException, RespException {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .addHeader("x-upyun-api-version ", "2")
                .url(url)
                .post(builder.build())
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RespException(response.code(), response.body().string());
        } else {
            return response.body().string();
        }
    }

    public String blockMultipartPost(String url, PostData postData) throws IOException, RespException {
        Map<String, String> requestParams = postData.params;
        MultipartBuilder builder = new MultipartBuilder()
                .type(MultipartBuilder.FORM);

        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            builder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        builder.addFormDataPart("file", postData.fileName, RequestBody.create(null, postData.data));
        Request request = new Request.Builder()
                .addHeader("x-upyun-api-version ", "2")
                .url(url)
                .post(builder.build())
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RespException(response.code(), response.body().string());
        } else {
            return response.body().string();
        }
    }
}
