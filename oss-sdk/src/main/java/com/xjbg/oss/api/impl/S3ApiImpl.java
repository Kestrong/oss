package com.xjbg.oss.api.impl;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.xjbg.oss.OssConstants;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.exception.OssExceptionEnum;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kesc
 * @date 2020-08-06 11:33
 */
public class S3ApiImpl extends AbstractSsoApiImpl {
    private static final String SLASH = "/";
    private static final String BACK_SLASH = "\\";
    protected AmazonS3 s3Client;
    protected String defaultBucket;
    protected boolean autoCreateBucket;

    public S3ApiImpl(@NonNull AmazonS3 s3Client, String defaultBucket) {
        this(s3Client, defaultBucket, Boolean.FALSE);
    }

    public S3ApiImpl(@NonNull AmazonS3 s3Client, String defaultBucket, boolean autoCreateBucket) {
        this.s3Client = s3Client;
        this.defaultBucket = defaultBucket;
        this.autoCreateBucket = autoCreateBucket;
        if (StringUtils.isNotBlank(defaultBucket)) {
            makeBucket(defaultBucket);
        }
    }

    private String getTopPathAsBucket(String bucket) {
        if (StringUtils.isBlank(bucket)) {
            bucket = defaultBucket == null ? "" : defaultBucket;
        }
        String[] paths = bucket.replace(BACK_SLASH, SLASH).split(SLASH);
        if (paths.length == 0) {
            throw OssExceptionEnum.BUCKET_NAME_NOT_EXIST.getException();
        }
        if (paths.length == 1) {
            return paths[0];
        }
        for (String path : paths) {
            if (StringUtils.isNotBlank(path)) {
                return path;
            }
        }
        log.debug(bucket);
        return bucket;
    }

