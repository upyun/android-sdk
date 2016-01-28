package com.upyun.library.listener;

import junit.framework.TestCase;

public class UpProgressListenerTest extends TestCase {

    public void testOnRequestProgress() throws Exception {
        final long write = 1000;
        final long length = 5000;
        UpProgressListener listener = new UpProgressListener() {
            @Override
            public void onRequestProgress(long bytesWrite, long contentLength) {
                assertEquals(write,bytesWrite);
                assertEquals(length,contentLength);
            }
        };
        listener.onRequestProgress(write,length);
    }
}