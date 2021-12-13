package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kesc
 * @date 2020-08-06 15:44
 */
@Getter
@Setter
public class RemoveObjectArgs extends BucketArgs {
    private List<String> objects = new ArrayList<>();
    /**
     * 递归删除
     */
    private boolean recursive = false;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, RemoveObjectArgs> {

        public Builder objects(List<String> objects) {
            operations.add(args -> args.objects.addAll(objects));
            return this;
        }

        public Builder recursive(boolean recursive) {
            operations.add(args -> args.recursive = recursive);
            return this;
        }

    }
}
