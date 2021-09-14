package com.xjbg.oss.api.impl.webhdfs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.s3.model.*;
import com.google.common.base.Joiner;
import com.xjbg.oss.api.ApiConstant;
import com.xjbg.oss.api.impl.AbstractOssApiImpl;
import com.xjbg.oss.api.request.*;
import com.xjbg.oss.api.response.*;
import com.xjbg.oss.enums.ApiType;
import com.xjbg.oss.enums.FileType;
import com.xjbg.oss.exception.OssException;
import com.xjbg.oss.exception.OssExceptionEnum;
import okhttp3.*;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

/**
 * @author kesc
 * @date 2021-09-02 17:04
 */
public class WebHdfsApiImpl extends AbstractOssApiImpl {
    private String userPrincipal;
    private String keyTab;
    private String url;
    private String defaultBucket;
    private String krb5;
    private OkHttpClient okHttpClient;
    private Authenticator authenticator;
    private final Authenticator.Token token = new Authenticator.Token();

    public WebHdfsApiImpl(OkHttpClient okHttpClient, Authenticator authenticator, String userPrincipal, String keyTab, String krb5, String url, String defaultBucket) {
        this.userPrincipal = userPrincipal;
        this.keyTab = keyTab;
        this.krb5 = krb5;
        this.defaultBucket = defaultBucket;
        this.url = url;
        this.okHttpClient = okHttpClient;
        this.authenticator = authenticator;
    }

    protected void ensureValidToken() throws IOException {
        if (!token.isSet()) {
            authenticator.authenticate(url, token);
        } else {
            long currentTime = System.currentTimeMillis();
            long tokenExpired = Long.parseLong(token.toString().split("&")[3].split("=")[1]);
            boolean expired = currentTime > tokenExpired;
            log.info("[currentTime vs. tokenExpired] [{} vs. {}] [expired:{}]", currentTime, tokenExpired, expired);
            if (expired) {
                authenticator.authenticate(url, token);
            }
        }
    }

    private Map<String, String> tokenHeaders() {
        Map<String, String> header = new HashMap<>(3);
        String t = token.getToken();
        if (t != null) {
            if (!t.startsWith("\"")) {
                t = "\"" + t + "\"";
            }
            header.put("Cookie", PseudoAuthenticator.AUTH_COOKIE_EQ + t);
        }
        return header;
    }

    private WebhdfsUrlBuilder defaultUrlBuilder() {
        WebhdfsUrlBuilder urlBuilder = WebhdfsUrlBuilder.newBuilder().endpoint(url).defaultBucket(defaultBucket);
        if (StringUtils.isAnyBlank(userPrincipal, keyTab, krb5)) {
            urlBuilder.params(Collections.singletonMap(PseudoAuthenticator.USER_NAME, keyTab));
        }
        return urlBuilder;
    }

