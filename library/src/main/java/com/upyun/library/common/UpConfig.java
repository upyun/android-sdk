package com.upyun.library.common;

public class UpConfig {
    public static int BLOCK_SIZE = 500 * 1024;
    public static long FILE_BOUND = 4 * 1024 * 1024;
    public static int CONCURRENCY = 2;

    //超时设置 单位秒(SECONDS)
    public static int CONNECT_TIMEOUT=10;
    public static int READ_TIMEOUT=10;
    public static int WRITE_TIMEOUT=10;
}
