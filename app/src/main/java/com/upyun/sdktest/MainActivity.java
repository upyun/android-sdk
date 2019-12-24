package com.upyun.sdktest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.upyun.library.common.BaseUploader;
import com.upyun.library.common.ParallelUploader;
import com.upyun.library.common.Params;
import com.upyun.library.common.SerialUploader;
import com.upyun.library.common.UploadEngine;
import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.Base64Coder;
import com.upyun.library.utils.UpYunUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Response;

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

    private ProgressBar bp_serial;

    private TextView tv_serial;

    private ProgressBar bp_parallel;

    private TextView tv_parallel;

    private String savePath = "/uploads/{year}{mon}{day}/{random32}{.suffix}";

    private String policy = "eyJyZXR1cm4tdXJsIjoiaHR0cGJpbi5vcmdcL3Bvc3QiLCJidWNrZXQiOiJidWNrZXQxIiwiY29udGVudC1tZDUiOiI4Nzc0NDE4OGMyZDgyYjcyMDNlOGI3NDQ3MzU5MTE2OCIsImRhdGUiOiJGcmksIDAzIE1hciAyMDE3IDA5OjAxOjAzIiwiZXhwaXJhdGlvbiI6MTQ4ODUzMzQ2Mywic2F2ZS1rZXkiOiJcL3VwbG9hZHNcL3t5ZWFyfXttb259e2RheX1cL3tyYW5kb20zMn17LnN1ZmZpeH0ifQ==";

    private String signature = "CuThEAj+xqwwtPotif1l2dT6P8w=";

    public static String KEY = "GqSu2v26RI+Xu3yLdsWfynTS/LM=";

    @SuppressLint("SdCardPath")
    private static final String SAMPLE_PIC_FILE = "/mnt/sdcard/tdtest.mp4";
    private ParallelUploader parallelUploader;
    private SerialUploader serialUploader;

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

        initView();

        //初始化断点续传
        serialUploader = new SerialUploader(SPACE, OPERATER, UpYunUtils.md5(PASSWORD));

        //初始化断点续传
        parallelUploader = new ParallelUploader(SPACE, OPERATER, UpYunUtils.md5(PASSWORD));

        //初始化断点续传 (服务端签名可用)
