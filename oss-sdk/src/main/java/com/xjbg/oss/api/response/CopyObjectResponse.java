package com.xjbg.oss.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:12
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CopyObjectResponse {
    private String srcBucket;
    private String bucket;
    private String region;
    private String srcObject;
    private String object;
}
