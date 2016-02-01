package com.upyun.library.listener;

import junit.framework.TestCase;

public class SignatureListenerTest extends TestCase {

    public void testSignatureListener(){
        SignatureListener listener=new SignatureListener(){
            @Override
            public String getSignature(String policy) {
                return policy;
            }
        };
        assertEquals("test", listener.getSignature("test"));
    }
}