    private Response execute(String url, String method, RequestBody requestBody) throws IOException {
        ensureValidToken();
        OkHttpClient okHttpClient = this.okHttpClient;
        if (method.equals(ApiConstant.POST) || method.equals(ApiConstant.PUT)) {
            okHttpClient = okHttpClient.newBuilder().retryOnConnectionFailure(false).build();
        }
        Headers.Builder headerBuilder = new Headers.Builder();
        for (Map.Entry<String, String> entry : tokenHeaders().entrySet()) {
            headerBuilder.add(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .headers(headerBuilder.build())
                .url(url)
                .method(method, requestBody)
                .build();
        log.info("method:{},headers:{},url:{}", method, request.headers().toMultimap(), url);
        return okHttpClient.newCall(request).execute();
    }

    private JSONObject validResult(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (!response.isSuccessful() || responseBody == null) {
            if (responseBody != null) {
                log.error(responseBody.string());
            }
            throw new OssException(String.valueOf(response.code()), response.message());
        }
        String string = responseBody.string();
        log.info(string);
        JSONObject jsonObject = JSON.parseObject(string);
        if (jsonObject == null) {
            throw OssExceptionEnum.SYS_TOO_BUSY.getException();
        }
        return jsonObject;
    }

    private boolean exist(String bucket, String object) throws Exception {
        String url = defaultUrlBuilder().bucket(bucket).object(object)
                .params(Collections.singletonMap(ApiConstant.OP, ApiConstant.GETFILESTATUS)).build();
        try (Response response = execute(url, ApiConstant.GET, null)) {
            if (response.code() == ApiConstant.NOT_FOUND) {
                return false;
            }
            JSONObject result = validResult(response);
            return result.containsKey("FileStatus");
        }
    }

    private void setAcl(String bucket, String object, AccessControlList acl) throws IOException {
        List<String> aclEntries = new ArrayList<>();
        acl.getGrantsAsList().forEach(x -> {
            String who;
            if (x.getGrantee() instanceof GroupGrantee) {
                who = "group:" + x.getGrantee().getIdentifier();
            } else if (x.getGrantee() instanceof CanonicalGrantee) {
                who = "user:" + x.getGrantee().getIdentifier();
            } else {
                who = "other:" + x.getGrantee().getIdentifier();
            }
            String permission;
            if (Permission.FullControl.toString().equals(x.getPermission().toString())) {
                permission = "rwx";
            } else if (Permission.Read.toString().equals(x.getPermission().toString())) {
                permission = "r--";
            } else if (Permission.Write.toString().equals(x.getPermission().toString())) {
                permission = "rw-";
            } else {
                permission = "--x";
            }
            aclEntries.add(who + ":" + permission);
        });
        Map<String, String> params = new HashMap<>();
        params.put(ApiConstant.OP, ApiConstant.SETACL);
        params.put("aclspec", Joiner.on(",").join(aclEntries));
        String url = defaultUrlBuilder().bucket(bucket).object(object).params(params).build();
        try (Response response = execute(url, ApiConstant.PUT, Util.EMPTY_REQUEST)) {
            validResult(response);
        }
    }

    private AclResponse getAcl(String bucket, String object) throws IOException {
        String url = defaultUrlBuilder().bucket(bucket).object(object).params(Collections.singletonMap(ApiConstant.OP, ApiConstant.GETACLSTATUS)).build();
        try (Response response = execute(url, ApiConstant.GET, null)) {
            JSONObject jsonObject = validResult(response);
            JSONObject aclStatus = jsonObject.getJSONObject("AclStatus");
            AclResponse aclResponse = new AclResponse();
            if (aclStatus == null) {
                return aclResponse;
            }
            aclResponse.setOwner(new Owner(aclStatus.getString(ApiConstant.OWNER), null));
            JSONArray entries = aclStatus.getJSONArray("entries");
            if (entries != null && !entries.isEmpty()) {
                List<Grant> grants = new ArrayList<>();
                for (int i = 0, n = entries.size(); i < n; i++) {
                    String entry = entries.getString(i);
                    String[] parts = entry.split(":");
                    Grantee grantee;
                    if ("group".equals(parts[0])) {
                        grantee = GroupGrantee.AllUsers;
                    } else if ("user".equals(parts[0])) {
                        grantee = new CanonicalGrantee(parts[1]);
                    } else {
                        grantee = new EmailAddressGrantee(parts[1]);
                    }
                    Permission permission;
                    if ("rwx".equals(parts[2])) {
                        permission = Permission.FullControl;
                    } else if ("rw-".equals(parts[2])) {
                        permission = Permission.Write;
                    } else if ("r--".equals(parts[2])) {
                        permission = Permission.Read;
                    } else {
                        permission = Permission.ReadAcp;
                    }
                    grants.add(new Grant(grantee, permission));
                }
                aclResponse.setGranteeList(grants);
            }
            return aclResponse;
        }
    }

    @Override
    public void setBucketAcl(SetBucketAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            setAcl(args.getBucket(), null, args.getAcl());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_ACL_ERROR.getException();
        }
    }

    @Override
    public AclResponse getBucketAcl(GetBucketAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            return getAcl(args.getBucket(), null);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_ACL_ERROR.getException();
        }
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
        try {
            return exist(bucket, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
        }
    }

    private boolean remove(String bucket, String object) throws Exception {
        Map<String, String> param = new HashMap<>();
        param.put("recursive", "true");
        param.put(ApiConstant.OP, ApiConstant.DELETE);
        String url = defaultUrlBuilder().bucket(bucket).object(object).params(param).build();
        try (Response response = execute(url, ApiConstant.DELETE, null)) {
            return validResult(response).getBooleanValue("boolean");
        }
    }

    @Override
    public void removeBucket(String bucket) {
        checkBucketName(bucket);
        try {
            remove(bucket, null);
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
        Map<String, String> param = new HashMap<>();
        param.put("permission", "777");
        param.put(ApiConstant.OP, ApiConstant.MKDIRS);
        String url = defaultUrlBuilder().bucket(bucket).params(param).build();
        try (Response response = execute(url, ApiConstant.PUT, Util.EMPTY_REQUEST)) {
            validResult(response);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
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
            String url = defaultUrlBuilder().bucket(bucket)
                    .params(Collections.singletonMap(ApiConstant.OP, ApiConstant.GETFILESTATUS)).build();
            try (Response response = execute(url, ApiConstant.GET, null)) {
                if (ApiConstant.NOT_FOUND == response.code()) {
                    continue;
                }
                JSONObject jsonObject = validResult(response);
                JSONObject fileStatus = jsonObject.getJSONObject(ApiConstant.FILESTATUS);
                if (fileStatus != null && FileType.DIRECTORY.getType().equalsIgnoreCase(fileStatus.getString(ApiConstant.TYPE))) {
                    BucketResponse bucketResponse = new BucketResponse();
                    bucketResponse.setName(bucket);
                    bucketResponse.setCreationDate(fileStatus.getDate(ApiConstant.MODIFICATION_TIME));
                    bucketResponses.add(bucketResponse);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw OssExceptionEnum.BUCKET_OPERATION_ERROR.getException();
            }
        }
        return bucketResponses;
    }

    @Override
    public void setObjectAcl(SetObjectAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            setAcl(args.getBucket(), args.getObject(), args.getAcl());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.OBJECT_ACL_ERROR.getException();
        }
    }

    @Override
    public AclResponse getObjectAcl(GetObjectAclArgs args) {
        log.info("{}", JSON.toJSONString(args));
        try {
            return getAcl(args.getBucket(), args.getObject());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.OBJECT_ACL_ERROR.getException();
        }
    }

    @Override
    public PutObjectResponse putObject(PutObjectArgs args) {
        log.info("putObject:{}", JSON.toJSONString(args));
        Map<String, String> params = new HashMap<>();
        params.put("overwrite", "true");
        params.put("permission", "777");
        params.put("createparent", "true");
        params.put(ApiConstant.OP, ApiConstant.CREATE);
        String url = defaultUrlBuilder().bucket(args.getBucket()).object(args.getObject())
                .params(params).build();
        try (InputStream inputStream = args.getInputStream();
             Response locationResponse = execute(url, ApiConstant.PUT, Util.EMPTY_REQUEST)) {
            if (locationResponse.isRedirect()) {
                url = locationResponse.header(ApiConstant.LOCATION);
            } else {
                JSONObject result = validResult(locationResponse);
                url = result.getString(ApiConstant.LOCATION);
            }
            RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.get(args.getContentType());
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    sink.writeAll(Okio.source(inputStream));
                }
            };
            try (Response putResponse = execute(url, ApiConstant.PUT, requestBody)) {
                if (!putResponse.isSuccessful()) {
                    throw new OssException(String.valueOf(putResponse.code()), putResponse.message());
                }
                return new PutObjectResponse(args.getBucket(), null, args.getObject());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.PUT_OBJECT_ERROR.getException();
        }
    }

    @Override
    public ObjectWriteResponse uploadObject(UploadObjectArgs args) {
        log.info("uploadObject:{}", JSON.toJSONString(args));
        return putObject(PutObjectArgs.builder()
                .bucket(args.getBucket())
                .object(args.getObject())
                .contentLength(args.getContentLength())
                .contentType(args.getContentType())
                .inputStream(args.getInputStream())
                .build());
    }

    private void copy(String srcBucket, String srcObject, String dstBucket, String dstObject) {
        List<ItemResponse> itemResponses = listObjects(ListObjectsArgs.builder().bucket(srcBucket + ApiConstant.SLASH + srcObject).build());
        if (itemResponses.isEmpty()) {
            itemResponses.add(new ItemResponse(srcBucket, srcObject, null, null, 0, null, null, FileType.FILE.getType()));
        }
        for (ItemResponse itemResponse : itemResponses) {
            if (FileType.DIRECTORY.getType().equals(itemResponse.getType())) {
                copy(srcBucket, srcObject + ApiConstant.SLASH + itemResponse.getObjectName(), dstBucket, dstObject + ApiConstant.SLASH + itemResponse.getObjectName());
            } else {
                try {
                    InputStream inputStream = getObject(GetObjectArgs.builder().bucket(srcBucket).object(itemResponse.getObjectName()).build()).getInputStream();
                    putObject(PutObjectArgs.builder().inputStream(inputStream).bucket(dstBucket).object(itemResponse.getObjectName()).build());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw OssExceptionEnum.COPY_OBJECT_ERROR.getException();
                }
            }
        }
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectArgs args) {
        log.info("copyObject:{}", JSON.toJSONString(args));
        copy(args.getSrcBucket(), args.getSrcObject(), args.getBucket(), StringUtils.isBlank(args.getObject()) ? args.getSrcObject() : args.getObject());
        if (args.getDelete()) {
            try {
                remove(args.getSrcBucket(), args.getSrcObject());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw OssExceptionEnum.DELETE_OBJECT_ERROR.getException();
            }
        }
        return new CopyObjectResponse(args.getSrcBucket(), args.getBucket(), null, args.getSrcObject(), args.getObject());
    }

    @Override
    public GetObjectResponse getObject(GetObjectArgs args) {
        String url = defaultUrlBuilder().bucket(args.getBucket()).object(args.getObject())
                .params(Collections.singletonMap(ApiConstant.OP, ApiConstant.OPEN)).build();
        Response response = null;
        try {
            response = execute(url, ApiConstant.GET, null);
            if ((response.isSuccessful() || response.isRedirect()) && response.body() != null) {
                GetObjectResponse getObjectResponse = new GetObjectResponse();
                getObjectResponse.setBucket(args.getBucket());
                getObjectResponse.setObject(args.getObject());
                getObjectResponse.setInputStream(response.body().byteStream());
                return getObjectResponse;
            }
            throw new OssException(String.valueOf(response.code()), response.message());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (response != null) {
                response.close();
            }
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public void downloadObject(DownloadObjectArgs args) {
        try {
            File file = new File(args.getFileName());
            if (!file.exists()) {
                createFile(file, Boolean.FALSE);
            }
            try (InputStream inputStream = getObject(GetObjectArgs.builder().bucket(args.getBucket()).object(args.getObject()).build()).getInputStream();
                 BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buff = new byte[2048];
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
    public List<ItemResponse> listObjects(ListObjectsArgs args) {
        log.info("listObjects:{}", JSON.toJSONString(args));
        String url = defaultUrlBuilder().bucket(args.getBucket())
                .params(Collections.singletonMap(ApiConstant.OP, ApiConstant.LISTSTATUS)).build();
        try (Response response = execute(url, ApiConstant.GET, null)) {
            JSONObject result = validResult(response);
            List<ItemResponse> itemResponses = new ArrayList<>();
            JSONObject fileStatuses = result.getJSONObject(ApiConstant.FILESTATUSES);
            if (fileStatuses == null) {
                return itemResponses;
            }
            JSONArray fileStatus = fileStatuses.getJSONArray(ApiConstant.FILESTATUS);
            if (fileStatus == null || fileStatus.isEmpty()) {
                return itemResponses;
            }
            for (int i = 0, n = fileStatus.size(); i < n; i++) {
                JSONObject jsonObject = fileStatus.getJSONObject(i);
                String path = jsonObject.getString("pathSuffix");
                if (StringUtils.isBlank(path)) {
                    continue;
                }
                if (StringUtils.isNotBlank(args.getPrefix()) && !path.startsWith(args.getPrefix())) {
                    continue;
                }
                if (StringUtils.isNotBlank(args.getSuffix()) && !path.endsWith(args.getSuffix())) {
                    continue;
                }
                if (args.getMax() != null && itemResponses.size() >= args.getMax()) {
                    break;
                }
                ItemResponse itemResponse = new ItemResponse(args.getBucket(), path, jsonObject.getDate(ApiConstant.MODIFICATION_TIME),
                        null, jsonObject.getLongValue("length"), null, new Owner(jsonObject.getString(ApiConstant.GROUP), jsonObject.getString(ApiConstant.OWNER)),
                        FileType.getType(jsonObject.getString(ApiConstant.TYPE)).getType());
                itemResponses.add(itemResponse);
            }
            return itemResponses;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw OssExceptionEnum.GET_OBJECT_ERROR.getException();
        }
    }

    @Override
    public List<RemoveObjectResponse> removeObjects(RemoveObjectArgs args) {
        log.info("removeObjects:{}", JSON.toJSONString(args));
        if (args.getObjects() == null || args.getObjects().isEmpty()) {
            args.setObjects(Collections.singletonList(null));
        }
        List<RemoveObjectResponse> responses = new ArrayList<>();
        for (String object : args.getObjects()) {
            try {
                boolean remove = remove(args.getBucket(), object);
                if (remove) {
                    responses.add(new RemoveObjectResponse(args.getBucket(), object, null));
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                throw OssExceptionEnum.DELETE_OBJECT_ERROR.getException();
            }
        }
        return responses;
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        String url = defaultUrlBuilder().bucket(bucketName).object(objectName)
                .params(Collections.singletonMap(ApiConstant.OP, ApiConstant.OPEN)).build();
        log.info("object url:{}", url);
        return url;
    }

    @Override
    public String getPresignedObjectUrl(GetPresignedObjectUrlArgs request) {
        return getObjectUrl(request.getBucket(), request.getObject());
    }

    @Override
    public ApiType apiType() {
        return ApiType.WEBHDFS;
    }
}