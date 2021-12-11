package com.xjbg.oss.api.impl;

import com.xjbg.oss.api.ApiConstant;
import com.xjbg.oss.api.OssApi;
import com.xjbg.oss.api.response.BucketResponse;
import com.xjbg.oss.exception.OssExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
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
    public BucketResponse getBucket(String bucket) {
        checkBucketName(bucket);
        List<BucketResponse> bucketResponses = listBuckets(Collections.singletonList(bucket));
        return bucketResponses.isEmpty() ? null : bucketResponses.get(0);
    }

    protected List<String> listFile(File file) {
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
                responses.addAll(listFile(f).stream().map(x -> Paths.get(f.getName(), x).toString()).collect(Collectors.toList()));
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

}
