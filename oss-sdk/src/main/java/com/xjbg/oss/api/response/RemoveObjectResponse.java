package com.xjbg.oss.api.response;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:47
 */
@Getter
@Setter
public class RemoveObjectResponse {
    private String bucketName;

    private String objectName;

    private String versonId;

    public RemoveObjectResponse() {
    }

    public RemoveObjectResponse(String bucketName, String objectName, String versonId) {
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.versonId = versonId;
    }
}
