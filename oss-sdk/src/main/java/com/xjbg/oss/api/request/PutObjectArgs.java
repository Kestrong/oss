package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:39
 */
@Getter
@Setter
public class PutObjectArgs extends ObjectWriteArgs {

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder extends ObjectWriteArgs.Builder<Builder, PutObjectArgs> {

    }

}
