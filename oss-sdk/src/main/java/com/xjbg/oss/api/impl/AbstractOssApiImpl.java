package com.xjbg.oss.api.impl;

import com.alibaba.fastjson.JSON;
import com.xjbg.oss.OssConstants;
import com.xjbg.oss.api.ApiConstant;
import com.xjbg.oss.api.OssApi;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.AclResponse;
import com.xjbg.oss.api.response.BucketResponse;
import com.xjbg.oss.api.response.PutObjectResponse;
import com.xjbg.oss.exception.OssExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author kesc
 * @date 2020-08-06 17:46
 */
public abstract class AbstractOssApiImpl implements OssApi {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final MimetypesFileTypeMap MIMETYPES_FILE_TYPE_MAP = new MimetypesFileTypeMap();

    protected void checkBucketName(String bucket) {
        if (StringUtils.isBlank(bucket)) {
            throw OssExceptionEnum.BUCKET_NAME_NOT_EXIST.getException();
        }
        log.info("bucket:{}", bucket);
    }

    @Override
    public void setBucketAcl(SetBucketAclArgs args) {
        log.warn("{} unsupported operation setBucketAcl: {}", apiType(), JSON.toJSONString(args));
    }

    @Override
    public AclResponse getBucketAcl(GetBucketAclArgs args) {
        log.warn("{} unsupported operation getBucketAcl: {}", apiType(), JSON.toJSONString(args));
        return null;
    }

    @Override
    public void setBucketPolicy(SetBucketPolicyArgs args) {
        log.warn("{} unsupported operation setBucketPolicy: {}", apiType(), JSON.toJSONString(args));
    }

    @Override
    public String getBucketPolicy(GetBucketPolicyArgs args) {
        log.warn("{} unsupported operation getBucketPolicy: {}", apiType(), JSON.toJSONString(args));
        return null;
    }

    @Override
    public BucketResponse getBucket(String bucket) {
        checkBucketName(bucket);
        List<BucketResponse> bucketResponses = listBuckets(Collections.singletonList(bucket));
        return bucketResponses.isEmpty() ? null : bucketResponses.get(0);
    }

    @Override
    public void setObjectAcl(SetObjectAclArgs args) {
        log.warn("{} unsupported operation setBucketAcl: {}", apiType(), JSON.toJSONString(args));
    }

    @Override
    public AclResponse getObjectAcl(GetObjectAclArgs args) {
        log.warn("{} unsupported operation setBucketAcl: {}", apiType(), JSON.toJSONString(args));
        return null;
    }

    protected List<String> listFile(File file, boolean recursive) {
        if (file == null || !file.exists()) {
            throw OssExceptionEnum.FILE_NOT_EXIST.getException();
        }
        List<String> responses = new ArrayList<>();
        if (!file.isDirectory()) {
            responses.add("");
            return responses;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return responses;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (!recursive) {
                    continue;
                }
                responses.addAll(listFile(f, true).stream().map(x -> Paths.get(f.getName(), x).toString()).collect(Collectors.toList()));
            } else {
                responses.add(Paths.get(f.getName()).toString());
            }
        }
        return responses;
    }

    @Override
    public String getContentType(String file) {
        String contentType = null;
        try {
            contentType = MIMETYPES_FILE_TYPE_MAP.getContentType(file);
        } catch (Exception e) {
            //ignore
        }
        return contentType;
    }

    protected void createFile(File file, boolean isDirectory) throws IOException {
        if (file.exists()) {
            return;
        }
        if (isDirectory) {
            file.mkdirs();
        } else {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            file.createNewFile();
        }
    }

    protected List<String> filterObjects(List<String> objects) {
        return Optional.ofNullable(objects).orElse(Collections.emptyList()).stream()
                .filter(x -> StringUtils.isNotBlank(x) && !x.equals(ApiConstant.SLASH) && !x.equals(ApiConstant.BACK_SLASH))
                .distinct().collect(Collectors.toList());
    }

    @Override
    public void downloadObject(DownloadObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            File file = new File(args.getFileName());
            if (!file.exists()) {
                createFile(file, Boolean.FALSE);
            }
            try (InputStream inputStream = getObject(GetObjectArgs.builder().bucket(args.getBucket()).object(args.getObject()).build()).getInputStream();
                 BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buff = new byte[OssConstants.DEFUALT_BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, read);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<PutObjectResponse> uploadObject(UploadObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        if (StringUtils.isBlank(args.getFileName())) {
            throw OssExceptionEnum.FILE_NOT_EXIST.getException();
        }
        List<PutObjectResponse> responses = new ArrayList<>();
        List<String> subFiles = listFile(new File(args.getFileName()), args.isRecursive());
        String object = args.getObject();
        for (String subFile : subFiles) {
            String filename = Paths.get(args.getFileName(), subFile).toString();
            String contentType = getContentType(filename);
            PutObjectArgs putObjectRequest = new PutObjectArgs();
            putObjectRequest.setContentType(contentType == null ? args.getContentType() : contentType);
            if (StringUtils.isBlank(object)) {
                putObjectRequest.setObject(subFile);
            } else {
                putObjectRequest.setObject(StringUtils.isBlank(subFile) ? object : object + ApiConstant.SLASH + subFile);
            }
            putObjectRequest.setBucket(args.getBucket());
            try {
                putObjectRequest.setInputStream(new FileInputStream(filename));
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
                throw OssExceptionEnum.FILE_NOT_EXIST.getException();
            }
            responses.add(putObject(putObjectRequest));
        }
        return responses;
    }


}
