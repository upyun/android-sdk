package com.upyun.library.common;

import java.util.Calendar;

public class UpConfig {
    public static int BLOCK_SIZE = 500 * 1024;
    public static long EXPIRATION = Calendar.getInstance().getTimeInMillis() + 60 * 1000; // 60s
    public static long FILE_BOUND = 4 * 1024 * 1024;
}
