package com.xjbg.oss.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * @author kesc
 * @date 2020-11-26 17:08
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectWriteResponse {
    /**
     * The ETag value of the new object
     */
    private String etag;

    /**
     * The version ID of the new, copied object. This field will only be present
     * if object versioning has been enabled for the bucket to which the object
     * was copied.
     */
    private String versionId;

    /**
     * The time this object expires, or null if it has no expiration
     */
    private Date expirationTime;

    /**
     * The expiration rule for this object
     */
    private String expirationTimeRuleId;

    /**
     * Indicate if the requester is charged for conducting this operation from
     * Requester Pays Buckets.
     */
    private boolean isRequesterCharged;
}
