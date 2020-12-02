package com.xjbg.oss.api;

import com.amazonaws.services.s3.model.AccessControlList;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.enums.ApiType;

import java.io.InputStream;
import java.util.List;

/**
 * @author kesc
 * @date 2020-08-06 11:26
 */
public interface OssApi {

    /**
     * 设置bucket权限
     *
     * @param args {@link SetBucketAclArgs}
     */
    void setBucketAcl(SetBucketAclArgs args);

    /**
     * 获取bucket权限
     *
     * @param args {@link GetBucketAclArgs}
     * @return {@link AccessControlList}
     */
    AclResponse getBucketAcl(GetBucketAclArgs args);

    /**
     * 设置bucket策略
     *
     * @param args {@link SetBucketPolicyArgs}
     */
    void setBucketPolicy(SetBucketPolicyArgs args);

    /**
     * 获取bucket策略
     *
     * @param args {@link GetBucketPolicyArgs}
     * @return policy json string
     */
    String getBucketPolicy(GetBucketPolicyArgs args);

    /**
     * 是否存在bucket
     *
     * @param bucket
     * @return
     */
    boolean bucketExist(String bucket);

    /**
     * 删除bucket
     *
     * @param bucket
     */
    void removeBucket(String bucket);

    /**
     * 创建bucket 存在则忽略
     *
     * @param bucket
     */
    void makeBucket(String bucket);

    /**
     * 获取所有的bucket
     *
     * @param filterBuckets {@link List<String>}
     * @return {@link List<BucketResponse>}
     */
    List<BucketResponse> listBuckets(List<String> filterBuckets);

    /**
     * 查找某个bucket
     *
     * @param bucket
     * @return {@link BucketResponse}
     */
    BucketResponse getBucket(String bucket);

    /**
     * 设置object权限
     *
     * @param args {@link SetObjectAclArgs}
     */
    void setObjectAcl(SetObjectAclArgs args);

    /**
     * 获取object权限
     *
     * @param args {@link GetObjectAclArgs}
     * @return {@link AccessControlList}
     */
    AclResponse getObjectAcl(GetObjectAclArgs args);

    /**
     * <p>
     * 复制`srcBucket`下的`srcObject`到`bucket`下的`object`,`object`不指定时默认取`srcObject`
     * </p>
     *
     * @param args {@link CopyObjectArgs}
     * @return {@link CopyObjectResponse}
     */
    CopyObjectResponse copyObject(CopyObjectArgs args);

    /**
     * 获取文件
     *
     * @param args {@link GetObjectArgs}
     * @return {@link GetObjectResponse} 用完后必须关闭{@link InputStream}
     */
    GetObjectResponse getObject(GetObjectArgs args);

    /**
     * 下载文件到本地
     *
     * @param args {@link DownloadObjectArgs}
     */
    void downloadObject(DownloadObjectArgs args);

    /**
     * 查询文件信息
     *
     * @param args {@link ListObjectsArgs}
     * @return {@link List<ItemResponse>}
     */
    List<ItemResponse> listObjects(ListObjectsArgs args);

    /**
     * 获取文件完整路径
     *
     * @param bucketName
     * @param objectName
     * @return objectUrl {@link String}
     */
    String getObjectUrl(String bucketName, String objectName);

    /**
     * 获取文件的contentType
     *
     * @param file
     * @return
     */
    String getContentType(String file);

    /**
     * 上传文件 内部会关闭流
     *
     * <ul>
     *   <li>If object size is unknown, pass -1 to objectSize and pass valid partSize.
     *   <li>If object size is known, pass -1 to partSize for auto detect; else pass valid partSize
     *       to control memory usage and no. of parts in upload.
     *   <li>If partSize is greater than objectSize, objectSize is used as partSize.
     * </ul>
     *
     * <p>A valid part size is between 5MiB to 5GiB (both limits inclusive).
     * <p>
     *
     * @param args {@link PutObjectArgs}
     * @return {@link CopyObjectResponse}
     */
    PutObjectResponse putObject(PutObjectArgs args);

    /**
     * 上传本地文件
     *
     * @param args {@link UploadObjectArgs}
     * @return
     */
    ObjectWriteResponse uploadObject(UploadObjectArgs args);

    /**
     * 删除文件
     *
     * @param args {@link RemoveObjectArgs}
     * @return {@link RemoveObjectResponse}
     */
    List<RemoveObjectResponse> removeObjects(RemoveObjectArgs args);

    /**
     * Gets presigned URL of an object for HTTP method, expiry time and custom request parameters.
     *
     * @param request {@link GetPresignedObjectUrlArgs}
     * @return presignedObjectUrl {@link String}
     */
    String getPresignedObjectUrl(GetPresignedObjectUrlArgs request);

    /**
     * api类型
     *
     * @return {@link ApiType}
     */
    ApiType apiType();
}
