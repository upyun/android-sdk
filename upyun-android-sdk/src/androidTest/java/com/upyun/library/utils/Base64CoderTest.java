package com.upyun.library.utils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class Base64CoderTest extends TestCase {

    public void testEncodeString() throws Exception {

        String raw = "{\"bucket\":\"demobucket\",\"expiration\":1409200758,\"save-key\":\"/img.jpg\"}";
        String result = Base64Coder.encodeString(raw);
        Assert.assertEquals("eyJidWNrZXQiOiJkZW1vYnVja2V0IiwiZXhwaXJhdGlvbiI6MTQwOTIwMDc1OCwic2F2ZS1rZXkiOiIvaW1nLmpwZyJ9",result);
    }
}