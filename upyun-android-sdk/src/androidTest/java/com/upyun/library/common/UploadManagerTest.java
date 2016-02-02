package com.upyun.library.common;

import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import junit.framework.TestCase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class UploadManagerTest extends TestCase {

    public static String KEY = "GqSu2v26RI+Xu3yLdsWfynTS/LM=";
    public static String SPACE = "formtest";
    String savePath = "/uploads/{year}{mon}{day}/{random32}{.suffix}";
    UploadManager manager = UploadManager.getInstance();

    public void testUploadManager() throws Exception {

        final CountDownLatch latch = new CountDownLatch(12);
        final Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(Params.BUCKET, SPACE);
        paramsMap.put(Params.SAVE_KEY, savePath);
        paramsMap.put(Params.EXPIRATION, Calendar.getInstance().getTimeInMillis() + UpConfig.EXPIRATION);
        paramsMap.put(Params.RETURN_URL, "httpbin.org/post");

        File temp = File.createTempFile("upyun", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.close();

        UpProgressListener progressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(final long bytesWrite, final long contentLength) {
                assertNotNull(bytesWrite);
                assertNotNull(contentLength);
                assertTrue(bytesWrite <= contentLength);
            }
        };

        UpCompleteListener completeListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                assertNotNull(isSuccess);
                assertNotNull(result);
                assertTrue(isSuccess);
                latch.countDown();
            }
        };

        UpCompleteListener faileCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(boolean isSuccess, String result) {
                assertNotNull(isSuccess);
                assertNotNull(result);
                assertFalse(isSuccess);
                latch.countDown();
            }
        };

        SignatureListener signatureListener = new SignatureListener() {
            @Override
            public String getSignature(String raw) {
                return UpYunUtils.md5(raw + KEY);
            }
        };

        manager.upload(temp, paramsMap, KEY, completeListener, progressListener);
        manager.upload(temp, paramsMap, signatureListener, completeListener, progressListener);
        manager.formUpload(temp, paramsMap, KEY, completeListener, progressListener);
        manager.formUpload(temp, paramsMap, signatureListener, completeListener, progressListener);
        manager.blockUpload(temp, paramsMap, KEY, completeListener, progressListener);
        manager.blockUpload(temp, paramsMap, signatureListener, completeListener, progressListener);

        manager.upload(temp, paramsMap, KEY + 1, faileCompleteListener, progressListener);
        manager.formUpload(temp, paramsMap, KEY + 1, faileCompleteListener, progressListener);
        manager.blockUpload(temp, paramsMap, KEY + 1, faileCompleteListener, progressListener);

        paramsMap.put(Params.BUCKET, SPACE + 1);
        manager.upload(temp, paramsMap, signatureListener, faileCompleteListener, progressListener);
        manager.formUpload(temp, paramsMap, signatureListener, faileCompleteListener, progressListener);
        manager.blockUpload(temp, paramsMap, signatureListener, faileCompleteListener, progressListener);
        latch.await();
    }
}