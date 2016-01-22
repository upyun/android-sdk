package com.upyun.library.common;

import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.AsyncRun;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadManager {
    private static UploadManager instance;
    private ExecutorService executor;
    private UploadClient upLoaderClient;

    private UploadManager() {
        executor = Executors.newFixedThreadPool(UpConfig.CONCURRENCY);
        upLoaderClient = new UploadClient();
    }

    public static UploadManager getInstance() {
        if (instance == null) {
            synchronized (UploadManager.class) {
                if (instance == null) {
                    instance = new UploadManager();
                }
            }
        }
        return instance;
    }

    public void upload(final File file, final Map<String, Object> params, String apiKey, final UpCompleteListener completeListener, final UpProgressListener progressListener) {

        if (file == null) {
            completeListener.onComplete(false, "文件不可以为空");
            return;
        } else if (params == null) {
            completeListener.onComplete(false, "参数不可为空");
            return;
        } else if (apiKey.isEmpty()) {
            completeListener.onComplete(false, "APIkey或signatureListener不可为空");
            return;
        } else if (completeListener == null) {
            completeListener.onComplete(false, "completeListener不可为空");
        }

        UpProgressListener uiProgressListener = null;

        if (progressListener != null) {
            uiProgressListener = new UpProgressListener() {
                @Override
                public void onRequestProgress(final long bytesWrite, final long contentLength) {
                    AsyncRun.run(new Runnable() {
                        @Override
                        public void run() {
                            progressListener.onRequestProgress(bytesWrite, contentLength);
                        }
                    });
                }
            };
        }

        UpCompleteListener uiCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(final boolean isSuccess, final String result) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        completeListener.onComplete(isSuccess, result);
                    }
                });
            }
        };

        Map<String, Object> localParams = new HashMap<>();
        localParams.putAll(params);

        if (file.length() < UpConfig.FILE_BOUND) {
            FormUploader formUploader = new FormUploader(upLoaderClient, file, localParams, apiKey, uiCompleteListener, uiProgressListener);
            executor.execute(formUploader);
        } else {
            BlockUploader uploader = new BlockUploader(upLoaderClient, file, localParams, apiKey, uiCompleteListener, uiProgressListener);
            executor.execute(uploader);
        }

    }

    public void upload(final File file, final Map<String, Object> params, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {

        if (file == null) {
            completeListener.onComplete(false, "文件不可以为空");
            return;
        } else if (params == null) {
            completeListener.onComplete(false, "参数不可为空");
            return;
        } else if (signatureListener == null) {
            completeListener.onComplete(false, "APIkey或signatureListener不可为空");
            return;
        } else if (completeListener == null) {
            completeListener.onComplete(false, "completeListener不可为空");
        }

        UpProgressListener uiProgressListener = null;

        if (progressListener != null) {
            uiProgressListener = new UpProgressListener() {
                @Override
                public void onRequestProgress(final long bytesWrite, final long contentLength) {
                    AsyncRun.run(new Runnable() {
                        @Override
                        public void run() {
                            progressListener.onRequestProgress(bytesWrite, contentLength);
                        }
                    });
                }
            };
        }

        UpCompleteListener uiCompleteListener = new UpCompleteListener() {
            @Override
            public void onComplete(final boolean isSuccess, final String result) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        completeListener.onComplete(isSuccess, result);
                    }
                });
            }
        };

        Map<String, Object> localParams = new HashMap<>();
        localParams.putAll(params);

        if (file.length() < UpConfig.FILE_BOUND) {
            FormUploader formUploader = new FormUploader(upLoaderClient, file, localParams, signatureListener, uiCompleteListener, uiProgressListener);
            executor.execute(formUploader);
        } else {
            BlockUploader uploader = new BlockUploader(upLoaderClient, file, localParams, signatureListener, uiCompleteListener, uiProgressListener);
            executor.execute(uploader);
        }
    }
}
