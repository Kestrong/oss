# oss

一个支持s3/minio/fusion storage/ceph/webhdfs协议的对象存储和文件系统操作sdk，支持spring-boot的starter方式配置和启动，简化开发的流程。同时也提供了rest接口方便调测。

## 模块介绍

* oss-sdk

基础模块，提供s3和文件系统的操作接口，内部适配了s3、minio、fusion storage、ceph、filesystem、webhdfs等多种存储服务的接口，可以自由拓展。

* spring-boot-starter-oss

支持spring-boot的配置，简化配置和提高开发效率。

* oss-application

提供rest接口，通过swagger访问。地址：http://{ip}:{port}/{context}/swagger-ui/index.html

## 快速开始

这里以spring-boot为例。

* 添加依赖

```
    compile 'com.xjbg:spring-boot-starter-oss:${oss-version}'
```
```
    <dependency>
        <groupId>com.xjbg</groupId>
        <artifactId>spring-boot-starter-oss</artifactId>
        <version>${oss-version}</version>
    </dependency>
```
* 配置文件
```
oss:
  enable: true #是否启用 默认true
  defaultApiType: minio #默认的api类型 可选[s3,minio,fusion,ceph,filesystem,webhdfs]
  baseDir: / #filesystem的基准目录 如不需要可以去掉该属性
  clientConfig:
    useHttps: false #是否开启https 默认false
  minio: #选择对应的api类型
    defaultBucket: temp #默认bucket 不需要可以不配置
    url: http://play.min.io #服务地址
    accessKey: Q3AM3UQ867SPQQA43P2F
    secretKey: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
    enable: true #是否启用minio 默认false
    autoCteateBucket: false #是否启用自动创建bucket 默认false
```

* 接口使用
```
    @Autowired
    protected OssTemplate ossTemplate;
    
    //三种方式获取api后直接使用需要的接口
    OssApi ossApi = ossTemplate.fileApi();// 显式指定
    OssApi ossApi=ossTemplate.api();//使用配置文件指定的默认类型
    OssApi ossApi=ossTemplate.api(ApiType.MINIO);// 根据类型获取
    //先创建bucket才能操作文件和目录
    ossApi.makeBucket(bucket);
    //上传文件
    try (InputStream inputStream = new FileInputStream(new File(directory))) {
        PutObjectArgs putObjectRequest = PutObjectArgs.builder()
                .bucket(bucket)
                .object(object)
                .contentLength(inputStream.available())
                .contentType(contentType)
                .inputStream(inputStream)
                .build();
        PutObjectResponse response = ossApi.putObject(putObjectRequest);
        log.info("putObject:{}", JSON.toJSONString(response));
    }
```

更多代码示例请查看[测试用例](/oss-application/src/test/java/com/xjbg/oss/application/OSSTest.java)。

## 注意事项

* webhdfs使用的是okhttp进行rest调用，其它满足s3协议的使用aws-java-sdk调用（底层为http-client）
* s3协议的bucket为存储桶，命名规则为```[a-zA-Z0-9.-]{3,63}```，为了方便使用内部会自动取'/'或'\\'的第一级作为bucket，剩下的部分会拼接到object的前面
* 使用https时webhdfs需要配置```SSL_CERT_FILE```其他类型需要配置```javax.net.ssl.keyStore```、```javax.net.ssl.keyStorePassword```和```javax.net.ssl.keyStoreType```系统变量(System Property，例如通过Java -D启动命令指定)
* 严格控制好目录和文件的命名，避免文件、目录之间命名冲突导致未知的异常
* 例子(linux系统)：
   * ```文件 bucket:tmp object:log/oss.log result: /tmp/log/oss.log ```
   
## 参考文档

1. [s3官方文档](https://amazonaws-china.com/cn/s3/)
2. [webhdfs官方文档](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/WebHDFS.html)