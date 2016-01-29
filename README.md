# UPYUN Android SDK

[![Build Status](https://travis-ci.org/upyun/android-sdk.svg?branch=master)](https://travis-ci.org/upyun/android-sdk)

UPYUN Android SDK, 集成：
- [UPYUN HTTP FORM 接口](http://docs.upyun.com/api/form_api/)
- [UPYUN 分块上传接口](http://docs.upyun.com/api/multipart_upload/)


## 使用说明：

1.直接下载 JAR 包复制进项目使用。

2.SDK 已经上传 Jcenter，Android Studio 的用户可以直接在 gradle 中添加一条 dependencies:

```
compile 'com.upyun:library:1.0.2'
```
## 参数设置

在 [UpConfig](https://github.com/upyun/android-sdk/blob/master/library/src/main/java/com/upyun/library/common/UpConfig.java) 中可以对 SDK 的一些参数进行配置。

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

> 详细示例请见 app module 下的 [MainActivity](https://github.com/upyun/android-sdk/blob/master/app/src/main/java/com/upyun/sdktest/MainActivity.java)。


### 文件上传

```
UploadManager.getInstance().upload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
UploadManager.getInstance().upload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
```

使用该方法根据配置文件自动判断文件通过分块还是表单上传。

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
其他可选参数见 [Params](https://github.com/upyun/android-sdk/blob/master/library/src/main/java/com/upyun/library/common/Params.java) 或者[官网 API 文档](http://docs.upyun.com/api/form_api/)。

### 表单上传

```
UploadManager.getInstance().formUpload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
UploadManager.getInstance().formUpload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
```

使用该方法，直接选择通过表单上传方式上传文件。
参数说明：同上。
### 分块上传

```
UploadManager.getInstance().blockUpload(new File(localFilePath), paramsMap, KEY, completeListener, progressListener);
UploadManager.getInstance().blockUpload(new File(localFilePath), paramsMap, signatureListener, completeListener, progressListener);
```

使用该方法，直接选择通过分块上传方式上传文件。
参数说明：同上。

## 测试
./gradlew connectedAndroidTest
 

## 错误码说明

请参照 [API 错误码表](http://docs.upyun.com/api/errno/#api)

## 兼容性

Android 2.3（API10） 以上环境