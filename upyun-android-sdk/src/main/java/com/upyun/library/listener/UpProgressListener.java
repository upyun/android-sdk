package com.upyun.library.listener;

public interface UpProgressListener {
    void onRequestProgress(long bytesWrite, long contentLength);
}
