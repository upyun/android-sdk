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

@Deprecated
public class UploadManager {
    private static UploadManager instance;
    private ExecutorService executor;
    private UploadClient upLoaderClient;

    enum UploadType {
        FORM, BLOCK
    }

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

    protected void upload(final File file, final Map<String, Object> params, String apiKey, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(file, params, apiKey, null, completeListener, progressListener);
    }

    protected void upload(final File file, final Map<String, Object> params, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(file, params, null, signatureListener, completeListener, progressListener);
    }

    public void formUpload(final File file, final Map<String, Object> params, String apiKey, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(UploadType.FORM, file, params, apiKey, null, completeListener, progressListener);
    }

    public void formUpload(final File file, final Map<String, Object> params, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(UploadType.FORM, file, params, null, signatureListener, completeListener, progressListener);
    }

    public void blockUpload(final File file, final Map<String, Object> params, String apiKey, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(UploadType.BLOCK, file, params, apiKey, null, completeListener, progressListener);
    }

    public void blockUpload(final File file, final Map<String, Object> params, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        this.upload(UploadType.BLOCK, file, params, null, signatureListener, completeListener, progressListener);
    }

    protected void upload(UploadType type, final File file, final Map<String, Object> params, String apiKey, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        if (file == null) {
            completeListener.onComplete(false, "文件不可以为空");
            return;
        } else if (params == null) {
            completeListener.onComplete(false, "参数不可为空");
            return;
        } else if (apiKey == null && signatureListener == null) {
            completeListener.onComplete(false, "APIkey和signatureListener不可同时为null");
            return;
        } else if (completeListener == null) {
            throw new RuntimeException("completeListener 不可为null");
        }

        if (params.get(Params.BUCKET) == null) {
            params.put(Params.BUCKET, UpConfig.BUCKET);
        }

        if (params.get(Params.EXPIRATION) == null) {
            params.put(Params.EXPIRATION, System.currentTimeMillis() / 1000 + UpConfig.EXPIRATION);
        }

        UpProgressListener uiProgressListener = new UpProgressListener() {
            @Override
            public void onRequestProgress(final long bytesWrite, final long contentLength) {
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        if (progressListener != null) {
                            progressListener.onRequestProgress(bytesWrite, contentLength);
                        }
                    }
                });
            }
        };

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
        Runnable uploadRunnable = null;
        switch (type) {
            case FORM:
                uploadRunnable = new FormUploader(upLoaderClient, file, localParams, apiKey, signatureListener, uiCompleteListener, uiProgressListener);
                break;
            case BLOCK:
                uploadRunnable = new BlockUploader(upLoaderClient, file, localParams, apiKey, signatureListener, uiCompleteListener, uiProgressListener);
                break;
        }
        executor.execute(uploadRunnable);
    }

    protected void upload(final File file, final Map<String, Object> params, String apiKey, SignatureListener signatureListener, final UpCompleteListener completeListener, final UpProgressListener progressListener) {
        if (file.length() < UpConfig.FILE_BOUND) {
            this.upload(UploadType.FORM, file, params, apiKey, signatureListener, completeListener, progressListener);
        } else {
            this.upload(UploadType.BLOCK, file, params, apiKey, signatureListener, completeListener, progressListener);
        }
    }
}
