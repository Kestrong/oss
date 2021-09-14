package com.xjbg.oss.api.impl.webhdfs;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.ietf.jgss.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthenticationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@link KerberosAuthenticator} implements the Kerberos SPNEGO
 * authentication sequence.
 * <p/>
 * It uses the default principal for the Kerberos cache (normally set via
 * kinit).
 * <p/>
 * It falls back to the {@link PseudoAuthenticator} if the HTTP endpoint does
 * not trigger an SPNEGO authentication sequence.
 *
 * @author kesc
 * @date 2021-09-09 11:30
 */
@Getter
@Setter
public class KerberosAuthenticator extends PseudoAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(KerberosAuthenticator.class);
    /**
     * HTTP header used by the SPNEGO server endpoint during an authentication
     * sequence.
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final boolean IBM = System.getProperty("java.vendor").contains("IBM");


    private static class KerberosConfiguration extends Configuration {

        private static final String OS_LOGIN_MODULE_NAME;
        private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

        static {
            if (WINDOWS) {
                OS_LOGIN_MODULE_NAME = "com.sun.security.auth.module.NTLoginModule";
            } else {
                OS_LOGIN_MODULE_NAME = "com.sun.security.auth.module.UnixLoginModule";
            }
        }

        private static final AppConfigurationEntry OS_SPECIFIC_LOGIN = new AppConfigurationEntry(
                OS_LOGIN_MODULE_NAME,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                new HashMap<String, String>());

        private static final Map<String, String> USER_KERBEROS_OPTIONS = new HashMap<String, String>();

        static {
            USER_KERBEROS_OPTIONS.put("doNotPrompt", "true");
            USER_KERBEROS_OPTIONS.put("useTicketCache", "true");
            USER_KERBEROS_OPTIONS.put("renewTGT", "true");
            String ticketCache = System.getenv("KRB5CCNAME");
            if (ticketCache != null) {
                USER_KERBEROS_OPTIONS.put("ticketCache", ticketCache);
            }
            //Krb5 in GSS API needs to be refreshed so it does not throw the error
            //Specified version of key is not available
            USER_KERBEROS_OPTIONS.put("refreshKrb5Config", "true");
        }

        private static final AppConfigurationEntry USER_KERBEROS_LOGIN = new AppConfigurationEntry(
                getKrb5LoginModuleName(),
                AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL,
                USER_KERBEROS_OPTIONS);

        private static final AppConfigurationEntry[] USER_KERBEROS_CONF = new AppConfigurationEntry[]{
                OS_SPECIFIC_LOGIN, USER_KERBEROS_LOGIN};

        /* Return the Kerberos login module name */
        public static String getKrb5LoginModuleName() {
            return IBM
                    ? "com.ibm.security.auth.module.Krb5LoginModule"
                    : "com.sun.security.auth.module.Krb5LoginModule";
        }

        public KerberosConfiguration(String userPrincipal, String keyTab, boolean debug) {
            if (new File(keyTab).exists()) {
                USER_KERBEROS_OPTIONS.put("useKeyTab", "true");
                USER_KERBEROS_OPTIONS.put("keyTab", keyTab);
                USER_KERBEROS_OPTIONS.put("principal", userPrincipal);
            } else {
                USER_KERBEROS_OPTIONS.put("tryFirstPass", "true");
                USER_KERBEROS_OPTIONS.put("useFirstPass", "true");
                USER_KERBEROS_OPTIONS.put("storePass", "true");
            }
            USER_KERBEROS_OPTIONS.put("debug", String.valueOf(debug));
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
            return USER_KERBEROS_CONF;
        }
    }

    private String userPrincipal;
    private String keyTab;
    private String url;
    private String servicePrincipal;
    private boolean debug;
    private String krb5;
    private OkHttpClient okHttpClient;
    private Base64 base64 = new Base64(0);

    public KerberosAuthenticator() {
    }


    public KerberosAuthenticator(OkHttpClient okHttpClient, String userPrincipal, String keyTab, String krb5, String url, String servicePrincipal, boolean debug) {
        this.userPrincipal = userPrincipal;
        this.keyTab = keyTab;
        this.krb5 = krb5;
        this.debug = debug;
        this.servicePrincipal = servicePrincipal;
        this.url = url;
        this.okHttpClient = okHttpClient;
        System.setProperty("java.security.krb5.conf", krb5);
        if (debug) {
            System.setProperty("sun.security.spnego.debug", "true");
            System.setProperty("sun.security.krb5.debug", "true");
        }
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    }

    /**
     * Performs SPNEGO authentication against the specified URL.
     * <p/>
     * If a token is given it does a NOP and returns the given token.
     * <p/>
     * If no token is given, it will perform the SPNEGO authentication sequence
     * using an HTTP <code>OPTIONS</code> request.
     *
     * @param url   the URl to authenticate against.
     * @param token the authentication token being used for the user.
     */
    @Override
    public void authenticate(String url, Authenticator.Token token) throws IOException {
        if (!token.isSet()) {
            if (isNegotiate(url)) {
                doSpnegoSequence(url, token);
            } else {
                getFallBackAuthenticator().authenticate(url, token);
            }
        }
    }

    /**
     * If the specified URL does not support SPNEGO authentication, a fallback
     * {@link Authenticator} will be used.
     * <p/>
     * This implementation returns a {@link PseudoAuthenticator}.
     *
     * @return the fallback {@link Authenticator}.
     */
    protected Authenticator getFallBackAuthenticator() {
        return new PseudoAuthenticator(userPrincipal, okHttpClient);
    }


    private boolean isNegotiate(String url) throws IOException {
        boolean negotiate = false;
        Request request = new Request.Builder()
                .url(url)
                .method(AUTH_HTTP_METHOD, null)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            String authHeader = response.header(WWW_AUTHENTICATE);
            negotiate = authHeader != null
                    && authHeader.trim().startsWith(NEGOTIATE);
        }
        return negotiate;
    }

    /**
     * Implements the SPNEGO authentication sequence interaction using the
     * current default principal in the Kerberos cache (normally set via kinit).
     *
     * @param token the authentication token being used for the user.
     */
    private void doSpnegoSequence(String url, Authenticator.Token token) throws AuthenticationException {
        try {
            AccessControlContext context = AccessController.getContext();
            Subject subject = Subject.getSubject(context);
            if (subject == null
                    || (subject.getPrivateCredentials(KerberosKey.class).isEmpty()
                    && subject.getPrivateCredentials(KerberosTicket.class).isEmpty())) {
                LOG.debug("No subject in context, logging in");
                subject = new Subject();
                LoginContext login = new LoginContext("WebHdfsKerberosLogin", subject,
                        new KerberosClientCallbackHandler(userPrincipal, keyTab),
                        new KerberosConfiguration(userPrincipal, keyTab, debug));
                login.login();
            }

            LOG.debug("Kerberos authenticated user: {}", subject);

            Subject.doAs(subject, (PrivilegedExceptionAction<Void>) () -> {
                GSSContext gssContext = null;
                try {
                    GSSManager gssManager = GSSManager.getInstance();
                    String servicePrincipal = getServicePrincipal("HTTP",
                            StringUtils.isBlank(getServicePrincipal()) ?
                                    new URL(url).getHost()
                                    : getServicePrincipal());
                    Oid oid = getOidInstance("NT_GSS_KRB5_PRINCIPAL");
                    GSSName serviceName = gssManager.createName(servicePrincipal, oid);
                    oid = getOidInstance("GSS_KRB5_MECH_OID");
                    gssContext = gssManager.createContext(serviceName, oid, null, GSSContext.DEFAULT_LIFETIME);
                    gssContext.requestCredDeleg(true);
                    gssContext.requestMutualAuth(true);

                    byte[] inToken = new byte[0];
                    byte[] outToken;
                    boolean established = false;
                    Response response = null;
                    // Loop while the context is still not established
                    while (!established) {
                        outToken = gssContext.initSecContext(inToken, 0,
                                inToken.length);
                        if (outToken != null) {
                            response = sendToken(url, outToken);
                        }

                        if (!gssContext.isEstablished()) {
                            inToken = readToken(response);
                        } else {
                            established = true;
                            extractToken(response, token);
                        }
                    }
                } finally {
                    if (gssContext != null) {
                        gssContext.dispose();
                    }
                }
                return null;
            });
        } catch (PrivilegedActionException | LoginException ex) {
            throw new AuthenticationException(ex.getMessage(), ex);
        }
    }

    public String getServicePrincipal(String service, String hostname)
            throws UnknownHostException {
        String fqdn = hostname;
        if (null == fqdn || "".equals(fqdn) || "0.0.0.0".equals(fqdn)) {
            fqdn = InetAddress.getLocalHost().getCanonicalHostName();
        }
        // convert hostname to lowercase as kerberos does not work with hostnames
        // with uppercase characters.
        return service + "/" + fqdn.toLowerCase(Locale.ENGLISH);
    }

    public Oid getOidInstance(String oidName)
            throws ClassNotFoundException, GSSException, NoSuchFieldException,
            IllegalAccessException {
        Class<?> oidClass;
        if (IBM) {
            if ("NT_GSS_KRB5_PRINCIPAL".equals(oidName)) {
                // IBM JDK GSSUtil class does not have field for krb5 principal oid
                return new Oid("1.2.840.113554.1.2.2.1");
            }
            oidClass = Class.forName("com.ibm.security.jgss.GSSUtil");
        } else {
            oidClass = Class.forName("sun.security.jgss.GSSUtil");
        }
        Field oidField = oidClass.getDeclaredField(oidName);
        return (Oid) oidField.get(oidClass);
    }


    private Response sendToken(String url, byte[] outToken) throws IOException {
        String token = base64.encodeToString(outToken);
        Request request = new Request.Builder()
                .url(url)
                .header(AUTHORIZATION, NEGOTIATE + " " + token)
                .method(AUTH_HTTP_METHOD, null)
                .build();
        return okHttpClient.newCall(request).execute();
    }


    private byte[] readToken(Response response) throws IOException {
        if (response == null) {
            return new byte[0];
        }
        int status = response.code();
        if (status == HttpURLConnection.HTTP_OK
                || status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            String authHeader = response.header(WWW_AUTHENTICATE);
            if (authHeader == null || !authHeader.trim().startsWith(NEGOTIATE)) {
                throw new AuthenticationException("Invalid SPNEGO sequence, '"
                        + WWW_AUTHENTICATE + "' header incorrect: "
                        + authHeader);
            }
            String negotiation = authHeader.trim()
                    .substring((NEGOTIATE + " ").length()).trim();
            return base64.decode(negotiation);
        }
        throw new AuthenticationException(
                "Invalid SPNEGO sequence, status code: " + status);
    }


    private static class KerberosClientCallbackHandler implements
            CallbackHandler {
        private String username;
        private String password;

        public KerberosClientCallbackHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) callback;
                    ncb.setName(username);
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback pwcb = (PasswordCallback) callback;
                    pwcb.setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(
                            callback,
                            "We got a "
                                    + callback.getClass().getCanonicalName()
                                    + ", but only NameCallback and PasswordCallback is supported");
                }
            }

        }

    }

}
