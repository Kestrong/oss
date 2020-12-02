package com.xjbg.oss.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-11-27 14:15
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PutObjectResponse extends ObjectWriteResponse {
    private String bucket;
    private String region;
    private String object;
}
