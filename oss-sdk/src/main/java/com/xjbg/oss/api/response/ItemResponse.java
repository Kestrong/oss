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
        this.type = type;
    }


    public ItemResponse(S3ObjectSummary item) {
        this.objectName = item.getKey();
        this.lastModified = item.getLastModified();
        this.etag = item.getETag();
        this.size = item.getSize();
        this.storageClass = item.getStorageClass();
        com.amazonaws.services.s3.model.Owner owner = item.getOwner();
        this.owner = owner == null ? null : new Owner(owner.getId(), owner.getDisplayName());
        this.type = this.objectName != null && (this.objectName.endsWith("/") || this.objectName.endsWith("\\")) ? FileType.DIRECTORY.getType() : FileType.FILE.getType();
    }

}
