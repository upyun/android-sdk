package com.upyun.library.utils;

import android.os.Looper;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;

public class AsyncRunTest extends TestCase {
    long testThreadId1;
    long testThreadId2;

    public void testRun() throws Exception {
        final long mainThreadId = Looper.getMainLooper().getThread().getId();
        final CountDownLatch latchMain = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                final CountDownLatch latch = new CountDownLatch(1);
                testThreadId1 = Thread.currentThread().getId();
                assertTrue(mainThreadId != testThreadId1);
                AsyncRun.run(new Runnable() {
                    @Override
                    public void run() {
                        testThreadId2 = Thread.currentThread().getId();
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertTrue(mainThreadId == testThreadId2);
                latchMain.countDown();
            }
        }.start();
        latchMain.await();
    }
}