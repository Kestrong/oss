package com.xjbg.oss.application;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;
import com.xjbg.oss.api.OssApi;
import com.xjbg.oss.api.OssTemplate;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.application.base.BaseTest;
import com.xjbg.oss.enums.ApiType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author kesc
 * @date 2020-08-10 9:12
 */
@Slf4j
public class OSSTest extends BaseTest {
    @Autowired
    private OssTemplate ossTemplate;
    private OssApi ossApi;
    private String bucket;
    private String object;
    private String object2;
    private String object3;

    @Before
    public void before() {
        ossApi = ossTemplate.api();
        if (!ossApi.apiType().equals(ApiType.FILESYSTEM)) {
            bucket = "itm-minio-test";
            object = "test.txt";
            object2 = "test2/test2.txt";
            object3 = "test3.txt";
        } else {
            bucket = "D://tmp";
            object = "/test.txt";
            object2 = "/test2.txt";
            object3 = "/target/test.txt";
        }
        ossApi.makeBucket(bucket);
    }

    @Test
    public void testSetBucketAcl() {
        SetBucketAclArgs bucketAclArgs = SetBucketAclArgs.builder()
                .bucket(bucket)
                .owner(new Owner("Lancelot", "兰斯洛特"))
                .grantEmail("kestrong@foxmail.com", Permission.FullControl)
                .build();
        ossApi.setBucketAcl(bucketAclArgs);
    }

    @Test
    public void testGetBucketAcl() {
        GetBucketAclArgs bucketAclArgs = GetBucketAclArgs.builder()
                .bucket(bucket)
                .build();
        AclResponse acl = ossApi.getBucketAcl(bucketAclArgs);
        log.info("acl:{}", JSON.toJSONString(acl));
    }

    @Test
    public void testSetBucketPolicy() {
        SetBucketPolicyArgs setBucketPolicyArgs = SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .policyText("{" +
                        "    \"Version\": \"2012-10-17\"," +
                        "    \"Statement\": [" +
                        "    ]" +
                        "}")
                .build();
        ossApi.setBucketPolicy(setBucketPolicyArgs);
    }

    @Test
    public void testGetBucketPolicy() {
        GetBucketPolicyArgs getBucketPolicyArgs = GetBucketPolicyArgs.builder()
                .bucket(bucket)
                .build();
        String bucketPolicy = ossApi.getBucketPolicy(getBucketPolicyArgs);
        log.info("bucketPolicy:{}", bucketPolicy);
    }

    @Test
    public void testMakeBucket() {
        ossApi.makeBucket(bucket);
    }

    @Test
    public void testGetBucket() {
        boolean bucketExist = ossApi.bucketExist(bucket);
        log.info("bucketExist:{}", bucketExist);
        List<BucketResponse> bucketResponses = ossApi.listBuckets(Collections.emptyList());
        log.info("bucketResponses:{}", JSON.toJSONString(bucketResponses));
        BucketResponse bucketResponse = ossApi.getBucket(bucket);
        log.info("bucketResponse:{}", JSON.toJSONString(bucketResponse));
    }

    @Test
    public void testRemoveBucket() {
        ossApi.removeBucket(bucket);
    }

    @Test
    public void testSetObjectAcl() {
        SetObjectAclArgs objectAclArgs = SetObjectAclArgs.builder()
                .bucket(bucket)
                .object(object)
                .owner(new Owner("Lancelot", "兰斯洛特"))
                .grantEmail("kestrong@foxmail.com", Permission.FullControl)
                .build();
        ossApi.setObjectAcl(objectAclArgs);
    }

    @Test
    public void testGetObjectAcl() {
        GetObjectAclArgs bucketAclArgs = GetObjectAclArgs.builder()
                .bucket(bucket)
                .object(object)
                .build();
        AclResponse acl = ossApi.getObjectAcl(bucketAclArgs);
        log.info("acl:{}", JSON.toJSONString(acl));
    }

    @Test
    public void testPutObject() throws IOException {
        String directory = "D://tmp/test.txt";
        String contentType = "text/plain";

        try (InputStream inputStream = new FileInputStream(new File(directory))) {
            PutObjectArgs putObjectRequest = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .contentLength((long) inputStream.available())
                    .contentType(contentType)
                    .inputStream(inputStream)
                    .build();
            PutObjectResponse response = ossApi.putObject(putObjectRequest);
            log.info("putObject:{}", JSON.toJSONString(response));
        }

        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket(bucket)
                .object(object2)
                .contentType(contentType)
                .fileName(directory)
                .build();
        List<PutObjectResponse> putObjectResponses = ossApi.uploadObject(uploadObjectArgs);
        log.info("putObjectResponses:{}", JSON.toJSONString(putObjectResponses));

        CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                .srcBucket(bucket)
                .srcObject(object2)
                .bucket(bucket)
                .object(object3)
                .build();
        CopyObjectResponse copyObjectResponse = ossApi.copyObject(copyObjectArgs);
        log.info("copyObject:{}", JSON.toJSONString(copyObjectResponse));
    }

    @Test
    public void testGetObject() throws IOException {
        String objectUrl = ossApi.getObjectUrl(bucket, object);
        log.info("objectUrl:{}", objectUrl);
        ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder().bucket(bucket).build();
        List<ItemResponse> itemResponses = ossApi.listObjects(listObjectsRequest);
        log.info("itemResponses:{}", JSON.toJSONString(itemResponses));
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucket)
                .object(object2)
                .build();
        try (InputStream inputStream = ossApi.getObject(getObjectArgs).getInputStream()) {
            log.info("available size:{}", inputStream.available());
        }
        String fileName = "D://tmp/README.txt";
        DownloadObjectArgs downloadObjectArgs = DownloadObjectArgs.builder()
                .bucket(bucket)
                .object(object2)
                .fileName(fileName)
                .build();
        ossApi.downloadObject(downloadObjectArgs);
    }

    @Test
    public void testDeleteObject() {
        RemoveObjectArgs removeObjectRequest = RemoveObjectArgs.builder()
                .bucket(bucket)
                .objects(Collections.singletonList(object3))
                .build();
        List<RemoveObjectResponse> responses = ossApi.removeObjects(removeObjectRequest);
        log.info("removeObjects:{}", JSON.toJSONString(responses));
    }

    @Test
    public void testPresignedObjectUrl() {
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(object)
                .build();
        String presignedObjectUrl = ossApi.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        log.info("getPresignedObjectUrl:{}", presignedObjectUrl);
    }
}
