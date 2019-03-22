package com.upyun.sdktest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.upyun.library.common.ParallelUploader;
import com.upyun.library.common.Params;
import com.upyun.library.common.ResumeUploader;
import com.upyun.library.common.UploadEngine;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    //空间名
    public static String SPACE = "formtest";
    //操作员
    public static String OPERATER = "one";
    //密码
    public static String PASSWORD = "****";

    private ProgressBar bp_form;

    private TextView tv_form;

    private ProgressBar bp_resume;

    private TextView tv_resume;

    private ProgressBar bp_parallel;

    private TextView tv_parallel;

    private String savePath = "/uploads/{year}{mon}{day}/{random32}{.suffix}";

    private String policy = "eyJyZXR1cm4tdXJsIjoiaHR0cGJpbi5vcmdcL3Bvc3QiLCJidWNrZXQiOiJidWNrZXQxIiwiY29udGVudC1tZDUiOiI4Nzc0NDE4OGMyZDgyYjcyMDNlOGI3NDQ3MzU5MTE2OCIsImRhdGUiOiJGcmksIDAzIE1hciAyMDE3IDA5OjAxOjAzIiwiZXhwaXJhdGlvbiI6MTQ4ODUzMzQ2Mywic2F2ZS1rZXkiOiJcL3VwbG9hZHNcL3t5ZWFyfXttb259e2RheX1cL3tyYW5kb20zMn17LnN1ZmZpeH0ifQ==";

    private String signature = "CuThEAj+xqwwtPotif1l2dT6P8w=";

    public static String KEY = "GqSu2v26RI+Xu3yLdsWfynTS/LM=";

    @SuppressLint("SdCardPath")
    private static final String SAMPLE_PIC_FILE = "/mnt/sdcard/tdtest.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        bp_form = findViewById(R.id.pb_form);
        tv_form = findViewById(R.id.tv_form);

        findViewById(R.id.bt_form).setOnClickListener(this);


        bp_resume = findViewById(R.id.pb_resume);
        tv_resume = findViewById(R.id.tv_resume);
        findViewById(R.id.bt_resume).setOnClickListener(this);

        bp_parallel = findViewById(R.id.pb_parallel);
        tv_parallel = findViewById(R.id.tv_parallel);

        findViewById(R.id.bt_parallel).setOnClickListener(this);
    }

    private void formUpload() {
        File temp = null;
//        try {
//            temp = getTempFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        temp = new File(SAMPLE_PIC_FILE);

        final Map<String, Object> paramsMap = new HashMap<>();
        //上传空间
        paramsMap.put(Params.BUCKET, SPACE);
        //保存路径，任选其中一个
        paramsMap.put(Params.SAVE_KEY, savePath);
        //添加 CONTENT_LENGTH 参数使用大文件表单上传
        paramsMap.put(Params.CONTENT_LENGTH, temp.length());


        paramsMap.put(Params.CONTENT_MD5, UpYunUtils.md5Hex(temp));
//        paramsMap.put(Params.PATH, savePath);
        //可选参数（详情见api文档介绍）
        paramsMap.put(Params.RETURN_URL, "httpbin.org/post");
        //进度回调，可为空
        UpProgressListener progressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_form.setProgress((int) ((100 * bytesWrite) / contentLength));
                tv_form.setText((100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, (100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, bytesWrite + "::" + contentLength);
            }
        };

        //结束回调，不可为空
        UpCompleteListener completeListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                tv_form.setText(isSuccess + ":" + result);
                Log.e(TAG, isSuccess + ":" + result);
            }
        };


        //初始化JSONArray，上传预处理（异步）参数详见 http://docs.upyun.com/cloud/image/#_6，
        JSONArray array = new JSONArray();

        //初始化JSONObject
        JSONObject json = new JSONObject();

        //json 添加 name 属性
        try {
            json.put("name", "thumb");
            //json 添加 X_GMKERL_THUMB 属性
//            json.put("x-gmkerl-thumb", "/fw/300/unsharp/true/quality/80/format/png");
            json.put("x-gmkerl-thumb", "/watermark/url/L2dta2VybC5qcGc=/align/center");

            //json 添加 save_as 属性
            json.put("save_as", "/path/to/wm_102.jpg");

            //json 添加 notify_url 属性
            json.put("notify_url", "http://httpbin.org/post");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //将json 对象放入 JSONArray
        array.put(json);

        //添加异步作图参数 APPS
        paramsMap.put("apps", array);

        //表单上传（本地签名方式）
        UploadEngine.getInstance().formUpload(temp, paramsMap, OPERATER, UpYunUtils.md5(PASSWORD), completeListener, progressListener);


        //表单上传（服务器签名方式）
//        UploadEngine.getInstance().formUpload(temp, policy, OPERATER, signature, completeListener, progressListener);
    }

    public void resumeUpload() {

        File file = new File(SAMPLE_PIC_FILE);

        //初始化断点续传
        ResumeUploader uploader = new ResumeUploader(SPACE, OPERATER, UpYunUtils.md5(PASSWORD));

        //设置 MD5 校验
        uploader.setCheckMD5(true);

        //设置进度监听
        uploader.setOnProgressListener(new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_resume.setProgress((int) ((100 * bytesWrite) / contentLength));
                Log.e(TAG, bytesWrite + ":" + contentLength);
            }
        });

        //初始化异步音视频处理参数,参数规则详见http://docs.upyun.com/cloud/av/#_3
        Map<String, Object> processParam = new HashMap<String, Object>();

        processParam.put(ResumeUploader.Params.BUCKET_NAME, SPACE);
        processParam.put(ResumeUploader.Params.NOTIFY_URL, "http://httpbin.org/post");
        processParam.put(ResumeUploader.Params.ACCEPT, "json");
        processParam.put(ResumeUploader.Params.SOURCE, "/test.mp4");

        JSONArray array = new JSONArray();

        try {
            JSONObject json = new JSONObject();
            json.put(ResumeUploader.Params.TYPE, "video");
            json.put(ResumeUploader.Params.AVOPTS, "/s/240p(4:3)/as/1/r/30");
            json.put(ResumeUploader.Params.RETURN_INFO, "true");
            json.put(ResumeUploader.Params.SAVE_AS, "testProcess.mp4");

            JSONObject json2 = new JSONObject();

            json2.put(ResumeUploader.Params.TYPE, "video");
            json2.put(ResumeUploader.Params.AVOPTS, "/s/240p(4:3)/as/1/r/30");
            json2.put(ResumeUploader.Params.RETURN_INFO, "true");
            json2.put(ResumeUploader.Params.SAVE_AS, "testProcess2.mp4");
            array.put(json2);
            array.put(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        processParam.put(ResumeUploader.Params.TASKS, array);

        //串行断点上传
//        uploader.upload(file, "/test.mp4", null, new UpCompleteListener() {
//            @Override
//            public void onComplete(boolean isSuccess, String result) {
//                Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
//            }
//        });

        //带异步处理参数的断点上传
        uploader.upload(file, "/test.mp4", null, processParam, new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
            }
        });
    }

    public void parallelUpload() {

        File file = new File(SAMPLE_PIC_FILE);

        //初始化断点续传
        ParallelUploader uploader = new ParallelUploader(SPACE, OPERATER, UpYunUtils.md5(PASSWORD));

        //设置 MD5 校验
        uploader.setCheckMD5(true);

        //设置进度监听
        uploader.setOnProgressListener(new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_parallel.setProgress((int) ((100 * bytesWrite) / contentLength));
                Log.e(TAG, bytesWrite + ":" + contentLength);
            }
        });

        uploader.upload(file, "/test.mp4", null, new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
            }
        });

//        uploader.upload(file, "/test.mp4", null, processParam, new UpCompleteListener() {
//            @Override
//            public void onComplete(boolean isSuccess, String result) {
//                Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
//            }
//        });
    }


    private File getTempFile() throws IOException {
        File temp = File.createTempFile("你好啊啊", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.close();
        return temp;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_form:
                formUpload();
                break;
            case R.id.bt_resume:
                resumeUpload();
                break;
            case R.id.bt_parallel:
                parallelUpload();
                break;
        }
    }
}
