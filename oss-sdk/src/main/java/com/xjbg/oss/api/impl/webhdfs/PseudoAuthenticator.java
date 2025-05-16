package com.xjbg.oss.api.impl.webhdfs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * The {@link PseudoAuthenticator} implementation provides an authentication
 * equivalent to Hadoop's Simple authentication, it trusts the value of the
 * 'user.name' Java System property.
 * <p/>
 * The 'user.name' value is propagated using an additional query string
 * parameter {@link #USER_NAME} ('user.name').
 *
 * @author kesc
 * @date 2021-09-09 11:30
 */
public class PseudoAuthenticator implements Authenticator {
    /**
     * HTTP header used by the SPNEGO client endpoint during an authentication
     * sequence.
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * HTTP header prefix used by the SPNEGO client/server endpoints during an
     * authentication sequence.
     */
    public static final String NEGOTIATE = "Negotiate";

    public static final String AUTH_HTTP_METHOD = "OPTIONS";

    /**
     * Name of the HTTP cookie used for the authentication token between the client and the server.
     */
    public static final String AUTH_COOKIE = "hadoop.auth";

    public static final String AUTH_COOKIE_EQ = AUTH_COOKIE + "=";

    /**
     * Name of the additional parameter that carries the 'user.name' value.
     */
    public static final String USER_NAME = "user.name";

    private static final String USER_NAME_EQ = USER_NAME + "=";

    /**
     * Performs simple authentication against the specified URL.
     * <p/>
     * If a token is given it does a NOP and returns the given token.
     * <p/>
     * If no token is given, it will perform an HTTP <code>OPTIONS</code>
     * request injecting an additional parameter {@link #USER_NAME} in the query
     * string with the value returned by the {@link #getUserName()} method.
     * <p/>
     * If the response is successful it will update the authentication token.
     *
     * @param url   the URl to authenticate against.
     * @param token the authencation token being used for the user.
     */
    @Override
    public void authenticate(String url, Authenticator.Token token) throws IOException {
        String strUrl = url;
        String paramSeparator = (strUrl.contains("?")) ? "&" : "?";
        strUrl += paramSeparator + USER_NAME_EQ + getUserName();
        Request request = new Request.Builder()
                .url(strUrl)
                .header(AUTHORIZATION, NEGOTIATE + " " + token)
                .method(AUTH_HTTP_METHOD, null)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            extractToken(response, token);
        }
    }

    public static void extractToken(Response response, Token token) throws IOException {
        if (response != null && (response.code() == HttpURLConnection.HTTP_OK
                || response.code() == HttpURLConnection.HTTP_CREATED
                || response.code() == HttpURLConnection.HTTP_ACCEPTED)) {
            Map<String, List<String>> headers = response.headers().toMultimap();
            List<String> cookies = headers.get("Set-Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.startsWith(AUTH_COOKIE_EQ)) {
                        String value = cookie.substring(AUTH_COOKIE_EQ.length());
                        int separator = value.indexOf(";");
                        if (separator > -1) {
                            value = value.substring(0, separator);
                        }
                        if (value.length() > 0) {
                            token.set(value);
                        }
                    }
                }
            }
        } else {
            throw new AuthenticationException("Authentication failed, status: " + response.code() +
                    ", message: " + response.message());
        }
    }

    /**
     * Returns the current user name.
     * <p/>
     * This implementation returns the value of the Java system property
     * 'user.name'
     *
     * @return the current user name.
     */
    protected String getUserName() {
        return username != null ? username : System.getProperty("user.name");
    }

    private String username = null;
    private OkHttpClient okHttpClient;

    public PseudoAuthenticator(String username, OkHttpClient okHttpClient) {
        this.username = username;
        this.okHttpClient = okHttpClient;
    }

    public PseudoAuthenticator() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
