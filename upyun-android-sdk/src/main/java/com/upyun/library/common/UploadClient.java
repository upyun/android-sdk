package com.upyun.library.common;

import com.upyun.library.exception.RespException;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadClient {

    private OkHttpClient client;

    UploadClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(UpConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(UpConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(UpConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    Response fromUpLoad2(File file, String url, String policy, String operator, String signature, UpProgressListener listener) throws IOException, RespException {

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("policy", policy)
                .addFormDataPart("authorization", "UPYUN " + operator + ":" + signature)
                .addFormDataPart("file", URLEncoder.encode(file.getName()), RequestBody.create(null, file))
                .build();

        if (listener != null) {
            requestBody = ProgressHelper.addProgressListener(requestBody, listener);
        }
        Request request = new Request.Builder()
                .addHeader("x-upyun-api-version", "2")
                .header("User-Agent", UpYunUtils.VERSION)
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RespException(response.code(), response.body().string());
        }
        return response;
    }
}
