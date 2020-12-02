package com.xjbg.oss.api.request;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author kesc
 * @date 2020-08-06 16:57
 */
@Getter
@Setter
public class BaseArgs {

    public abstract static class Builder<B extends Builder<B, A>, A extends BaseArgs> {
        protected List<Consumer<A>> operations;

        protected void validate(A args) {
            //do nothing
        }

        protected void validateNotNull(Object arg, String argName) {
            if (arg == null) {
                throw new IllegalArgumentException(argName + " must not be null.");
            }
        }

        protected void validateNotEmptyString(String arg, String argName) {
            validateNotNull(arg, argName);
            if (arg.isEmpty()) {
                throw new IllegalArgumentException(argName + " must be a non-empty string.");
            }
        }

        protected void validateNullOrNotEmptyString(String arg, String argName) {
            if (arg != null && arg.isEmpty()) {
                throw new IllegalArgumentException(argName + " must be a non-empty string.");
            }
        }

        protected void validateNullOrPositive(Number arg, String argName) {
            if (arg != null && arg.longValue() < 0) {
                throw new IllegalArgumentException(argName + " cannot be non-negative.");
            }
        }

        public Builder() {
            this.operations = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        private A newInstance() {
            try {
                for (Constructor<?> constructor :
                        this.getClass().getEnclosingClass().getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 0) {
                        return (A) constructor.newInstance();
                    }
                }

                throw new RuntimeException(
                        this.getClass().getEnclosingClass() + " must have no argument constructor");
            } catch (InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException
                    | SecurityException e) {
                // Args class must have no argument constructor with at least protected access.
                throw new RuntimeException(e);
            }
        }

        public A build() {
            A args = newInstance();
            operations.forEach(operation -> operation.accept(args));
            validate(args);
            return args;
        }
    }
}
