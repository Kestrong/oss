package com.xjbg.oss.application.controller;

import com.xjbg.oss.OssConstants;
import com.xjbg.oss.api.OssApi;
import com.xjbg.oss.api.OssTemplate;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.application.base.BaseResponse;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.exception.OssExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author kesc
 * @date 2020-08-13 11:40
 */
@Slf4j
@RestController
public class OssController {

    @Autowired
    private OssTemplate ossTemplate;

    private OssApi ossApi(ApiType apiType) {
        if (apiType != null) {
            return ossTemplate.api(apiType);
        }
        return ossTemplate.api();
    }

    @PostMapping(value = "/bucket-acl")
    public BaseResponse<Void> setBucketAcl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                           SetBucketAclArgs args) {
        OssApi ossApi = ossApi(apiType);
        ossApi.setBucketAcl(args);
        return new BaseResponse<>();
    }

    @GetMapping(value = "/bucket-acl")
    public BaseResponse<AclResponse> getBucketAcl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                  GetBucketAclArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getBucketAcl(args));
    }

    @PostMapping(value = "/bucket-policy")
    public BaseResponse<Void> setBucketPolicy(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                              SetBucketPolicyArgs args) {
        OssApi ossApi = ossApi(apiType);
        ossApi.setBucketPolicy(args);
        return new BaseResponse<>();
    }

    @GetMapping(value = "/bucket-policy")
    public BaseResponse<String> getBucketPolicy(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                GetBucketPolicyArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getBucketPolicy(args));
    }

    @GetMapping(value = "/bucket-exist")
    public BaseResponse<Boolean> bucketExist(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                             @RequestParam(name = "bucket") String bucket) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.bucketExist(bucket));
    }

    @DeleteMapping(value = "/remove-bucket")
    public BaseResponse<Void> removeBucket(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                           @RequestParam(name = "bucket") String bucket) {
        OssApi ossApi = ossApi(apiType);
        ossApi.removeBucket(bucket);
        return new BaseResponse<>();
    }

    @PostMapping(value = "/make-bucket")
    public BaseResponse<Void> makeBucket(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                         @RequestParam(name = "bucket") String bucket) {
        OssApi ossApi = ossApi(apiType);
        ossApi.makeBucket(bucket);
        return new BaseResponse<>();
    }

    @GetMapping(value = "/list-buckets")
    public BaseResponse<List<BucketResponse>> listBuckets(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                          @RequestParam(name = "filterBuckets", required = false) List<String> filterBuckets) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.listBuckets(filterBuckets));
    }

    @GetMapping(value = "/bucket")
    public BaseResponse<BucketResponse> getBucket(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                  @RequestParam(name = "bucket") String bucket) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getBucket(bucket));
    }

    @PostMapping(value = "/object-acl")
    public BaseResponse<Void> setObjectAcl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                           SetObjectAclArgs args) {
        OssApi ossApi = ossApi(apiType);
        ossApi.setObjectAcl(args);
        return new BaseResponse<>();
    }

    @GetMapping(value = "/object-acl")
    public BaseResponse<AclResponse> getObjectAcl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                  GetObjectAclArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getObjectAcl(args));
    }

    @PutMapping(value = "/copy-object")
    public BaseResponse<CopyObjectResponse> copyObject(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                       CopyObjectArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.copyObject(args));
    }

    @GetMapping(value = "/list-objects")
    public BaseResponse<List<ItemResponse>> listObjects(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                        ListObjectsArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.listObjects(args));
    }

    @GetMapping(value = "/object-url")
    public BaseResponse<String> getObjectUrl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                             @RequestParam(name = "bucket") String bucket,
                                             @RequestParam(name = "object") String object) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getObjectUrl(bucket, object));
    }

    @DeleteMapping(value = "/remove-objects")
    public BaseResponse<List<RemoveObjectResponse>> removeObjects(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                                  RemoveObjectArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.removeObjects(args));
    }

    @GetMapping(value = "/object")
    public void getObject(@RequestParam(name = "apiType", required = false) ApiType apiType,
                          GetObjectArgs args, HttpServletResponse response) {
        OssApi ossApi = ossApi(apiType);
        try (InputStream inputStream = ossApi.getObject(args).getInputStream()) {
            response.setContentType(OssConstants.DEFAUL_CONTENT_TYPE);
            response.setHeader("Content-Disposition", "attachment; filename=" + Paths.get(args.getObject()).getFileName().toString());
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buff = new byte[1024];
            int read = inputStream.read(buff);
            while (read != -1) {
                outputStream.write(buff, 0, read);
                outputStream.flush();
                read = inputStream.read(buff);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @PostMapping(value = "/put-object")
    public BaseResponse<PutObjectResponse> putObject(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                     @RequestParam(name = "file") MultipartFile file,
                                                     PutObjectArgs args) {
        OssApi ossApi = ossApi(apiType);
        try {
            args.setInputStream(file.getInputStream());
            args.setContentLength(file.getSize());
            args.setContentType(file.getContentType());
            return new BaseResponse<>(ossApi.putObject(args));
        } catch (IOException e) {
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @PostMapping(value = "/upload")
    public BaseResponse<ObjectWriteResponse> upload(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                    @RequestParam(name = "file") MultipartFile file,
                                                    UploadObjectArgs args) {
        OssApi ossApi = ossApi(apiType);
        try {
            args.setInputStream(file.getInputStream());
            args.setContentLength(file.getSize());
            args.setContentType(file.getContentType());
            return new BaseResponse<>(ossApi.uploadObject(args));
        } catch (IOException e) {
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @GetMapping(value = "/presigned-object-url")
    public BaseResponse<String> getPresignedObjectUrl(@RequestParam(name = "apiType", required = false) ApiType apiType,
                                                      GetPresignedObjectUrlArgs args) {
        OssApi ossApi = ossApi(apiType);
        return new BaseResponse<>(ossApi.getPresignedObjectUrl(args));
    }

}