    private String getValidObject(String bucket, String object) {
        if (StringUtils.isBlank(bucket)) {
            bucket = defaultBucket == null ? "" : defaultBucket;
        }
        String fullName = (bucket + (StringUtils.isNotBlank(object) ? SLASH + object : ""))
                .replace(BACK_SLASH, SLASH);
        String[] paths = fullName.split(SLASH);
        if (paths.length == 0) {
            throw OssExceptionEnum.OBJECT_NAME_NOT_EXIST.getException();
        }
        StringBuilder sb = new StringBuilder();
        boolean begin = false;
        for (String path : paths) {
            if (StringUtils.isNotBlank(path)) {
                if (begin) {
                    sb.append(path).append(SLASH);
                }
                begin = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(sb.toString());
        }
        if (fullName.endsWith(SLASH)) {
            return sb.toString();
        }
        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public void setBucketAcl(SetBucketAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        String bucket = getTopPathAsBucket(args.getBucket());
        SetBucketAclRequest setBucketAclRequest = new SetBucketAclRequest(bucket, args.getAcl());
        setBucketAclRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        try {
            s3Client.setBucketAcl(setBucketAclRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_ACL_ERROR.getException();
        }
    }

    private AclResponse convertAcl(AccessControlList acl) {
        AclResponse aclResponse = new AclResponse();
        aclResponse.setGranteeList(acl.getGrantsAsList());
        aclResponse.setOwner(acl.getOwner());
        return aclResponse;
    }

    @Override
    public AclResponse getBucketAcl(GetBucketAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        GetBucketAclRequest getBucketAclRequest = new GetBucketAclRequest(getTopPathAsBucket(args.getBucket()));
        getBucketAclRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        try {
            AccessControlList acl = s3Client.getBucketAcl(getBucketAclRequest);
            return convertAcl(acl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_ACL_ERROR.getException();
        }
    }

    @Override
    public void setBucketPolicy(SetBucketPolicyArgs args) {
        log.info("{}", JSON.toJSONString(args));
        SetBucketPolicyRequest setBucketPolicyRequest = new SetBucketPolicyRequest();
        setBucketPolicyRequest.setBucketName(getTopPathAsBucket(args.getBucket()));
        setBucketPolicyRequest.setConfirmRemoveSelfBucketAccess(args.getConfirmRemoveSelfBucketAccess());
        setBucketPolicyRequest.setPolicyText(args.getPolicyText());
        setBucketPolicyRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        try {
            s3Client.setBucketPolicy(setBucketPolicyRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_POLICY_ERROR.getException();
        }
    }

    @Override
    public String getBucketPolicy(GetBucketPolicyArgs args) {
        log.info("{}", JSON.toJSONString(args));
        GetBucketPolicyRequest getBucketPolicyRequest = new GetBucketPolicyRequest(getTopPathAsBucket(args.getBucket()));
        getBucketPolicyRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        try {
            return s3Client.getBucketPolicy(getBucketPolicyRequest).getPolicyText();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_POLICY_ERROR.getException();
        }
    }

    @Override
    public boolean bucketExist(String bucket) {
        checkBucketName(bucket);
        try {
            return s3Client.doesBucketExistV2(getTopPathAsBucket(bucket));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
        }
    }

    @Override
    public void removeBucket(String bucket) {
        checkBucketName(bucket);
        try {
            s3Client.deleteBucket(getTopPathAsBucket(bucket));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
        }
    }

    @Override
    public void makeBucket(String bucket) {
        if (bucketExist(bucket)) {
            return;
        }
        try {
            s3Client.createBucket(getTopPathAsBucket(bucket));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
        }
    }

    @Override
    public List<BucketResponse> listBuckets(List<String> filterBuckets) {
        log.info("filterBuckets:{}", JSON.toJSONString(filterBuckets));
        try {
            boolean isEmpty = filterBuckets == null || filterBuckets.isEmpty();
            List<Bucket> buckets = s3Client.listBuckets();
            if (buckets == null || buckets.isEmpty()) {
                return Collections.emptyList();
            }
            List<BucketResponse> bucketResponses = new ArrayList<>();
            buckets.forEach(x -> {
                if (isEmpty || filterBuckets.contains(x.getName())) {
                    BucketResponse bucketResponse = new BucketResponse();
                    bucketResponse.setName(x.getName());
                    bucketResponse.setCreationDate(Date.from(x.getCreationDate().toInstant()));
                    Owner owner = x.getOwner();
                    if (owner != null) {
                        bucketResponse.setOwner(new Owner(owner.getId(), owner.getDisplayName()));
                    }
                    bucketResponses.add(bucketResponse);
                }
            });
            return bucketResponses;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
        }
    }

    @Override
    public void setObjectAcl(SetObjectAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        String bucket = getTopPathAsBucket(args.getBucket());
        String object = getValidObject(args.getBucket(), args.getObject());
        SetObjectAclRequest setObjectAclRequest = new SetObjectAclRequest(bucket, object, args.getAcl());
        setObjectAclRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        try {
            s3Client.setObjectAcl(setObjectAclRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.OBJECT_ACL_ERROR.getException();
        }
    }

    @Override
    public AclResponse getObjectAcl(GetObjectAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        GetObjectAclRequest getObjectAclRequest = new GetObjectAclRequest(getTopPathAsBucket(args.getBucket()), getValidObject(args.getBucket(), args.getObject()));
        getObjectAclRequest.setExpectedBucketOwner(args.getExpectedBucketOwner());
        getObjectAclRequest.setVersionId(args.getVersionId());
        try {
            AccessControlList acl = s3Client.getObjectAcl(getObjectAclRequest);
            return convertAcl(acl);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.OBJECT_ACL_ERROR.getException();
        }
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            String srcObject = getValidObject(args.getSrcBucket(), args.getSrcObject());
            String object = getValidObject(args.getBucket(), StringUtils.isNotBlank(args.getObject()) ? args.getObject() : args.getSrcObject());
            List<ImmutablePair<String, String>> objectPairs = new ArrayList<>();
            //deal with directory
            if (srcObject.endsWith(SLASH)) {
                ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder()
                        .bucket(args.getSrcBucket())
                        .prefix(srcObject)
                        .recursive(Boolean.TRUE)
                        .build();
                List<ItemResponse> itemResponses = listObjects(listObjectsRequest);
                itemResponses.forEach(x -> objectPairs.add(new ImmutablePair<>(x.getObjectName(), x.getObjectName().replace(srcObject, object))));
            } else {
                objectPairs.add(new ImmutablePair<>(srcObject, object));
            }
            String bucket = getTopPathAsBucket(args.getBucket());
            String srcBucket = getTopPathAsBucket(args.getSrcBucket());
            if (autoCreateBucket) {
                makeBucket(bucket);
                if (!srcBucket.equals(bucket)) {
                    makeBucket(srcBucket);
                }
            }
            for (Pair<String, String> objectPair : objectPairs) {
                CopyObjectRequest copyObjectRequest = new CopyObjectRequest();
                copyObjectRequest.setSourceBucketName(srcBucket);
                copyObjectRequest.setSourceKey(objectPair.getLeft());
                copyObjectRequest.setDestinationBucketName(bucket);
                copyObjectRequest.setDestinationKey(objectPair.getRight());
                s3Client.copyObject(copyObjectRequest);
            }
            if (args.getDelete()) {
                RemoveObjectArgs removeObjectRequest = new RemoveObjectArgs();
                removeObjectRequest.setBucket(srcBucket);
                Collections.reverse(objectPairs);
                removeObjectRequest.setObjects(objectPairs.stream().map(ImmutablePair::getLeft).collect(Collectors.toList()));
                removeObjects(removeObjectRequest);
            }
            return new CopyObjectResponse(args.getSrcBucket(), args.getBucket(), s3Client.getRegionName(), args.getSrcObject(), args.getObject());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.COPY_OBJECT_ERROR.getException();
        }
    }

    @Override
    public GetObjectResponse getObject(GetObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(getTopPathAsBucket(args.getBucket()),
                    getValidObject(args.getBucket(), args.getObject()));
            S3Object s3Object = s3Client.getObject(getObjectRequest);
            GetObjectResponse getObjectResponse = new GetObjectResponse();
            getObjectResponse.setBucket(args.getBucket());
            getObjectResponse.setObject(args.getObject());
            getObjectResponse.setInputStream(s3Object.getObjectContent());
            getObjectResponse.setRedirectLocation(s3Object.getRedirectLocation());
            getObjectResponse.setMetadata(s3Object.getObjectMetadata().getRawMetadata());
            getObjectResponse.setUserMetadata(s3Object.getObjectMetadata().getUserMetadata());
            return getObjectResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public void downloadObject(DownloadObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            String presignedObjectUrl = getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(args.getBucket())
                    .object(args.getObject())
                    .build());
            PresignedUrlDownloadRequest presignedUrlDownloadRequest = new PresignedUrlDownloadRequest(new URL(presignedObjectUrl));
            PresignedUrlDownloadResult downloadResult = s3Client.download(presignedUrlDownloadRequest);
            try (InputStream inputStream = new BufferedInputStream(downloadResult.getS3Object().getObjectContent());
                 OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(args.getFileName()))) {
                int index;
                byte[] bytes = new byte[1024];
                while ((index = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, index);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<ItemResponse> listObjects(ListObjectsArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(getTopPathAsBucket(args.getBucket()))
                    .withDelimiter(args.getDelimiter())
                    .withEncodingType(args.getUseUrlEncoding() ? "url" : null)
                    .withPrefix(args.getPrefix())
                    .withExpectedBucketOwner(args.getExpectedBucketOwner())
                    .withMaxKeys(args.getMax())
                    .withMarker(StringUtils.isNotBlank(args.getMaker()) ? args.getMaker() : args.getSuffix());
            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            if (objectListing == null || objectListing.getObjectSummaries() == null) {
                return Collections.emptyList();
            }
            List<ItemResponse> itemResponses = new ArrayList<>();
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            for (S3ObjectSummary s3ObjectSummary : objectSummaries) {
                ItemResponse itemResponse = new ItemResponse(s3ObjectSummary);
                itemResponses.add(itemResponse);
            }
            return itemResponses;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        log.info("bucket:{},object:{}", bucketName, objectName);
        try {
            return s3Client.getUrl(getTopPathAsBucket(bucketName), getValidObject(bucketName, objectName)).toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_URL_ERROR.getException();
        }
    }

    @Override
    public PutObjectResponse putObject(PutObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try (InputStream inputStream = args.getInputStream()) {
            String bucket = getTopPathAsBucket(args.getBucket());
            if (autoCreateBucket) {
                makeBucket(bucket);
            }
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(args.getContentType());
            objectMetadata.setContentLength(args.getContentLength());
            String object = getValidObject(args.getBucket(), args.getObject());
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, object, inputStream, objectMetadata);
            PutObjectResult putObjectResult = s3Client.putObject(putObjectRequest);
            PutObjectResponse putObjectResponse = new PutObjectResponse(bucket, s3Client.getRegionName(), object);
            putObjectResponse.setEtag(putObjectResult.getETag());
            putObjectResponse.setExpirationTime(putObjectResult.getExpirationTime());
            putObjectResponse.setExpirationTimeRuleId(putObjectResult.getExpirationTimeRuleId());
            putObjectResponse.setRequesterCharged(putObjectResult.isRequesterCharged());
            putObjectResponse.setVersionId(putObjectResult.getVersionId());
            return putObjectResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @Override
    public ObjectWriteResponse uploadObject(UploadObjectArgs args) {
        //todo 优化content length和part size设置与计算
        log.info("{}", JSON.toJSONString(args));
        if (args.getInputStream() == null) {
            throw OssExceptionEnum.FILE_NOT_EXIST.getException();
        }
        try {
            String bucket = getTopPathAsBucket(args.getBucket());
            if (autoCreateBucket) {
                makeBucket(bucket);
            }
            List<PartETag> partETags = new ArrayList<>();

            // Initiate the multipart upload.
            String object = getValidObject(args.getBucket(), args.getObject());
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, object);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long contentLength = args.getContentLength();
            long partSize = args.getPartSize();
            if (partSize <= 0) {
                partSize = OssConstants.MIN_PART_SIZE;
            }
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucket)
                        .withKey(object)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withInputStream(args.getInputStream())
                        .withPartSize(partSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, object,
                    initResponse.getUploadId(), partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = s3Client.completeMultipartUpload(compRequest);

            ObjectWriteResponse objectWriteResponse = new ObjectWriteResponse();
            objectWriteResponse.setEtag(completeMultipartUploadResult.getETag());
            objectWriteResponse.setExpirationTime(completeMultipartUploadResult.getExpirationTime());
            objectWriteResponse.setExpirationTimeRuleId(completeMultipartUploadResult.getExpirationTimeRuleId());
            objectWriteResponse.setVersionId(completeMultipartUploadResult.getVersionId());
            objectWriteResponse.setRequesterCharged(completeMultipartUploadResult.isRequesterCharged());
            return objectWriteResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<RemoveObjectResponse> removeObjects(RemoveObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            List<String> objects = args.getObjects();
            if (objects == null) {
                objects = Collections.emptyList();
            }
            List<DeleteObjectsRequest.KeyVersion> deleteObjects = new ArrayList<>();
            for (String object : objects) {
                if (object.endsWith(SLASH)) {
                    ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder()
                            .bucket(args.getBucket())
                            .prefix(object)
                            .recursive(Boolean.TRUE).build();
                    List<ItemResponse> itemResponses = listObjects(listObjectsRequest);
                    itemResponses.forEach(x -> deleteObjects.add(new DeleteObjectsRequest.KeyVersion(getValidObject(args.getBucket(), x.getObjectName()))));
                }
                deleteObjects.add(new DeleteObjectsRequest.KeyVersion(getValidObject(args.getBucket(), object)));
            }
            String bucket = getTopPathAsBucket(args.getBucket());
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
            deleteObjectsRequest.setKeys(deleteObjects);
            DeleteObjectsResult deleteObjectsResult = s3Client.deleteObjects(deleteObjectsRequest);
            if (deleteObjectsResult == null || deleteObjectsResult.getDeletedObjects() == null) {
                return Collections.emptyList();
            }
            List<DeleteObjectsResult.DeletedObject> deletedObjects = deleteObjectsResult.getDeletedObjects();
            List<RemoveObjectResponse> responses = new ArrayList<>();
            for (DeleteObjectsResult.DeletedObject deletedObject : deletedObjects) {
                responses.add(new RemoveObjectResponse(bucket, deletedObject.getKey(), deletedObject.getVersionId()));
            }
            return responses;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.DELETE_OBJECT_ERROR.getException();
        }
    }

    @Override
    public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                    getTopPathAsBucket(args.getBucket()),
                    getValidObject(args.getBucket(), args.getObject())
            );
            generatePresignedUrlRequest.setMethod(args.getMethod());
            generatePresignedUrlRequest.setExpiration(args.getExpire());
            return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_URL_ERROR.getException();
        }
    }

    @Override
    public ApiType apiType() {
        return ApiType.S3;
    }
}
