package com.upyun.library.common;

import com.upyun.library.exception.RespException;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.Base64Coder;
import com.upyun.library.utils.UpYunUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Map;

public class FormUploader2 implements Runnable {


    private static final String TAG = "FormUploader2";
    private UploadClient client;
    private File file;
    private String bucket;
    private String policy;
    private String signature;
    private UpProgressListener progressListener;
    private UpCompleteListener completeListener;
    private int retryTime;
    private Map<String, Object> params;

    private String operator;
    private String password;


    public FormUploader2(UploadClient upLoaderClient, File file, Map<String, Object> localParams, String operator, String password, UpCompleteListener uiCompleteListener, UpProgressListener uiProgressListener) {
        this.client = upLoaderClient;
        this.file = file;
        this.bucket = (String) localParams.get(Params.BUCKET);
        this.params = localParams;
        this.completeListener = uiCompleteListener;
        this.progressListener = uiProgressListener;

        this.operator = operator;
        this.password = password;
    }

    public FormUploader2(UploadClient upLoaderClient, File file, String policy, String operator, String signature, UpCompleteListener uiCompleteListener, UpProgressListener uiProgressListener) {
        this.client = upLoaderClient;
        this.file = file;

        String paramMap = Base64Coder.decodeString(policy);
        try {
            JSONObject obj = new JSONObject(paramMap);
            this.bucket = (String) obj.get(Params.BUCKET);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.policy = policy;
        this.signature = signature;
        this.completeListener = uiCompleteListener;
        this.progressListener = uiProgressListener;
        this.operator = operator;
    }


    @Override
    public void run() {

        if (policy != null && operator != null && signature != null && bucket != null) {

        } else if (params != null && operator != null & password != null) {

            this.policy = UpYunUtils.getPolicy(params);
            String date = (String) params.get(Params.DATE);
            String contentMd5 = (String) params.get(Params.CONTENT_MD5);

            StringBuilder sb = new StringBuilder();
            String sp = "&";
            sb.append("POST");
            sb.append(sp);
            sb.append("/" + bucket);

            if (date != null) {
                sb.append(sp);
                sb.append(date);
            }

            sb.append(sp);
            sb.append(policy);

            if (contentMd5 != null) {
                sb.append(sp);
                sb.append(contentMd5);
            }

            String raw = sb.toString().trim();

            byte[] hmac;

            try {
                hmac = UpYunUtils.calculateRFC2104HMACRaw(password, raw);
            } catch (SignatureException e) {
                completeListener.onComplete(false, "签名计算失败");
                return;
            } catch (NoSuchAlgorithmException e) {
                completeListener.onComplete(false, "找不到 SHA1 算法");
                return;
            } catch (InvalidKeyException e) {
                completeListener.onComplete(false, "password 错误");
                return;
            }

            if (hmac != null) {
                this.signature = Base64Coder.encodeLines(hmac);
            }
        } else {
            completeListener.onComplete(false, "参数错误");
            return;
        }

        String url = UpConfig.FORM_HOST + "/" + bucket;
        try {
            String response = client.fromUpLoad2(file, url, policy, operator, signature, progressListener);
            completeListener.onComplete(true, response);
        } catch (IOException | RespException e) {
            if (++retryTime > UpConfig.RETRY_TIME || (e instanceof RespException && ((RespException) e).code() / 100 != 5)) {
                completeListener.onComplete(false, e.toString());
            } else {
                this.run();
            }
        }
    }
}
