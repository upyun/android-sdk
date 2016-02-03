package com.upyun.library.common;

import android.util.Log;

import com.upyun.library.exception.RespException;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockUploader implements Runnable {
    private static final String TAG = "BlockUploader";
    private String bucket;
    private String url;
    private long expiration;
    private UpProgressListener progressListener;
    private UpCompleteListener completeListener;
    private File file;
    private UploadClient client;

    private String userPolicy;
    private String userSignature;
    private int totalBlockNum;
    private String saveToken;
    private String tokenSecret;
    private RandomAccessFile randomAccessFile = null;
    private int[] blockIndex;
    private PostData postData;
    private Map<String, Object> params;
    private String apiKey;
    private SignatureListener signatureListener;
    private int retryTime;

    public BlockUploader(UploadClient upLoaderClient, File file, Map<String, Object> localParams, String apiKey, SignatureListener signatureListener, UpCompleteListener uiCompleteListener, UpProgressListener uiProgressListener) {
        this.client = upLoaderClient;
        this.file = file;
        this.params = localParams;
        this.progressListener = uiProgressListener;
        this.completeListener = uiCompleteListener;
        this.apiKey = apiKey;
        this.signatureListener = signatureListener;
    }

    @Override
    public void run() {
        try {
            this.bucket = (String) params.remove(Params.BUCKET);
            this.url = UpConfig.BLOCK_HOST + "/" + this.bucket;
            this.expiration = (long) params.get(Params.EXPIRATION);
            params.put(Params.BLOCK_NUM, UpYunUtils.getBlockNum(file, UpConfig.BLOCK_SIZE));
            params.put(Params.FILE_SIZE, file.length());
            params.put(Params.FILE_MD5, UpYunUtils.md5Hex(file));
            String save_path = (String) params.remove(Params.SAVE_KEY);
            String path = (String) params.get(Params.PATH);
            if (save_path != null && path == null) {
                params.put(Params.PATH, save_path);
            }
            this.userPolicy = UpYunUtils.getPolicy(params);
            if (apiKey != null) {
                this.userSignature = UpYunUtils.getSignature(params, apiKey);
            } else if (signatureListener != null) {
                this.userSignature = signatureListener.getSignature(getParamsString(params));
            } else {
                throw new RuntimeException("apikey 和 signatureListener 不可都为null");
            }

            this.randomAccessFile = new RandomAccessFile(this.file, "r");
            this.totalBlockNum = UpYunUtils.getBlockNum(this.file, UpConfig.BLOCK_SIZE);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("文件不存在", e);
        }
        initRequest();
    }

    private void blockUpload(int index) {

        while (true) {
            if (postData == null) {
                postData = new PostData();
            }
            try {
                postData.data = readBlockByIndex(index);
            } catch (UpYunException e) {
                completeListener.onComplete(false, e.toString());
            }

            HashMap<String, Object> policyMap = new HashMap<>();
            policyMap.put(Params.SAVE_TOKEN, saveToken);
            policyMap.put(Params.EXPIRATION, expiration);
            policyMap.put(Params.BLOCK_INDEX, blockIndex[index]);
            policyMap.put(Params.BLOCK_MD5, UpYunUtils.md5(postData.data));
            String policy = UpYunUtils.getPolicy(policyMap);
            String signature = UpYunUtils.getSignature(policyMap, this.tokenSecret);

            Map<String, String> map = new HashMap<>();
            map.put(Params.POLICY, policy);
            map.put(Params.SIGNATURE, signature);
            postData.fileName = file.getName();
            postData.params = map;

            try {
                client.blockMultipartPost(this.url, postData);
                if (progressListener != null) {
                    progressListener.onRequestProgress(index, blockIndex.length);
                }

                if (index++ == (blockIndex.length - 1)) {
                    megreRequest();
                    break;
                }
            } catch (IOException | RespException e) {
                if (++retryTime > UpConfig.RETRY_TIME || (e instanceof RespException && ((RespException) e).code() / 100 != 5)) {
                    completeListener.onComplete(false, e.toString());
                    break;
                }
            } finally {
                postData = null;
            }
        }
    }

    private void megreRequest() {
        HashMap<String, Object> paramsMapFinish = new HashMap<>();
        paramsMapFinish.put(Params.EXPIRATION, expiration);
        paramsMapFinish.put(Params.SAVE_TOKEN, saveToken);
        String policyForMerge = UpYunUtils.getPolicy(paramsMapFinish);
        String signatureForMerge = UpYunUtils.getSignature(paramsMapFinish, tokenSecret);

        Map<String, String> paramMap = new LinkedHashMap<>();
        paramMap.put(Params.POLICY, policyForMerge);
        paramMap.put(Params.SIGNATURE, signatureForMerge);

        try {
            String response = client.post(url, paramMap);
            progressListener.onRequestProgress(blockIndex.length, blockIndex.length);
            completeListener.onComplete(true, response);
        } catch (IOException | RespException e) {
            if (++retryTime > UpConfig.RETRY_TIME || (e instanceof RespException && ((RespException) e).code() / 100 != 5)) {
                completeListener.onComplete(false, e.toString());
            } else {
                megreRequest();
            }
        }
    }

    private void initRequest() {
        Map<String, String> paramMap = new LinkedHashMap<>();
        paramMap.put(Params.POLICY, userPolicy);
        paramMap.put(Params.SIGNATURE, userSignature);
        try {
            String response = client.post(url, paramMap);
            JSONObject initialResult = new JSONObject(response);
            saveToken = initialResult.optString(Params.SAVE_TOKEN);
            tokenSecret = initialResult.optString(Params.TOKEN_SECRET);
            JSONArray array = initialResult.getJSONArray(Params.STATUS);
            blockIndex = getBlockIndex(array);

            if (blockIndex.length == 0) {
                megreRequest();
            } else {
                // 上传分块
                blockUpload(0);
            }
        } catch (IOException | RespException e) {
            if (++retryTime > UpConfig.RETRY_TIME || (e instanceof RespException && ((RespException) e).code() / 100 != 5)) {
                completeListener.onComplete(false, e.toString());
            } else {
                initRequest();
            }
        } catch (JSONException e) {
            throw new RuntimeException("json 解析出错", e);
        }
    }


    /**
     * 从文件中读取块
     * <p/>
     * index begin at 0
     *
     * @param index
     * @return
     * @throws UpYunException
     */
    private byte[] readBlockByIndex(int index) throws UpYunException {
        if (index > this.totalBlockNum) {
            Log.e("Block index error", "the index is bigger than totalBlockNum.");
            throw new UpYunException("readBlockByIndex: the index is bigger than totalBlockNum.");
        }
        byte[] block = new byte[UpConfig.BLOCK_SIZE];
        int readedSize = 0;
        try {
            int offset = (blockIndex[index]) * UpConfig.BLOCK_SIZE;
            randomAccessFile.seek(offset);
            readedSize = randomAccessFile.read(block, 0, UpConfig.BLOCK_SIZE);
        } catch (IOException e) {
            throw new UpYunException(e.getMessage());
        }

        // read last block, adjust byte size
        if (readedSize < UpConfig.BLOCK_SIZE) {
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
