package com.xjbg.oss.api.impl.webhdfs;


import java.io.IOException;

/**
 * @author kesc
 * @date 2021-09-09 11:39
 */
public interface Authenticator {
    /**
     * Authenticates against a URL and returns a {@link Authenticator.Token} to be
     * used by subsequent requests.
     *
     * @param url   the URl to authenticate against.
     * @param token the authentication token being used for the user.
     * @throws IOException
     */
    void authenticate(String url, Authenticator.Token token) throws IOException;

    class NoopAuthenticator implements Authenticator {
        public void authenticate(String url, Authenticator.Token token) throws IOException {
        }
    }

    /**
     * Client side authentication token.
     */
    class Token {

        private String token;

        /**
         * Creates a token.
         */
        public Token() {
        }

        /**
         * Creates a token using an existing string representation of the token.
         *
         * @param tokenStr string representation of the tokenStr.
         */
        public Token(String tokenStr) {
            if (tokenStr == null) {
                throw new IllegalArgumentException("tokenStr cannot be null");
            }
            set(tokenStr);
        }

        /**
         * Returns if a token from the server has been set.
         *
         * @return if a token from the server has been set.
         */
        public boolean isSet() {
            return token != null;
        }

        /**
         * Sets a token.
         *
         * @param tokenStr string representation of the tokenStr.
         */
        void set(String tokenStr) {
            token = tokenStr;
        }

        public String getToken() {
            return token;
        }

        /**
         * Returns the string representation of the token.
         *
         * @return the string representation of the token.
         */
        @Override
        public String toString() {
            return token;
        }

    }
}
