package com.upyun.library.common;

import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FormUploader implements Runnable {


    private UploadClient client;
    private File file;
    private String bucket;
    private String policy;
    private String signature;
    private UpProgressListener progressListener;
    private UpCompleteListener completeListener;
    private int retryTime;


    public FormUploader(UploadClient upLoaderClient, File file, Map<String, Object> localParams, String apiKey, SignatureListener signatureListener, UpCompleteListener uiCompleteListener, UpProgressListener uiProgressListener) {
        this.client = upLoaderClient;
        this.file = file;
        this.bucket = (String) localParams.get(Params.BUCKET);
        this.policy = UpYunUtils.getPolicy(localParams);
        if(apiKey!=null){
            this.signature = UpYunUtils.getSignature(policy, apiKey);
        }else if(signatureListener!=null){
            this.signature = signatureListener.getSignature(policy+"&");
        }else {
            throw new RuntimeException("apiKey 和 signature 不能同时为null");
        }
        this.completeListener = uiCompleteListener;
        this.progressListener = uiProgressListener;
    }

    @Override
    public void run() {
        String url = UpConfig.FORM_HOST + "/" + bucket;
        try {
            String response = client.fromUpLoad(file, url, policy, signature, progressListener);
            completeListener.onComplete(true, response);
        } catch (IOException | UpYunException e) {
            if (++retryTime > UpConfig.RETRY_TIME) {
                completeListener.onComplete(false, e.toString());
            } else {
                this.run();
            }
        }
    }
}
