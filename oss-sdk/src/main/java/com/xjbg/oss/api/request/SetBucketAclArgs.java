package com.xjbg.oss.api.request;

import com.amazonaws.services.s3.model.*;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @date 2020-11-30 14:28
 */
@Getter
@Setter
public class SetBucketAclArgs extends BucketArgs {
    private AccessControlList acl = new AccessControlList();
    private String expectedBucketOwner;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BucketArgs.Builder<Builder, SetBucketAclArgs> {
        public Builder grantUser(Owner owner, Permission permission) {
            validateNotNull(owner, "grant user");
            validateNotNull(permission, "permission");
            operations.add(args -> {
                CanonicalGrantee canonicalGrantee = new CanonicalGrantee(owner.getId());
                canonicalGrantee.setDisplayName(owner.getDisplayName());
                args.getAcl().grantPermission(canonicalGrantee, permission);
            });
            return this;
        }

        public Builder grantEmail(String email, Permission permission) {
            validateNotEmptyString(email, "email");
            validateNotNull(permission, "permission");
            operations.add(args -> {
                args.getAcl().grantPermission(new EmailAddressGrantee(email), permission);
            });
            return this;
        }

        public Builder grantGroup(GroupGrantee group, Permission permission) {
            validateNotNull(group, "group");
            validateNotNull(permission, "permission");
            operations.add(args -> {
                args.getAcl().grantPermission(group, permission);
            });
            return this;
        }

        public Builder owner(Owner owner) {
            validateNotNull(owner, "owner");
            operations.add(args -> args.getAcl().setOwner(owner));
            return this;
        }

        public Builder expectedBucketOwner(String expectedBucketOwner) {
            operations.add(args -> args.expectedBucketOwner = expectedBucketOwner);
            return this;
        }
    }

}
