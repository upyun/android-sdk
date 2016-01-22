package com.upyun.library.common;

import android.util.Log;

import com.upyun.library.exception.UpYunException;
import com.upyun.library.listener.SignatureListener;
import com.upyun.library.listener.UpCompleteListener;
import com.upyun.library.listener.UpProgressListener;
import com.upyun.library.utils.UpYunUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockUploader implements Runnable {
    private String bucket;
    private int blockSize = 500 * 1024;
    private long expiration = Calendar.getInstance().getTimeInMillis() + 60 * 1000; // 60s
    private UpProgressListener progressListener = null;
    private UpCompleteListener completeListener = null;
    private File file;
    private UploadClient client;

    private String userPolicy;
    private String userSignature;
    private int totalBlockNum;
    private String saveToken;
    private String tokenSecret;
    private RandomAccessFile randomAccessFile = null;
    private long fileSize;
    private int[] blockIndex;
    private PostData postData;
    private Map<String, Object> params;
    private String apiKey;
    private SignatureListener signatureListener;

    public static final String INIT_REQUEST = "INIT_REQUEST";  //初始化请求
    public static final String BLOCK_UPLOAD = "BLOCK_UPLOAD";  //分块上传
    public static final String MERGE_REQUEST = "MERGE_REQUESt";//合并请求


    public BlockUploader(UploadClient client, File file,
                         Map<String, Object> params, String apiKey, UpCompleteListener completeListener, UpProgressListener progressListener) {

        this.file = file;
        this.params = params;
        this.client = client;
        this.bucket = (String) params.remove(Params.BUCKET);
        this.progressListener = progressListener;
        this.completeListener = completeListener;
        this.apiKey = apiKey;
    }

    public BlockUploader(UploadClient client, File file, Map<String, Object> params, SignatureListener signatureListener, UpCompleteListener completeListener, UpProgressListener progressListener) {


        this.file = file;
        this.client = client;
        this.bucket = (String) params.get(Params.BUCKET);
        this.progressListener = progressListener;
        this.completeListener = completeListener;
        this.params = params;
        this.signatureListener = signatureListener;
    }


    @Override
    public void run() {
        try {
            params.put(Params.BLOCK_NUM, UpYunUtils.getBlockNum(file, UpConfig.BLOCK_SIZE));
            params.put(Params.FILE_SIZE, file.length());
            params.put(Params.FILE_MD5, UpYunUtils.md5Hex(file));
            String path = (String) params.remove(Params.SAVE_KEY);
            params.put("path", path);
            this.userPolicy = UpYunUtils.getPolicy(params);
            if (apiKey != null) {
                this.userSignature = UpYunUtils.getSignature(params, apiKey);
            } else if (signatureListener != null) {
                this.userSignature = signatureListener.getSignature(getParamsString(params));
            } else {
                throw new RuntimeException("apikey 和 signatureListener 不可都为空");
            }

            this.randomAccessFile = new RandomAccessFile(this.file, "r");
            this.fileSize = this.file.length();
            this.totalBlockNum = UpYunUtils.getBlockNum(this.file, this.blockSize);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("文件不存咋", e);
        }
        nextTask(INIT_REQUEST, -1);
    }

    /**
     * index 为 blockIndex的下标
     *
     * @param type
     * @param index
     */
    private void nextTask(final String type, final int index) {
        if (INIT_REQUEST.equals(type)) {

            Map<String, String> paramMap = new LinkedHashMap<>();
            paramMap.put(Params.POLICY, userPolicy);
            paramMap.put(Params.SIGNATURE, userSignature);
            try {
                String response = client.blockPost(bucket, paramMap);
                JSONObject initialResult = new JSONObject(response);
                saveToken = initialResult.optString(Params.SAVE_TOKEN);
                tokenSecret = initialResult.optString(Params.TOKEN_SECRET);
                JSONArray array = initialResult.getJSONArray(Params.STATUS);
                blockIndex = getBlockIndex(array);

                if (blockIndex.length == 0) {
                    nextTask(MERGE_REQUEST, -1);
                    return;
                }

                if (blockIndex.length != 0) {
                    // 上传分块
                    nextTask(BLOCK_UPLOAD, 0);
                }
            } catch (Exception e) {
                completeListener.onComplete(false, e.getMessage());
            }
        } else if (MERGE_REQUEST.equals(type)) {
            HashMap<String, Object> paramsMapFinish = new HashMap<String, Object>();
            paramsMapFinish.put(Params.EXPIRATION, expiration);
            paramsMapFinish.put(Params.SAVE_TOKEN, saveToken);
            String policyForMerge = UpYunUtils.getPolicy(paramsMapFinish);
            String signatureForMerge = UpYunUtils.getSignature(paramsMapFinish, tokenSecret);


            Map<String, String> paramMap = new LinkedHashMap<>();
            paramMap.put(Params.POLICY, policyForMerge);
            paramMap.put(Params.SIGNATURE, signatureForMerge);

            try {
                String response = client.blockPost(bucket, paramMap);
                completeListener.onComplete(true, response);
            } catch (Exception e) {
                completeListener.onComplete(false, e.getMessage());
            }
        } else if (BLOCK_UPLOAD.equals(type)) {
            postData = new PostData();
            try {
                postData.data = readBlockByIndex(index);
            } catch (UpYunException e) {
                completeListener.onComplete(false, e.getMessage());
            }

            HashMap<String, Object> policyMap = new HashMap<>();
            policyMap.put(Params.SAVE_TOKEN, saveToken);
            policyMap.put(Params.EXPIRATION, expiration);
            policyMap.put(Params.BLOCK_INDEX, blockIndex[index]);
            policyMap.put(Params.BLOCK_MD5, UpYunUtils.md5(postData.data));
            String policy = UpYunUtils.getPolicy(policyMap);
            String signature = UpYunUtils.getSignature(policyMap, this.tokenSecret);

            Map<String, String> map = new HashMap<String, String>();
            map.put(Params.POLICY, policy);
            map.put(Params.SIGNATURE, signature);
            postData.fileName = file.getName();
            postData.params = map;

            try {
                client.blockMutipartPost(this.bucket, postData);
                if (progressListener != null) {
                    progressListener.onRequestProgress(index + 1, blockIndex.length);
                }
                if (index == (blockIndex.length - 1)) {
                    nextTask(MERGE_REQUEST, -1);
                } else {
                    nextTask(BLOCK_UPLOAD, index + 1);
                }
            } catch (Exception e) {
                completeListener.onComplete(false, e.getMessage());
            }
        }
    }


    /**
     * 从文件中读取块
     * <p/>
     * index begin at 0
     *
     * @param index
     * @return
     * @throws IOException
     */
    private byte[] readBlockByIndex(int index) throws UpYunException {
        if (index > this.totalBlockNum) {
            Log.e("Block index error", "the index is bigger than totalBlockNum.");
            throw new UpYunException("readBlockByIndex: the index is bigger than totalBlockNum.");
        }
        byte[] block = new byte[this.blockSize];
        int readedSize = 0;
        try {
            int offset;
            if (blockIndex[index] == 0) {
                offset = 0;
            } else {
                offset = (blockIndex[index]) * blockSize;
            }
            randomAccessFile.seek(offset);
            readedSize = randomAccessFile.read(block, 0, blockSize);
        } catch (IOException e) {
            throw new UpYunException(e.getMessage());
        }

        // read last block, adjust byte size
        if (readedSize < blockSize) {
            byte[] notFullBlock = new byte[readedSize];
            System.arraycopy(block, 0, notFullBlock, 0, readedSize);
            return notFullBlock;
        }
        return block;
    }

    /**
     * 获取没有上传的分块下标
     *
     * @param array
     * @return
     * @throws JSONException
     */
    private int[] getBlockIndex(JSONArray array) throws JSONException {
        int size = 0;
        for (int i = 0; i < array.length(); i++) {
            if (array.getInt(i) == 0) {
                size++;
            }
        }
        // 获取未上传的块下标
        int[] blockIndex = new int[size];
        int index = 0;
        for (int i = 0; i < array.length(); i++) {
            if (array.getInt(i) == 0) {
                blockIndex[index] = i;
                index++;
            }
        }
        return blockIndex;
    }

    private String getParamsString(Map<String, Object> params) {
        Object[] keys = params.keySet().toArray();
        Arrays.sort(keys);
        StringBuffer tmp = new StringBuffer("");
        for (Object key : keys) {
            tmp.append(key).append(params.get(key));
        }
        return tmp.toString();
    }

}
