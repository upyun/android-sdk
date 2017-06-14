package com.upyun.sdktest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.upyun.library.common.BlockUploader;
import com.upyun.library.common.Params;
import com.upyun.library.common.ResumeUploader;
import com.upyun.library.common.UploadClient;
import com.upyun.library.common.UploadEngine;
import com.upyun.library.common.UploadManager;
import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    //空间名
    public static String SPACE = "formtest";
    //操作员
    public static String OPERATER = "one";
    //密码
    public static String PASSWORD = "qwertyuiop";

    private ProgressBar uploadProgress;

    private TextView textView;

    private String savePath = "/uploads/{year}{mon}{day}/{random32}{.suffix}";

    private String policy = "eyJyZXR1cm4tdXJsIjoiaHR0cGJpbi5vcmdcL3Bvc3QiLCJidWNrZXQiOiJidWNrZXQxIiwiY29udGVudC1tZDUiOiI4Nzc0NDE4OGMyZDgyYjcyMDNlOGI3NDQ3MzU5MTE2OCIsImRhdGUiOiJGcmksIDAzIE1hciAyMDE3IDA5OjAxOjAzIiwiZXhwaXJhdGlvbiI6MTQ4ODUzMzQ2Mywic2F2ZS1rZXkiOiJcL3VwbG9hZHNcL3t5ZWFyfXttb259e2RheX1cL3tyYW5kb20zMn17LnN1ZmZpeH0ifQ==";

    private String signature = "CuThEAj+xqwwtPotif1l2dT6P8w=";

    public static String KEY = "GqSu2v26RI+Xu3yLdsWfynTS/LM=";

    private static final String SAMPLE_PIC_FILE = "/mnt/sdcard/test1";
    private ResumeUploader uploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File temp = null;
        try {
            temp = getTempFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        uploadProgress = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.tv_process);
        final Map<String, Object> paramsMap = new HashMap<>();
        //上传空间
        paramsMap.put(Params.BUCKET, SPACE);
        //保存路径，任选其中一个
        paramsMap.put(Params.SAVE_KEY, savePath);


//        Locale locale = Locale.US;
//        Date d = new Date();
//        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", locale);
//        format.setTimeZone(TimeZone.getTimeZone("GMT"));
//        System.out.println(format.format(d));
//        paramsMap.put(Params.DATE, format.format(d));

        paramsMap.put(Params.CONTENT_MD5, UpYunUtils.md5Hex(temp));
//        paramsMap.put(Params.PATH, savePath);
        //可选参数（详情见api文档介绍）
        paramsMap.put(Params.RETURN_URL, "httpbin.org/post");
        //进度回调，可为空
        UpProgressListener progressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(final long bytesWrite, final long contentLength) {
                uploadProgress.setProgress((int) ((100 * bytesWrite) / contentLength));
                textView.setText((100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, (100 * bytesWrite) / contentLength + "%");
            }
        };

        //结束回调，不可为空
        UpCompleteListener completeListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                textView.setText(isSuccess + ":" + result);
                Log.e(TAG, isSuccess + ":" + result);
            }
        };

        //表单上传（本地签名方式）
        UploadEngine.getInstance().formUpload(temp, paramsMap, OPERATER, UpYunUtils.md5(PASSWORD), completeListener, progressListener);
        //表单上传（服务器签名方式）
        UploadEngine.getInstance().formUpload(temp, policy, OPERATER, signature, completeListener, progressListener);


        //初始化断点续传
        uploader = new ResumeUploader(SPACE, OPERATER, UpYunUtils.md5(PASSWORD));
        //设置 MD5 校验
        uploader.setCheckMD5(true);

        //设置进度监听
        uploader.setOnProgressListener(new ResumeUploader.OnProgressListener() {
            @Override
            public void onProgress(int index, int total) {
                Log.e(TAG, index + ":" + total);
            }
        });

    }

    public void resumeUpload(View view) {
        resumeUpdate(new File(SAMPLE_PIC_FILE), null);
    }


    private File getTempFile() throws IOException {
        File temp = File.createTempFile("upyun", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.close();
        return temp;
    }


    //断点续传使用 DEMO
    public void resumeUpdate(final File file, final Map<String, String> params) {

        new Thread() {
            @Override
            public void run() {
                try {
                    //同步方法网络请求，Android 上自行管理线程
                    uploader.upload(file, "/test1.txt", params);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (UpYunException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
