package com.upyun.library.common;

import junit.framework.TestCase;

public class UploadClientTest extends TestCase {

//    UploadClient client = new UploadClient();
//    String url = "http://httpbin.org/post";
//
//    public void testFromUpLoad() throws Exception {
//        UpProgressListener progressListener = new UpProgressListener() {
//            @Override
//            public void onRequestProgress(long bytesWrite, long contentLength) {
//                assertNotNull(bytesWrite);
//                assertNotNull(contentLength);
//                assertTrue(bytesWrite <= contentLength);
//            }
//        };
//        File temp = File.createTempFile("ymm", "test");
//        temp.deleteOnExit();
//        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp));
//        outputStream.write("just for test !".getBytes());
//        outputStream.close();
//        String result = client.fromUpLoad(temp, url, "policy", "signature", progressListener);
//        assertNotNull(result);
//
//        JSONObject object = new JSONObject(result);
//        JSONObject files = object.getJSONObject("files");
//        assertEquals("just for test !", files.get("file"));
//
//        JSONObject form = object.getJSONObject("form");
//        assertEquals("policy", form.get("policy"));
//        assertEquals("signature", form.get("signature"));
//    }
//
//    public void testPost() throws Exception {
//        Map<String, String> paramMap = new HashMap<>();
//        paramMap.put("policy", "policy");
//        paramMap.put("signature", "signature");
//        String result = client.post(url, paramMap);
//        assertNotNull(result);
//
//        JSONObject object = new JSONObject(result);
//        JSONObject form = object.getJSONObject("form");
//        assertEquals("policy", form.get("policy"));
//        assertEquals("signature", form.get("signature"));
//
//    }
//
//    public void testBlockMultipartPost() throws Exception {
//        Map<String, String> paramMap = new HashMap<>();
//        paramMap.put("policy", "policy");
//        paramMap.put("signature", "signature");
//        PostData postData = new PostData();
//        postData.data = "just for test !".getBytes();
//        postData.fileName = "testFile";
//        postData.params = paramMap;
//        String result = client.blockMultipartPost(url, postData);
//        assertNotNull(result);
//
//        JSONObject object = new JSONObject(result);
//        JSONObject files = object.getJSONObject("files");
//        assertEquals("just for test !", files.get("file"));
//
//        JSONObject form = object.getJSONObject("form");
//        assertEquals("policy", form.get("policy"));
//        assertEquals("signature", form.get("signature"));
//    }
}