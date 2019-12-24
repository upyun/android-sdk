package com.upyun.library.listener;

import okhttp3.Response;

public interface UpCompleteListener {
    void onComplete(boolean isSuccess, Response response, Exception error);
}