//        parallelUploader = new ParallelUploader();
    }

    private void initView() {
        bp_form = findViewById(R.id.pb_form);
        tv_form = findViewById(R.id.tv_form);

        findViewById(R.id.bt_form).setOnClickListener(this);

        bp_serial = findViewById(R.id.pb_serial);
        tv_serial = findViewById(R.id.tv_serial);
        findViewById(R.id.bt_serial).setOnClickListener(this);
        findViewById(R.id.bt_serial_pause).setOnClickListener(this);

        bp_parallel = findViewById(R.id.pb_parallel);
        tv_parallel = findViewById(R.id.tv_parallel);

        findViewById(R.id.bt_parallel).setOnClickListener(this);
        findViewById(R.id.bt_parallel_pause).setOnClickListener(this);
    }

    private void formUpload() {
        File temp = new File(SAMPLE_PIC_FILE);
//        try {
//            temp = getTempFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        final Map<String, Object> paramsMap = new HashMap<>();
        //上传空间
        paramsMap.put(Params.BUCKET, SPACE);
        //保存路径
        paramsMap.put(Params.SAVE_KEY, savePath);
        //添加 CONTENT_LENGTH 参数使用大文件表单上传
        paramsMap.put(Params.CONTENT_LENGTH, temp.length());
        //上传文件的 MD5 值，如果请求中文件太大计算 MD5 不方便，可以为空
//        paramsMap.put(Params.CONTENT_MD5, UpYunUtils.md5Hex(temp));
        //可选参数（详情见api文档介绍）
        paramsMap.put(Params.RETURN_URL, "httpbin.org/post");
        //进度回调，可为空
        UpProgressListener progressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_form.setProgress((int) ((100 * bytesWrite) / contentLength));
//                tv_form.setText((100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, (100 * bytesWrite) / contentLength + "%");
                Log.e(TAG, bytesWrite + "::" + contentLength);
            }
        };

        //结束回调，不可为空
        UpCompleteListener completeListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, Response response, Exception error) {
                try {
                    String result = null;
                    if (response != null) {
                        result = response.body().string();
                    } else if (error != null) {
                        result = error.toString();
                    }
                    tv_form.setText("isSuccess:" + isSuccess + " result:" + result);
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

    public void serialUpload() {

        File file = new File(SAMPLE_PIC_FILE);

        //设置 MD5 校验
        serialUploader.setCheckMD5(true);

        //设置进度监听
        serialUploader.setOnProgressListener(new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_serial.setProgress((int) ((100 * bytesWrite) / contentLength));
                Log.e(TAG, bytesWrite + ":" + contentLength);
            }
        });

        //初始化异步音视频处理参数,参数规则详见http://docs.upyun.com/cloud/av/#_3
        Map<String, Object> processParam = new HashMap<String, Object>();

        processParam.put(BaseUploader.Params.BUCKET_NAME, SPACE);
        processParam.put(BaseUploader.Params.NOTIFY_URL, "http://httpbin.org/post");
        processParam.put(BaseUploader.Params.ACCEPT, "json");
        processParam.put(BaseUploader.Params.SOURCE, "/test.mp4");

        JSONArray array = new JSONArray();

        try {
            JSONObject json = new JSONObject();
            json.put(BaseUploader.Params.TYPE, "video");
            json.put(BaseUploader.Params.AVOPTS, "/s/240p(4:3)/as/1/r/30");
            json.put(BaseUploader.Params.RETURN_INFO, "true");
            json.put(BaseUploader.Params.SAVE_AS, "testProcess.mp4");

            JSONObject json2 = new JSONObject();

            json2.put(BaseUploader.Params.TYPE, "video");
            json2.put(BaseUploader.Params.AVOPTS, "/s/240p(4:3)/as/1/r/30");
            json2.put(BaseUploader.Params.RETURN_INFO, "true");
            json2.put(BaseUploader.Params.SAVE_AS, "testProcess2.mp4");
            array.put(json2);
            array.put(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        processParam.put(BaseUploader.Params.TASKS, array);

        //串行断点上传
        serialUploader.upload(file, "/test.mp4", null, new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, Response response, Exception error) {
                Log.e(TAG, "isSuccess:" + isSuccess + " response:" + response + " error:" + error);
                tv_serial.setText("isSuccess:" + isSuccess + " response:" + response + " error:" + error);
            }
        });

        //带异步处理参数的断点上传
//        serialUploader.upload(file, "/1/test{「你好.mp4", null, processParam, new UpCompleteListener() {
//            @Override
//            public void onComplete(boolean isSuccess, String result) {
//                Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
//            }
//        });


        //请求路径(带空间名)
//        String uri = "/" + SPACE + "/11.test";
//        //请求日期时间
//        String date = getGMTDate();
//        //签名
//        String sign = null;
//        try {
//            sign = sign("PUT", date, uri, OPERATER, UpYunUtils.md5(PASSWORD), null);
//        } catch (UpYunException e) {
//            e.printStackTrace();
//        }
//
//        //设置 MD5 校验 （服务端签名不可校验）
//        serialUploader.setCheckMD5(false);
//
//        //服务端签名
//        serialUploader.upload(file, uri, date, sign,
//                null, new UpCompleteListener() {
//                    @Override
//                    public void onComplete(boolean isSuccess, String result) {
//                        Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
//                    }
//                });

    }

    public void parallelUpload() {

        File file = new File(SAMPLE_PIC_FILE);

        //设置 MD5 校验
        parallelUploader.setCheckMD5(true);

        //设置进度监听
        parallelUploader.setOnProgressListener(new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                bp_parallel.setProgress((int) ((100 * bytesWrite) / contentLength));
                Log.e(TAG, bytesWrite + ":" + contentLength);
            }
        });


        parallelUploader.upload(file, "/11.test", null, new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, Response response, Exception error) {
                Log.e(TAG, "isSuccess:" + isSuccess + " response:" + response + " error:" + error);
                tv_parallel.setText("isSuccess:" + isSuccess + " response:" + response + " error:" + error);
            }
        });


        //请求路径(带空间名)
//        String uri = "/" + SPACE + "/11.test";
//        //请求日期时间
//        String date = getGMTDate();
//        //签名
//        String sign = null;
//        try {
//            sign = sign("PUT", date, uri, OPERATER, UpYunUtils.md5(PASSWORD), null);
//        } catch (UpYunException e) {
//            e.printStackTrace();
//        }
//
//        //设置 MD5 校验 （服务端签名不可校验）
//        parallelUploader.setCheckMD5(false);
//
//        //服务端签名
//        parallelUploader.upload(file, uri, date, sign,
//                null, new UpCompleteListener() {
//                    @Override
//                    public void onComplete(boolean isSuccess, String result) {
//                        Log.e(TAG, "isSuccess:" + isSuccess + "  result:" + result);
//                    }
//                });
    }


    private File getTempFile() throws IOException {
        File temp = File.createTempFile("test", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.close();
        return temp;
    }

    private String sign(String method, String date, String path, String userName, String password, String md5) throws UpYunException {

        StringBuilder sb = new StringBuilder();
        String sp = "&";
        sb.append(method);
        sb.append(sp);
        sb.append(path);

        sb.append(sp);
        sb.append(date);

        if (md5 != null) {
            sb.append(sp);
            sb.append(md5);
        }
        String raw = sb.toString().trim();
        byte[] hmac = null;
        try {
            hmac = UpYunUtils.calculateRFC2104HMACRaw(password, raw);
        } catch (Exception e) {
            throw new UpYunException("calculate SHA1 wrong.");
        }

        if (hmac != null) {
            return "UPYUN " + userName + ":" + Base64Coder.encodeLines(hmac);
        }

        return null;
    }

    private String getGMTDate() {
        SimpleDateFormat formater = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        formater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formater.format(new Date());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_form:
                formUpload();
                break;
            case R.id.bt_serial:
                serialUpload();
                break;
            case R.id.bt_parallel:
                parallelUpload();
                break;
            case R.id.bt_serial_pause:
                serialUploader.pause();
                break;
            case R.id.bt_parallel_pause:
                parallelUploader.pause();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }
}
