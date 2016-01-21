package com.upyun.library.common;

import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.util.Map;

public class FormUploader implements Runnable {


    private UploadClient client;
    private File file;
    private String bucket;
    private String policy;
    private String signature;
    private UpProgressListener progressListener;
    private UpCompleteListener completeListener;


    public FormUploader(UploadClient client,File file,Map<String,Object> params,String apiKey,UpCompleteListener completeListener,UpProgressListener progressListener) {
        this.client = client;
        this.file = file;
        this.bucket = (String)params.get(Params.BUCKET);
        this.policy = UpYunUtils.getPolicy(params);
        this.signature = UpYunUtils.getSignature(policy, apiKey);
        this.completeListener = completeListener;
        this.progressListener = progressListener;
    }

    public FormUploader(UploadClient client, File file, Map<String, Object> params, SignatureListener signatureListener, UpCompleteListener completeListener, UpProgressListener progressListener) {
        this.client = client;
        this.file = file;
        this.bucket = (String)params.get(Params.BUCKET);
        this.policy = UpYunUtils.getPolicy(params);
        this.signature =signatureListener.getSignature(policy+"&");
        this.completeListener = completeListener;
        this.progressListener = progressListener;
    }

    @Override
    public void run() {
        try {
            String response=client.fromUpLoad(file, bucket, policy, signature, progressListener);
            completeListener.onComplete(true,response);
        } catch (Exception e) {
            completeListener.onComplete(false,e.getMessage());
        }
    }
}
