package com.upyun.library.common;


import com.upyun.library.listener.UpProgressListener;

import okhttp3.RequestBody;


public class ProgressHelper {
    public ProgressHelper() {
    }

    public static ProgressRequestBody addProgressListener(RequestBody requestBody, UpProgressListener progressRequestListener) {
        return new ProgressRequestBody(requestBody, progressRequestListener);
    }
}

