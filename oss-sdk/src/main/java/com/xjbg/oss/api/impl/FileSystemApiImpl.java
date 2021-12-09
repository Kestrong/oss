package com.xjbg.oss.api.impl;

import com.alibaba.fastjson.JSON;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.enums.FileType;
import com.xjbg.oss.exception.OssExceptionEnum;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author kesc
 * @date 2020-08-06 17:30
 */
public class FileSystemApiImpl extends AbstractOssApiImpl {
    private String baseDir;

    public FileSystemApiImpl(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void setBucketAcl(SetBucketAclArgs args) {
        log.warn("unsupported operation");
    }

    @Override
    public AclResponse getBucketAcl(GetBucketAclArgs args) {
        log.warn("unsupported operation");
        return null;
    }

    @Override
    public void setBucketPolicy(SetBucketPolicyArgs args) {
        log.warn("unsupported operation");
    }

    @Override
    public String getBucketPolicy(GetBucketPolicyArgs args) {
        log.warn("unsupported operation");
        return null;
    }

    @Override
    public boolean bucketExist(String bucket) {
        checkBucketName(bucket);
        File file = Paths.get(baseDir, bucket).toFile();
        return file.exists() && file.isDirectory();
    }

    @Override
    public void removeBucket(String bucket) {
        checkBucketName(bucket);
        File file = Paths.get(baseDir, bucket).toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void makeBucket(String bucket) {
        checkBucketName(bucket);
        File file = Paths.get(baseDir, bucket).toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public List<BucketResponse> listBuckets(List<String> filterBuckets) {
        log.info("filterBuckets:{}", JSON.toJSONString(filterBuckets));
        if (filterBuckets == null || filterBuckets.isEmpty()) {
            return Collections.emptyList();
        }
        List<BucketResponse> bucketResponses = new ArrayList<>();
        for (String bucket : filterBuckets) {
            File file = Paths.get(baseDir, bucket).toFile();
            if (file.exists() && file.isDirectory()) {
                BucketResponse bucketResponse = new BucketResponse();
                bucketResponse.setName(bucket);
                bucketResponse.setCreationDate(new Date(file.lastModified()));
                bucketResponses.add(bucketResponse);
            }
        }
        return bucketResponses;
    }

    @Override
    public void setObjectAcl(SetObjectAclArgs args) {
        log.warn("unsupported operation");
    }

    @Override
    public AclResponse getObjectAcl(GetObjectAclArgs args) {
        log.warn("unsupported operation");
        return null;
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        String fullSrcPath = Paths.get(baseDir, args.getSrcBucket(), args.getSrcObject()).toString();
        File src = new File(fullSrcPath);
        if (!src.exists()) {
            throw OssExceptionEnum.FILE_NOT_EXIST.getException();
        }
        String targetObject = StringUtils.isNotBlank(args.getObject()) ? args.getObject() : args.getSrcObject();
        String fullPath = Paths.get(baseDir, args.getBucket(), targetObject).toString();
        File target = new File(fullPath);
        CopyObjectResponse response = new CopyObjectResponse();
        response.setBucket(args.getBucket());
        response.setObject(targetObject);
        response.setSrcBucket(args.getSrcBucket());
        response.setSrcObject(args.getSrcObject());
        try {
            createFile(target, src.isDirectory());
            List<String> files = listFile(src);
            for (String file : files) {
                File toFile = Paths.get(target.toPath().toString(), file).toFile();
                createFile(toFile, toFile.isDirectory());
                try (FileChannel inputChannel = new FileInputStream(Paths.get(src.toPath().toString(), file).toFile()).getChannel();
                     FileChannel outputChannel = new FileOutputStream(toFile).getChannel()) {
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                }
            }
            if (args.getDelete()) {
                RemoveObjectArgs removeObjectRequest = RemoveObjectArgs.builder()
                        .bucket(args.getSrcBucket())
                        .objects(Collections.singletonList(args.getSrcObject()))
                        .build();
                removeObjects(removeObjectRequest);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.COPY_OBJECT_ERROR.getException();
        }
        return response;
    }

    @Override
    public GetObjectResponse getObject(GetObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            String fullPath = Paths.get(baseDir, args.getBucket(), args.getObject()).toString();
            GetObjectResponse getObjectResponse = new GetObjectResponse();
            getObjectResponse.setBucket(args.getBucket());
            getObjectResponse.setObject(args.getObject());
            getObjectResponse.setInputStream(new FileInputStream(fullPath));
            return getObjectResponse;
        } catch (FileNotFoundException e) {
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
            URL website = new URL(args.getBucket() + args.getObject());
            try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                 FileOutputStream fos = new FileOutputStream(args.getFileName())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<ItemResponse> listObjects(ListObjectsArgs args) {
        log.info("{}", JSON.toJSONString(args));
        File file = StringUtils.isNotBlank(args.getPrefix()) ? Paths.get(baseDir, args.getBucket(), args.getPrefix()).toFile() : Paths.get(baseDir, args.getBucket()).toFile();
        if (!file.exists()) {
            throw OssExceptionEnum.FILE_NOT_EXIST.getException();
        }
        if (!file.isDirectory()) {
            log.warn("this bucket[{}] is not a directory", args.getBucket());
            return Collections.emptyList();
        }
        List<ItemResponse> responses = new ArrayList<>();
        listObjects(responses, file, args, "");
        return responses;
    }

    private void listObjects(List<ItemResponse> responses, File file, ListObjectsArgs args, String parentName) {
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (File f : files) {
            if (args.getMax() != null && responses.size() >= args.getMax()) {
                break;
            }
            String object = Paths.get(parentName, f.getName()).toString();
            if (f.isDirectory() && args.getRecursive()) {
                listObjects(responses, f, args, object);
            } else {
                if (StringUtils.isNotBlank(args.getSuffix()) && !object.endsWith(args.getSuffix())) {
                    continue;
                }
                responses.add(new ItemResponse(args.getBucket(), object, new Date(f.lastModified()), null, f.length(), null, null, f.isDirectory() ? FileType.DIRECTORY.getType() : FileType.FILE.getType()));
            }
        }
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        log.info("bucket:{},object:{}", bucketName, objectName);
        return Paths.get(baseDir, bucketName, objectName).toString();
    }

    @Override
    public PutObjectResponse putObject(PutObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        String fullPath = Paths.get(baseDir, args.getBucket(), args.getObject()).toString();
        try (InputStream inputStream = args.getInputStream()) {
            File file = new File(fullPath);
            boolean isDirectory = args.getObject() != null && (args.getObject().endsWith("\\") || args.getObject().endsWith("/"));
            createFile(file, isDirectory);
            if (file.isFile() && inputStream != null) {
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    int index;
                    byte[] bytes = new byte[2048];
                    while ((index = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, index);
                        outputStream.flush();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
        PutObjectResponse putObjectResponse = new PutObjectResponse();
        putObjectResponse.setBucket(args.getBucket());
        putObjectResponse.setObject(args.getObject());
        return putObjectResponse;
    }

    @Override
    public ObjectWriteResponse uploadObject(UploadObjectArgs args) {
        return putObject(PutObjectArgs.builder()
                .bucket(args.getBucket())
                .object(args.getObject())
                .contentLength(args.getContentLength())
                .contentType(args.getContentType())
                .inputStream(args.getInputStream())
                .build());
    }

    @Override
    public List<RemoveObjectResponse> removeObjects(RemoveObjectArgs args) {
        log.info("{}", JSON.toJSONString(args));
        List<String> objects = args.getObjects();
        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        }
        List<RemoveObjectResponse> responses = new ArrayList<>();
        for (String object : objects) {
            String fullPath = Paths.get(baseDir, args.getBucket(), object).toString();
            File file = new File(fullPath);
            try {
                if (file.exists()) {
                    deleteDir(responses, file);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return responses;
    }

    private void deleteDir(List<RemoveObjectResponse> responses, File file) {
        if (file.isFile()) {
            boolean delete = file.delete();
            if (delete) {
                responses.add(new RemoveObjectResponse(null, file.getAbsolutePath(), null));
            }
        } else {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    deleteDir(responses, f);
                }
            }
            boolean delete = file.delete();
            if (delete) {
                responses.add(new RemoveObjectResponse(null, file.getAbsolutePath(), null));
            }
        }
    }

    @Override
    public String getPresignedObjectUrl(GetPresignedObjectUrlArgs request) {
        return getObjectUrl(request.getBucket(), request.getObject());
    }

    @Override
    public ApiType apiType() {
        return ApiType.FILESYSTEM;
    }
}
