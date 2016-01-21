package com.upyun.sdktest;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.upyun.library.common.Params;
import com.upyun.library.common.UpConfig;
import com.upyun.library.common.UploadManager;
import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static String KEY = "GqSu2v26RI+Xu3yLdsWfynTS/LM=";
    public static String SPACE = "formtest";
    private String localFilePath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "test3.dmg";
    private ProgressBar uploadProgress;
    private TextView textView;
    //保存路径
    String savePath = "/uploads/{year}{mon}{day}/{random32}{.suffix}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uploadProgress = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.tv_process);
        final Map<String, Object> paramsMap = new HashMap<>();
        //上传空间
        paramsMap.put(Params.BUCKET, SPACE);
        //默认过期时间60s 可自定义
        paramsMap.put(Params.EXPIRATION, UpConfig.EXPIRATION);
        paramsMap.put(Params.SAVE_KEY, savePath);
        paramsMap.put(Params.RETURN_URL, "httpbin.org/post");

        //进度回调，可为空
        UpProgressListener progressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(final long bytesWrite, final long contentLength) {

//                uploadProgress.setProgress((int) ((100 * bytesWrite) / contentLength));
//                textView.setText((100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, (100 * bytesWrite) / contentLength + "%");
            }
        };

        UpCompleteListener completeListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
//                textView.setText(isSuccess + ":" + result);
                Log.e(TAG, result);
            }
        };

        SignatureListener signatureListener=new SignatureListener() {
            @Override
            public String getSignature(String raw) {
                return UpYunUtils.md5(raw+KEY);
            }
        };

        UploadManager.getInstance().upload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
        UploadManager.getInstance().upload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
    }
}
