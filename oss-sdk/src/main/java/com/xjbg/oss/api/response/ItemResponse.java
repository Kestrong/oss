package com.xjbg.oss.api.response;

import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.xjbg.oss.enums.FileType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author kesc
 * @date 2020-08-06 15:34
 */
@Getter
@Setter
public class ItemResponse {
    private String objectName;
    private Date lastModified;
    private String etag;
    private long size;
    private String storageClass;
    private Owner owner;
    private String type;

    public ItemResponse(String objectName, Date lastModified, String etag, long size, String storageClass, Owner owner, String type) {
        this.objectName = objectName;
        this.lastModified = lastModified;
        this.etag = etag;
        this.size = size;
        this.storageClass = storageClass;
        this.owner = owner;
        if (type != null) {
            this.type = type;
        } else {
            this.type = this.objectName != null && (this.objectName.endsWith("/") || this.objectName.endsWith("\\")) ? FileType.DIRECTORY.getType() : FileType.FILE.getType();
        }
    }

    public ItemResponse(S3ObjectSummary item) {
        this(item.getKey(), item.getLastModified(), item.getETag(), item.getSize(), item.getStorageClass(), item.getOwner(), null);
    }

}
