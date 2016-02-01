package com.upyun.library.utils;

import android.os.Handler;
import android.os.Looper;

public class AsyncRun {
    public static void run(Runnable r) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(r);
    }
}