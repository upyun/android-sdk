package com.upyun.library.exception;

import junit.framework.TestCase;

public class UpYunExceptionTest extends TestCase {

    public void testException() {
        UpYunException exception = new UpYunException("test");
        try {
            throw exception;
        } catch (UpYunException e) {
            assertNotNull(e);
            assertEquals("test",e.getMessage());
        }
    }
}