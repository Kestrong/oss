package com.xjbg.oss.api.impl.s3;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.xjbg.oss.OssConstants;
import com.xjbg.oss.api.ApiConstant;
import com.xjbg.oss.api.impl.AbstractOssApiImpl;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.enums.FileType;
import com.xjbg.oss.exception.OssExceptionEnum;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kesc
 * @date 2020-08-06 11:33
 */
public class S3ApiImpl extends AbstractOssApiImpl {
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
        String result = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug(result);
        }
        if (fullName.endsWith(SLASH)) {
            return result;
        }
        if (StringUtils.isNotBlank(result)) {
            return result.substring(0, result.length() - 1);
        }
        return result;
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
    public ObjectMetadataResponse statObject(GetObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(getTopPathAsBucket(args.getBucket()),
                    getValidObject(args.getBucket(), args.getObject()));

            ObjectMetadata objectMetadata = s3Client.getObjectMetadata(getObjectMetadataRequest);
            ObjectMetadataResponse objectMetadataResponse = new ObjectMetadataResponse();
            objectMetadataResponse.setBucket(args.getBucket());
            objectMetadataResponse.setObject(args.getObject());
            objectMetadataResponse.setEtag(objectMetadata.getETag());
            objectMetadataResponse.setLastModified(objectMetadata.getLastModified());
            objectMetadataResponse.setLength(objectMetadata.getContentLength());
            objectMetadataResponse.setContentType(objectMetadata.getContentType());
            return objectMetadataResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
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
            if (args.isRecursive()) {
                final String srcObjectWithSlash = srcObject.endsWith(ApiConstant.SLASH) ? srcObject : srcObject + ApiConstant.SLASH;
                final String objectWithSlash = object.endsWith(ApiConstant.SLASH) ? object : object + ApiConstant.SLASH;
                ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder()
                        .bucket(args.getSrcBucket())
                        .prefix(srcObject)
                        .recursive(Boolean.TRUE)
                        .build();
                List<ItemResponse> itemResponses = listObjects(listObjectsRequest);
                itemResponses.forEach(x -> objectPairs.add(new ImmutablePair<>(srcObjectWithSlash + x.getObjectName(), objectWithSlash + x.getObjectName())));
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
                Collections.reverse(objectPairs);
                RemoveObjectArgs removeObjectRequest = RemoveObjectArgs.builder()
                        .bucket(srcBucket)
                        .objects(objectPairs.stream().map(ImmutablePair::getLeft).collect(Collectors.toList()))
                        .recursive(args.isRecursive())
                        .build();
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
            if (args.getRange() != null) {
                getObjectRequest.withRange(args.getRange()[0], args.getRange()[1]);
            }
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
            File file = new File(args.getFileName());
            if (!file.exists()) {
                createFile(file, Boolean.FALSE);
            }
            String presignedObjectUrl = getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(args.getBucket())
                    .object(args.getObject())
                    .build());
            PresignedUrlDownloadRequest presignedUrlDownloadRequest = new PresignedUrlDownloadRequest(new URL(presignedObjectUrl));
            PresignedUrlDownloadResult downloadResult = s3Client.download(presignedUrlDownloadRequest);
            try (InputStream inputStream = new BufferedInputStream(downloadResult.getS3Object().getObjectContent());
                 OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(args.getFileName()))) {
                int index;
                byte[] bytes = new byte[OssConstants.DEFUALT_BUFFER_SIZE];
                while ((index = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, index);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    private String formatPath(String path, boolean endSlash) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        String formatPath = Arrays.stream(path.replace(ApiConstant.BACK_SLASH, ApiConstant.SLASH)
                .split(ApiConstant.SLASH)).filter(StringUtils::isNotBlank).collect(Collectors.joining(ApiConstant.SLASH));
        if (endSlash) {
            formatPath = formatPath + ApiConstant.SLASH;
        }
        return formatPath;
    }

    @Override
    public List<ItemResponse> listObjects(ListObjectsArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            String formatPrefix = getValidObject(args.getBucket(), args.getPrefix());
            boolean searchFile = StringUtils.isNotBlank(formatPrefix) && formatPrefix.contains(".");
            formatPrefix = formatPath(formatPrefix, !searchFile);
            ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request()
                    .withBucketName(getTopPathAsBucket(args.getBucket()))
                    .withDelimiter(args.getDelimiter())
                    .withMaxKeys(2)
                    .withEncodingType(args.getUseUrlEncoding() ? "url" : null)
                    .withPrefix(formatPrefix)
                    .withExpectedBucketOwner(args.getExpectedBucketOwner());
            List<ItemResponse> itemResponses = new ArrayList<>();
            ListObjectsV2Result objectListing;
            LOOP:
            do {
                objectListing = s3Client.listObjectsV2(listObjectsRequest);
                if (objectListing == null) {
                    break;
                }
                for (String commonPrefix : objectListing.getCommonPrefixes()) {
                    if (args.getMax() != null && itemResponses.size() >= args.getMax()) {
                        break LOOP;
                    }
                    ItemResponse itemResponse = new ItemResponse(commonPrefix, null, null, 0, null, null, FileType.DIRECTORY.getType());
                    itemResponses.add(itemResponse);
                }
                for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                    if (args.getMax() != null && itemResponses.size() >= args.getMax()) {
                        break LOOP;
                    }
                    ItemResponse itemResponse = new ItemResponse(s3ObjectSummary);
                    String objectName = itemResponse.getObjectName();
                    if (StringUtils.isNotBlank(args.getSuffix()) && !objectName.endsWith(args.getSuffix())) {
                        continue;
                    }
                    boolean hasPrefix = StringUtils.isNotBlank(formatPrefix);
                    if (hasPrefix) {
                        objectName = itemResponse.getObjectName().replaceFirst(formatPrefix, "");
                    }
                    if (hasPrefix && StringUtils.isBlank(objectName)) {
                        objectName = formatPrefix;
                    }
                    objectName = formatPath(objectName, false);
                    if (StringUtils.isNotBlank(objectName)) {
                        itemResponse.setObjectName(objectName);
                        itemResponses.add(itemResponse);
                    }
                }
                listObjectsRequest.setContinuationToken(objectListing.getNextContinuationToken());
            } while (objectListing.isTruncated());
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

    private long getAvailableSize(Object data, long expectedReadSize)
            throws IOException {

        BufferedInputStream stream = (BufferedInputStream) data;
        stream.mark((int) expectedReadSize);

        byte[] buf = new byte[16384]; // 16KiB buffer for optimization
        long totalBytesRead = 0;
        while (totalBytesRead < expectedReadSize) {
            long bytesToRead = expectedReadSize - totalBytesRead;
            if (bytesToRead > buf.length) {
                bytesToRead = buf.length;
            }

            int bytesRead = stream.read(buf, 0, (int) bytesToRead);
            if (bytesRead < 0) {
                break; // reached EOF
            }

            totalBytesRead += bytesRead;
        }

        stream.reset();
        return totalBytesRead;
    }

    @Override
    public PutObjectResponse putObject(PutObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        String uploadId = null;
        String bucket = getTopPathAsBucket(args.getBucket());
        String object = getValidObject(args.getBucket(), args.getObject());

        try (InputStream inputStream = args.getInputStream()) {
            if (autoCreateBucket) {
                makeBucket(bucket);
            }

            List<PartETag> parts = new ArrayList<>();
            long objectSize = args.getContentLength();
            long partSize = Math.min(Math.max(args.getPartSize(), OssConstants.MIN_PART_SIZE), OssConstants.MAX_PART_SIZE);
            int partCount = objectSize <= 0 ? -1 : (int) Math.ceil((double) objectSize / partSize);
            long uploadedSize = 0L;
            for (int partNumber = 1; partNumber <= partCount || partCount < 0; partNumber++) {
                long availableSize = partSize;
                if (partCount > 0) {
                    if (partNumber == partCount) {
                        availableSize = objectSize - uploadedSize;
                    }
                } else {
                    availableSize = getAvailableSize(inputStream, partSize + 1);

                    // If availableSize is less or equal to partSize, then we have reached last
                    // part.
                    if (availableSize <= partSize) {
                        partCount = partNumber;
                    } else {
                        availableSize = partSize;
                    }
                }

                if (partCount == 1) {
                    ObjectMetadata objectMetadata = new ObjectMetadata();
                    objectMetadata.setContentType(args.getContentType());
                    objectMetadata.setContentLength(availableSize);
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, object, inputStream, objectMetadata);
                    PutObjectResult putObjectResult = s3Client.putObject(putObjectRequest);
                    PutObjectResponse putObjectResponse = new PutObjectResponse(bucket, s3Client.getRegionName(), object);
                    putObjectResponse.setEtag(putObjectResult.getETag());
                    putObjectResponse.setExpirationTime(putObjectResult.getExpirationTime());
                    putObjectResponse.setExpirationTimeRuleId(putObjectResult.getExpirationTimeRuleId());
                    putObjectResponse.setRequesterCharged(putObjectResult.isRequesterCharged());
                    putObjectResponse.setVersionId(putObjectResult.getVersionId());
                    return putObjectResponse;
                }

                if (uploadId == null) {
                    // Initiate the multipart upload.
                    InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, object);
                    InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
                    uploadId = initResponse.getUploadId();
                }

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucket)
                        .withKey(object)
                        .withUploadId(uploadId)
                        .withPartNumber(partNumber)
                        .withInputStream(inputStream)
                        .withPartSize(availableSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                parts.add(uploadResult.getPartETag());
                uploadedSize += availableSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, object,
                    uploadId, parts);
            CompleteMultipartUploadResult completeMultipartUploadResult = s3Client.completeMultipartUpload(compRequest);
            log.info("uploadedSize:{}", uploadedSize);

            PutObjectResponse putObjectResponse = new PutObjectResponse(bucket, s3Client.getRegionName(), object);
            putObjectResponse.setEtag(completeMultipartUploadResult.getETag());
            putObjectResponse.setExpirationTime(completeMultipartUploadResult.getExpirationTime());
            putObjectResponse.setExpirationTimeRuleId(completeMultipartUploadResult.getExpirationTimeRuleId());
            putObjectResponse.setRequesterCharged(completeMultipartUploadResult.isRequesterCharged());
            putObjectResponse.setVersionId(completeMultipartUploadResult.getVersionId());
            return putObjectResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (uploadId != null) {
                AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucket, object, uploadId);
                s3Client.abortMultipartUpload(abortMultipartUploadRequest);
            }
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<RemoveObjectResponse> removeObjects(RemoveObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            List<String> objects = filterObjects(args.getObjects());
            List<DeleteObjectsRequest.KeyVersion> deleteObjects = new ArrayList<>();
            for (String object : objects) {
                if (args.isRecursive()) {
                    ListObjectsArgs listObjectsRequest = ListObjectsArgs.builder()
                            .bucket(args.getBucket())
                            .prefix(object)
                            .recursive(Boolean.TRUE).build();
                    List<ItemResponse> itemResponses = listObjects(listObjectsRequest);
                    itemResponses.forEach(x -> deleteObjects.add(new DeleteObjectsRequest.KeyVersion(getValidObject(args.getBucket(), object + ApiConstant.SLASH + x.getObjectName()))));
                } else {
                    deleteObjects.add(new DeleteObjectsRequest.KeyVersion(getValidObject(args.getBucket(), object)));
                }
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
