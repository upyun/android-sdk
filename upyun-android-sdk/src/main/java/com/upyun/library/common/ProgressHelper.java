package com.upyun.library.common;


import com.squareup.okhttp.RequestBody;
import com.upyun.library.listener.UpProgressListener;


public class ProgressHelper {
    public ProgressHelper() {
    }

    public static ProgressRequestBody addProgressListener(RequestBody requestBody, UpProgressListener progressRequestListener) {
        return new ProgressRequestBody(requestBody, progressRequestListener);
    }
}

