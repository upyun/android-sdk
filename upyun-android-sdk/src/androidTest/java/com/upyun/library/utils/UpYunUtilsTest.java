package com.upyun.library.utils;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UpYunUtilsTest extends TestCase {

    public void testGetPolicy() throws Exception {

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("save-key", "/img.jpg");
        paramMap.put("expiration", 1409200758);
        paramMap.put("bucket", "demobucket");
        String result = UpYunUtils.getPolicy(paramMap);
//        Assert.assertEquals("eyJzYXZlLWtleSI6IlwvaW1nLmpwZyIsImV4cGlyYXRpb24iOjE0MDkyMDA3NTgsImJ1Y2tldCI6ImRlbW9idWNrZXQifQ==", result);
    }

    public void testGetSignature() throws Exception {

        String policy = "eyJidWNrZXQiOiJkZW1vYnVja2V0IiwiZXhwaXJhdGlvbiI6MTQwOTIwMDc1OCwic2F2ZS1rZXkiOiIvaW1nLmpwZyJ9";
        String secretKey = "cAnyet74l9hdUag34h2dZu8z7gU=";
        Assert.assertEquals("646a6a629c344ce0e6a10cadd49756d4", UpYunUtils.getSignature(policy, secretKey));
    }

    public void testGetSignature1() throws Exception {

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("path", "/demo.png");
        paramMap.put("expiration", 1409200758);
        paramMap.put("file_blocks", 1);
        paramMap.put("file_hash", "b1143cbc07c8e768d517fa5e73cb79ca");
        paramMap.put("file_size", 653252);
        String secretKey = "cAnyet74l9hdUag34h2dZu8z7gU=";
        String result = UpYunUtils.getSignature(paramMap, secretKey);
        Assert.assertEquals("a178e6e3ff4656e437811616ca842c48", result);

    }

    public void testMd5() throws Exception {
        String raw = "eyJidWNrZXQiOiJkZW1vYnVja2V0IiwiZXhwaXJhdGlvbiI6MTQwOTIwMDc1OCwic2F2ZS1rZXkiOiIvaW1nLmpwZyJ9&cAnyet74l9hdUag34h2dZu8z7gU=";
        String result = UpYunUtils.md5(raw);
        Assert.assertEquals("646a6a629c344ce0e6a10cadd49756d4", result);
    }

    public void testMd51() throws Exception {

        String raw = "eyJidWNrZXQiOiJkZW1vYnVja2V0IiwiZXhwaXJhdGlvbiI6MTQwOTIwMDc1OCwic2F2ZS1rZXkiOiIvaW1nLmpwZyJ9&cAnyet74l9hdUag34h2dZu8z7gU=";
        String result = UpYunUtils.md5(raw.toString());
        Assert.assertEquals("646a6a629c344ce0e6a10cadd49756d4", result);
    }

    public void testMd5Hex() throws Exception {

        File temp = File.createTempFile("ymm", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.flush();
        String result = UpYunUtils.md5Hex(temp);
        Assert.assertEquals("87744188c2d82b7203e8b74473591168", result);
        outputStream.write("just for test !".getBytes());
        outputStream.close();
        String result2 = UpYunUtils.md5Hex(temp);
        Assert.assertEquals("52194664b7fa122122b11b4c7dc656a5", result2);
    }

    public void testGetBlockNum() throws Exception {

        File temp = File.createTempFile("ymm", "test");
        temp.deleteOnExit();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
        outputStream.write("just for test !".getBytes());
        outputStream.flush();
        int result = UpYunUtils.getBlockNum(temp, 3);
        Assert.assertEquals(5, result);
        outputStream.write("just for test !".getBytes());
        outputStream.close();
        int result2 = UpYunUtils.getBlockNum(temp, 2);
        Assert.assertEquals(15, result2);
    }

    public void testSig() throws Exception {
        String raw1 = "482c811da5d5b4bc6d497ffa98491e38";
        String raw2 = "POST&/upyun-temp&Wed, 09 Nov 2016 14:26:58 GMT&eyJidWNrZXQiOiAidXB5dW4tdGVtcCIsICJzYXZlLWtleSI6ICIvZGVtby5qcGciLCAiZXhwaXJhdGlvbiI6ICIxNDc4Njc0NjE4IiwgImRhdGUiOiAiV2VkLCA5IE5vdiAyMDE2IDE0OjI2OjU4IEdNVCIsICJjb250ZW50LW1kNSI6ICI3YWM2NmMwZjE0OGRlOTUxOWI4YmQyNjQzMTJjNGQ2NCJ9&7ac66c0f148de9519b8bd264312c4d64";
        byte[] b = UpYunUtils.calculateRFC2104HMACRaw(raw1, raw2);
        String result = Base64Coder.encodeLines(b);
        Assert.assertEquals("DTGOeaCa1yk1JWG4G3DH+u5sI5M=", result);
    }
}