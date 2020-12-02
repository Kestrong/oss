package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-08-06 15:19
 */
@Getter
@Setter
public class GetObjectArgs extends ObjectArgs {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ObjectArgs.Builder<Builder, GetObjectArgs> {

    }
}
