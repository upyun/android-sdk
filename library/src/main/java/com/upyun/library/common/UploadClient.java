package com.upyun.library.common;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.UpProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UploadClient {

    private static final String TAG = "UploadClient";
    private static String FORM_HOST = "https://v0.api.upyun.com";
    private static String BLOCK_HOST = "https://m0.api.upyun.com";
    private OkHttpClient client;

    protected UploadClient() {
        client = new OkHttpClient();
        client.setConnectTimeout(5000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(5000, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(5000, TimeUnit.MILLISECONDS);
        client.setFollowRedirects(true);
    }

    public String fromUpLoad(File file, String bucket, String policy, String signature, UpProgressListener listener) throws IOException, UpYunException {

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
                .url(FORM_HOST + "/" + bucket)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new UpYunException("Unexpected code :" + response);
        } else {
            String result = response.body().string();
            response.body().close();
//            Log.i(TAG, "from:" + result);
            return result;
        }
    }

    public String blockPost(String bucket, final Map<String, String> requestParams) throws IOException, UpYunException {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        for (Map.Entry entry : requestParams.entrySet()) {
            builder.add((String) entry.getKey(), (String) entry.getValue());
        }
        Request request = new Request.Builder()
                .url(BLOCK_HOST + "/" + bucket)
                .post(builder.build())
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new UpYunException("Unexpected code :" + response);
        } else {
            String result = response.body().string();
//            Log.i(TAG, "blockPost:" + result);
            return result;
        }
    }

    public String blockMutipartPost(final String bucket, final PostData postData) throws IOException, UpYunException {
        Map<String, String> requestParams = postData.params;
        MultipartBuilder builder = new MultipartBuilder()
                .type(MultipartBuilder.FORM);

        for (Map.Entry entry : requestParams.entrySet()) {
            builder.addFormDataPart((String) entry.getKey(), (String) entry.getValue());
        }
        builder.addFormDataPart("file", postData.fileName, RequestBody.create(null, postData.data));
        Request request = new Request.Builder()
                .url(BLOCK_HOST+"/"+bucket)
                .post(builder.build())
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new UpYunException("Unexpected code :" + response);
        } else {
            String result = response.body().string();
//            Log.i(TAG, "blockMutipartPost:" + result);
            return result;
        }
    }
}
