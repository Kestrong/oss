package com.xjbg.oss.api.response;

import com.amazonaws.services.s3.model.Owner;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author kesc
 * @date 2020-08-06 15:11
 */
@Getter
@Setter
public class BucketResponse {

    /**
     * bucket name
     */
    private String name;

    /**
     * bucket create date
     */
    private Date creationDate;

    /**
     * The details on the owner of this bucket
     */
    private Owner owner = null;
}
