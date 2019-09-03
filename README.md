# UPYUN Android SDK

[![Build Status](https://travis-ci.org/upyun/android-sdk.svg?branch=master)](https://travis-ci.org/upyun/android-sdk)
[ ![Download](https://api.bintray.com/packages/upyun/maven/upyun-android-sdk/images/download.svg) ](https://bintray.com/upyun/maven/upyun-android-sdk/_latestVersion)

UPYUN Android SDK, 集成：
- [UPYUN HTTP FORM 接口](http://docs.upyun.com/api/form_api/)
- [UPYUN 分块上传接口](http://docs.upyun.com/api/multipart_upload/)
- [UPYUN 断点续传接口](http://docs.upyun.com/api/rest_api/#_7)

**注：1.XX SDK 服务器验证策略已过期，推荐使用最新 2.0 上传方式**

## 使用说明：

1.SDK 依赖 [okhttp](http://square.github.io/okhttp/)。

2.SDK 已经上传 Jcenter，Android Studio 的用户可以直接在 gradle 中添加一条 dependencies:

```
implementation 'com.upyun:upyun-android-sdk:2.1.1'
```

3.DEMO 示例在 app module 下的 [MainActivity](https://github.com/upyun/android-sdk/blob/master/app/src/main/java/com/upyun/sdktest/MainActivity.java)。


## 参数设置

在 [UpConfig](https://github.com/upyun/android-sdk/blob/master/upyun-android-sdk/src/main/java/com/upyun/library/common/UpConfig.java) 中可以对 SDK 的一些参数进行配置。

* `BLOCK_SIZE` 单个分块大小
* `FILE_BOUND` 自动判断使用分块或者表单上传的文件大小界限
* `CONCURRENCY` 上传线程并发数量
* `EXPIRATION` 默认过期时间偏移量（秒）
* `FORM_HOST` 表单上传 HOST
* `BLOCK_HOST` 分块上传 HOST
* `RETRY_TIME` 失败重传次数
* `CONNECT_TIMEOUT` 连接超时（秒）
* `READ_TIMEOUT` 读超时（秒）
* `WRITE_TIMEOUT` 写超时（秒）


## 上传接口

### 表单上传 (新)
```
//表单上传（本地签名方式）
UploadEngine.getInstance().formUpload(temp, paramsMap, OPERATER, UpYunUtils.md5(PASSWORD), completeListener, progressListener);
//表单上传（服务器签名方式）
UploadEngine.getInstance().formUpload(temp, policy, OPERATER, signature, completeListener, progressListener);

```

参数说明：

* `temp`  上传文件
* `paramsMap `  参数键值对
* `OPERATER ` 操作员
* `PASSWORD `  操作员密码（MD5后传入）
* `completeListener`  结束回调(回调到 UI 线程，不可为 NULL)
* `progressListener`  进度条回调(回调到 UI 线程，可为 NULL)
* `policy`  从服务器获取的 policy（生成规则见[官网文档](http://docs.upyun.com/api/authorization/)）
* `progressListener`  从服务器获取的 signature（生成规则见[官网文档](http://docs.upyun.com/api/authorization/)）

### 串行式断点续传

```
//初始化断点续传
SerialUploader serialUploader = new SerialUploader(SPACE,OPERATER,UpYunUtils.md5(PASSWORD));

//设置 MD5 校验(服务端签名方式不可校验 MD5)
serialUploader.setCheckMD5(true);

//设置进度监听
serialUploader.setOnProgressListener(new ResumeUploader.OnProgressListener() {
      @Override
      public void onProgress(int index, int total) {
      }
});

//暂停
serialUploader.pause()

//开始断点续传，可用方法 1 或方法 2
//方法 1
serialUploader.upload(final File file, final String uploadPath, final Map<String, String> restParams, final UpCompleteListener completeListener)

//方法 2
serialUploader.upload(final File file, final String uploadPath, final Map<String, String> restParams, final Map<String, Object> processParam, final UpCompleteListener completeListener)

```

### 并行式断点续传

```
//初始化断点续传
ParallelUploader parallelUploader = new ParallelUploader(SPACE,OPERATER,UpYunUtils.md5(PASSWORD));

//初始化断点续传 (服务端签名可用)
ParallelUploader parallelUploader = new ParallelUploader();

//设置 MD5 校验(服务端签名方式不可校验 MD5)
parallelUploader(true);

//设置进度监听
parallelUploader(new ResumeUploader.OnProgressListener() {
      @Override
      public void onProgress(int index, int total) {
      }
});

//暂停
parallelUploader.pause()

//开始断点续传，可用方法 1 或方法 2
//方法 1
parallelUploader(final File file, final String uploadPath, final Map<String, String> restParams, final UpCompleteListener completeListener)

//方法 2
parallelUploader(final File file, final String uploadPath, final Map<String, String> restParams, final Map<String, Object> processParam, final UpCompleteListener completeListener)

//服务端签名方式
parallelUploader.upload(final File file, final String uri, final String date, final String signature, final Map<String, String> restParams, final UpCompleteListener completeListener)

```

参数说明：

* `file `  上传文件
* `uploadPath `  上传路径
* `restParams ` rest api 上传预处理参数可为空 （详见[文档](http://docs.upyun.com/api/rest_api/#_17)）
* `processParam `  异步音视频处理参数 （详见[文档](http://docs.upyun.com/cloud/av/)）
* `completeListener`  结束回调(回调到 UI 线程，不可为 NULL)

服务器签名参数：

* `uri `  请求路径(带空间名)
* `date `  请求日期时间
* `signature` 服务端签名


### 表单上传（旧）

```
UploadManager.getInstance().formUpload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
UploadManager.getInstance().formUpload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
```

使用该方法，直接选择通过表单上传方式上传文件。

参数说明：

* `localFilePath`  文件路径
* `paramsMap`  参数键值对
* `KEY`  表单 API 验证密钥（form_api_secret）
* `signatureListener`  获取签名回调
* `completeListener`  结束回调(回调到 UI 线程，不可为 NULL)
* `progressListener`  进度条回调(回调到 UI 线程，可为 NULL)


两种上传方式可根据自己情况选择一种，`KEY` 用户可直接保存在客户端，`signatureListener` 用户可以通过请求服务器获取签名返回客户端。`signatureListener` 回调接口规则如下：

```
SignatureListener signatureListener=new SignatureListener() {
    @Override
    public String getSignature(String raw) {
        return UpYunUtils.md5(raw+KEY);
    }
};
```
将参数 `raw` 传给后台服务器和表单密匙连接后做一次 md5 运算返回结果。

参数键值对中 `Params.BUCKET`（上传空间名）和 `Params.SAVE_KEY` 或 `Params.PATH`（保存路径，任选一个）为必选参数，
其他可选参数见 [Params](https://github.com/upyun/android-sdk/blob/master/upyun-android-sdk/src/main/java/com/upyun/library/common/Params.java) 或者[官网 API 文档](http://docs.upyun.com/api/form_api/)。

### 分块上传（旧）

```
UploadManager.getInstance().blockUpload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
UploadManager.getInstance().blockUpload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
```

使用该方法，直接选择通过分块上传方式上传文件。
参数说明：同上。

## 测试

```
./gradlew connectedAndroidTest
```
 

## 错误码说明

请参照 [API 错误码表](http://docs.upyun.com/api/errno/#api)

## 兼容性

Android 2.3（API10） 以上环境