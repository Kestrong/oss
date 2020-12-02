package com.xjbg.oss.api.response;

import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Owner;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kesc
 * @date 2020-12-01 18:19
 */
@Getter
@Setter
public class AclResponse {
    private List<Grant> granteeList;
    private Owner owner;
}
