package com.mediamelon.smartstreaming.Utils.http;

import java.io.FilterInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Lightweight Java HTTP-Client for calling JSON REST-Services (especially for Android).
 *
 * @author hgoebl
 */
public class mmHTTPClient {
    //public static final String DEFAULT_USER_AGENT = mmHTTPConst.DEFAULT_USER_AGENT;
    public static final String APP_FORM = mmHTTPConst.APP_FORM;
    public static final String APP_JSON = mmHTTPConst.APP_JSON;
    public static final String APP_BINARY = mmHTTPConst.APP_BINARY;
    public static final String TEXT_PLAIN = mmHTTPConst.TEXT_PLAIN;
    public static final String HDR_CONTENT_TYPE = mmHTTPConst.HDR_CONTENT_TYPE;
    public static final String HDR_CONTENT_ENCODING = mmHTTPConst.HDR_CONTENT_ENCODING;
    public static final String HDR_ACCEPT = mmHTTPConst.HDR_ACCEPT;
    public static final String HDR_ACCEPT_ENCODING = mmHTTPConst.HDR_ACCEPT_ENCODING;
    public static final String HDR_USER_AGENT = mmHTTPConst.HDR_USER_AGENT;
    public static final String HDR_AUTHORIZATION = "Authorization";

    static final Map<String, Object> globalHeaders = new LinkedHashMap<String, Object>();
    static String globalBaseUri;

    static Integer connectTimeout = 10000; // 10 seconds
    static Integer readTimeout = 3 * 60000; // 5 minutes
    static int jsonIndentFactor = -1;

    Boolean followRedirects;
    String baseUri;
    Map<String, Object> defaultHeaders;
    SSLSocketFactory sslSocketFactory;
    HostnameVerifier hostnameVerifier;
    mmHTTPRetryManager retryManager;
    Proxy proxy;

    protected mmHTTPClient() {}

    /**
     * Create an instance which can be reused for multiple requests in the same Thread.
     * @return the created instance.
     */
    public static mmHTTPClient create() {
        return new mmHTTPClient();
    }

    /**
     * Set the value for a named header which is valid for all requests in the running JVM.
     * <br>
     * The value can be overwritten by calling {@link mmHTTPClient#setDefaultHeader(String, Object)} and/or
     * {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#header(String, Object)}.
     * <br>
     * For the supported types for values see {@link mmHTTPRequest#header(String, Object)}.
     *
     * @param name name of the header (regarding HTTP it is not case-sensitive, but here case is important).
     * @param value value of the header. If <code>null</code> the header value is cleared (effectively not set).
     *
     * @see #setDefaultHeader(String, Object)
     * @see com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#header(String, Object)
     */
    public static void setGlobalHeader(String name, Object value) {
        if (value != null) {
            globalHeaders.put(name, value);
        } else {
            globalHeaders.remove(name);
        }
    }

    /**
     * Set the base URI for all requests starting in this JVM from now.
     * <br>
     * For all requests this value is taken as a kind of prefix for the effective URI, so you can address
     * the URIs relatively. The value is only taken when {@link mmHTTPClient#setBaseUri(String)} is not called or
     * called with <code>null</code>.
     *
     * @param globalBaseUri the prefix for all URIs of new Requests.
     * @see #setBaseUri(String)
     */
    public static void setGlobalBaseUri(String globalBaseUri) {
        mmHTTPClient.globalBaseUri = globalBaseUri;
    }

    /**
     * The number of characters to indent child properties, <code>-1</code> for "productive" code.
     * <br>
     * Default is production ready JSON (-1) means no indentation (single-line serialization).
     * @param indentFactor the number of spaces to indent
     */
    public static void setJsonIndentFactor(int indentFactor) {
        mmHTTPClient.jsonIndentFactor = indentFactor;
    }

    /**
     * Set the timeout in milliseconds for connecting the server.
     * <br>
     * In contrast to {@link java.net.HttpURLConnection}, we use a default timeout of 10 seconds, since no
     * timeout is odd.<br>
     * Can be overwritten for each Request with {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#connectTimeout(int)}.
     * @param globalConnectTimeout the new timeout or <code>&lt;= 0</code> to use HttpURLConnection default timeout.
     */
    public static void setConnectTimeout(int globalConnectTimeout) {
        connectTimeout = globalConnectTimeout > 0 ? globalConnectTimeout : null;
    }

    /**
     * Set the timeout in milliseconds for getting response from the server.
     * <br>
     * In contrast to {@link java.net.HttpURLConnection}, we use a default timeout of 3 minutes, since no
     * timeout is odd.<br>
     * Can be overwritten for each Request with {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#readTimeout(int)}.
     * @param globalReadTimeout the new timeout or <code>&lt;= 0</code> to use HttpURLConnection default timeout.
     */
    public static void setReadTimeout(int globalReadTimeout) {
        readTimeout = globalReadTimeout > 0 ? globalReadTimeout : null;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#setInstanceFollowRedirects(boolean)">
     *     </a>.
     * <br>
     * Use this method to set the behaviour for all requests created by this instance when receiving redirect responses.
     * You can overwrite the setting for a single request by calling {@link mmHTTPRequest#followRedirects(boolean)}.
     * @param auto <code>true</code> to automatically follow redirects (HTTP status code 3xx).
     *             Default value comes from HttpURLConnection and should be <code>true</code>.
     */
    public void setFollowRedirects(boolean auto) {
        this.followRedirects = auto;
    }

    /**
     * Set a custom {@link javax.net.ssl.SSLSocketFactory}, most likely to relax Certification checking.
     * @param sslSocketFactory the factory to use (see test cases for an example).
     */
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * Set a custom {@link javax.net.ssl.HostnameVerifier}, most likely to relax host-name checking.
     * @param hostnameVerifier the verifier (see test cases for an example).
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * Sets a proxy object to be used for opening the connection.
     * See {@link URL#openConnection(Proxy)}
     * @param proxy the proxy to be used or <tt>null</tt> for no proxy.
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Set the base URI for all requests created from this instance.
     * <br>
     * For all requests this value is taken as a kind of prefix for the effective URI, so you can address
     * the URIs relatively. The value takes precedence over the value set in {@link #setGlobalBaseUri(String)}.
     *
     * @param baseUri the prefix for all URIs of new Requests.
     * @see #setGlobalBaseUri(String)
     */
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * Returns the base URI of this instance.
     *
     * @return base URI
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Set the value for a named header which is valid for all requests created by this instance.
     * <br>
     * The value takes precedence over {@link mmHTTPClient#setGlobalHeader(String, Object)} but can be overwritten by
     * {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#header(String, Object)}.
     * <br>
     * For the supported types for values see {@link mmHTTPRequest#header(String, Object)}.
     *
     * @param name name of the header (regarding HTTP it is not case-sensitive, but here case is important).
     * @param value value of the header. If <code>null</code> the header value is cleared (effectively not set).
     *              When setting the value to null, a value from global headers can shine through.
     *
     * @see #setGlobalHeader(String, Object)
     * @see com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#header(String, Object)
     */
    public void setDefaultHeader(String name, Object value) {
        if (defaultHeaders == null) {
            defaultHeaders = new HashMap<String, Object>();
        }
        if (value == null) {
            defaultHeaders.remove(name);
        } else {
            defaultHeaders.put(name, value);
        }
    }

    /**
     * Registers an alternative {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRetryManager}.
     * @param retryManager the new manager for deciding whether it makes sense to retry a request.
     */
    public void setRetryManager(mmHTTPRetryManager retryManager) {
        this.retryManager = retryManager;
    }

    /**
     * Creates a <b>GET HTTP</b> request with the specified absolute or relative URI.
     * @param pathOrUri the URI (will be concatenated with global URI or default URI without further checking).
     *                  If it starts already with http:// or https:// this URI is taken and all base URIs are ignored.
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public mmHTTPRequest get(String pathOrUri) {
        return new mmHTTPRequest(this, mmHTTPRequest.Method.GET, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>POST</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI (will be concatenated with global URI or default URI without further checking)
     *                  If it starts already with http:// or https:// this URI is taken and all base URIs are ignored.
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public mmHTTPRequest post(String pathOrUri) {
        return new mmHTTPRequest(this, mmHTTPRequest.Method.POST, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>PUT</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI (will be concatenated with global URI or default URI without further checking)
     *                  If it starts already with http:// or https:// this URI is taken and all base URIs are ignored.
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public mmHTTPRequest put(String pathOrUri) {
        return new mmHTTPRequest(this, mmHTTPRequest.Method.PUT, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>DELETE</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI (will be concatenated with global URI or default URI without further checking)
     *                  If it starts already with http:// or https:// this URI is taken and all base URIs are ignored.
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public mmHTTPRequest delete(String pathOrUri) {
        return new mmHTTPRequest(this, mmHTTPRequest.Method.DELETE, buildPath(pathOrUri));
    }

    private String buildPath(String pathOrUri) {
        if (pathOrUri == null) {
            throw new IllegalArgumentException("pathOrUri must not be null");
        }
        if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
            return pathOrUri;
        }
        String myBaseUri = baseUri != null ? baseUri : globalBaseUri;
        return myBaseUri == null ? pathOrUri : myBaseUri + pathOrUri;
    }

    <T> mmHTTPResponse<T> execute(mmHTTPRequest request, Class<T> clazz) {
        mmHTTPResponse<T> response = null;

        if (request.retryCount == 0) {
            // no retry -> just delegate to inner method
            response = _execute(request, clazz);
        } else {
            if (retryManager == null) {
                retryManager = mmHTTPRetryManager.DEFAULT;
            }
            for (int tries = 0; tries <= request.retryCount; ++tries) {
                try {
                    response = _execute(request, clazz);
                    if (tries >= request.retryCount || !retryManager.isRetryUseful(response)) {
                        break;
                    }
                } catch (mmHTTPException we) {
                    // analyze: is exception recoverable?
                    if (tries >= request.retryCount || !retryManager.isRecoverable(we)) {
                        throw we;
                    }
                }
                if (request.waitExponential) {
                    retryManager.wait(tries);
                }
            }
        }
        if (response == null) {
            throw new IllegalStateException(); // should never reach this line
        }
        if (request.ensureSuccess) {
            response.ensureSuccess();
        }

        return response;
    }

    private <T> mmHTTPResponse<T> _execute(mmHTTPRequest request, Class<T> clazz) {
        mmHTTPResponse<T> response = new mmHTTPResponse<T>(request);

        InputStream is = null;
        boolean closeStream = true;
        HttpURLConnection connection = null;

        try {
            String uri = request.uri;
            if (request.method == mmHTTPRequest.Method.GET &&
                    !uri.contains("?") &&
                    request.params != null &&
                    !request.params.isEmpty()) {
                uri += "?" + mmHTTPUtils.queryString(request.params);
            }
            URL apiUrl = new URL(uri);
            if (proxy != null) {
                connection = (HttpURLConnection) apiUrl.openConnection(proxy);
            } else {
                connection = (HttpURLConnection) apiUrl.openConnection();
            }

            prepareSslConnection(connection);
            connection.setRequestMethod(request.method.name());
            if (request.followRedirects != null) {
                connection.setInstanceFollowRedirects(request.followRedirects);
            }
            connection.setUseCaches(request.useCaches);
            setTimeouts(request, connection);
            if (request.ifModifiedSince != null) {
                connection.setIfModifiedSince(request.ifModifiedSince);
            }

            mmHTTPUtils.addRequestProperties(connection, mergeHeaders(request.headers));
            if (clazz == JSONObject.class || clazz == JSONArray.class) {
                mmHTTPUtils.ensureRequestProperty(connection, HDR_ACCEPT, APP_JSON);
            }

            if (request.method != mmHTTPRequest.Method.GET && request.method != mmHTTPRequest.Method.DELETE) {
                if (request.streamPayload) {
                    mmHTTPUtils.setContentTypeAndLengthForStreaming(connection, request, request.compress);
                    connection.setDoOutput(true);
                    streamBody(connection, request.payload, request.compress);
                } else {
                    byte[] requestBody = mmHTTPUtils.getPayloadAsBytesAndSetContentType(
                            connection, request, request.compress, jsonIndentFactor);

                    if (requestBody != null) {
                        connection.setDoOutput(true);
                        writeBody(connection, requestBody);
                    }
                }
            } else {
                //connection.connect();
            }

            response.connection = connection;
            response.responseMessage = connection.getResponseMessage();
            response.statusCode = connection.getResponseCode();

            // get the response body (if any)
            is = response.isSuccess() ? connection.getInputStream() : connection.getErrorStream();
            is = mmHTTPUtils.wrapStream(connection.getContentEncoding(), is);


            if (clazz == InputStream.class) {
                is = new AutoDisconnectInputStream(connection, is);
            }
            if (response.isSuccess()) {
                mmHTTPUtils.parseResponseBody(clazz, response, is);
            } else {
                mmHTTPUtils.parseErrorResponse(clazz, response, is);
            }
            if (clazz == InputStream.class) {
                closeStream = false;
            }


            return response;

        } catch (mmHTTPException e) {

            throw e;

        } catch (Exception e) {

            throw new mmHTTPException(e);

        } finally {
            if (closeStream) {
                if (is != null) {
                    try { is.close(); } catch (Exception ignored) {}
                }
                if (connection != null) {
                    try { connection.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void setTimeouts(mmHTTPRequest request, HttpURLConnection connection) {
        if (request.connectTimeout != null || connectTimeout != null) {
            connection.setConnectTimeout(
                    request.connectTimeout != null ? request.connectTimeout : connectTimeout);
        }
        if (request.readTimeout != null || readTimeout != null) {
            connection.setReadTimeout(
                    request.readTimeout != null ? request.readTimeout : readTimeout);
        }
    }

    private void writeBody(HttpURLConnection connection, byte[] body) throws IOException {
        // Android StrictMode might complain about not closing the connection:
        // "E/StrictMode﹕ A resource was acquired at attached stack trace but never released"
        // It seems like some kind of bug in special devices (e.g. 4.0.4/Sony) but does not
        // happen e.g. on 4.4.2/Moto G.
        // Closing the stream in the try block might help sometimes (it's intermittently),
        // but I don't want to deal with the IOException which can be thrown in close().
        OutputStream os = null;
        try {
            os = connection.getOutputStream();
            os.write(body);
            os.flush();
        } finally {
            if (os != null) {
                try { os.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void streamBody(HttpURLConnection connection, Object body, boolean compress) throws IOException {
        InputStream is;
        boolean closeStream;

        if (body instanceof File) {
            is = new FileInputStream((File) body);
            closeStream = true;
        } else {
            is = (InputStream) body;
            closeStream = false;
        }

        // "E/StrictMode﹕ A resource was acquired at attached stack trace but never released"
        // see comments about this problem in #writeBody()
        OutputStream os = null;
        try {
            os = connection.getOutputStream();
            if (compress) {
                os = new GZIPOutputStream(os);
            }
            mmHTTPUtils.copyStream(is, os);
            os.flush();
        } finally {
            if (os != null) {
                try { os.close(); } catch (Exception ignored) {}
            }
            if (is != null && closeStream) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void prepareSslConnection(HttpURLConnection connection) {
        if ((hostnameVerifier != null || sslSocketFactory != null) && connection instanceof HttpsURLConnection) {
            HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
            if (hostnameVerifier != null) {
                sslConnection.setHostnameVerifier(hostnameVerifier);
            }
            if (sslSocketFactory != null) {
                sslConnection.setSSLSocketFactory(sslSocketFactory);
            }
        }
    }

    Map<String, Object> mergeHeaders(Map<String, Object> requestHeaders) {
        Map<String, Object> headers = null;
        if (!globalHeaders.isEmpty()) {
            headers = new LinkedHashMap<String, Object>();
            headers.putAll(globalHeaders);
        }
        if (defaultHeaders != null) {
            if (headers == null) {
                headers = new LinkedHashMap<String, Object>();
            }
            headers.putAll(defaultHeaders);
        }
        if (requestHeaders != null) {
            if (headers == null) {
                headers = requestHeaders;
            } else {
                headers.putAll(requestHeaders);
            }
        }
        return headers;
    }

    /**
     * Disconnect the underlying <code>HttpURLConnection</code> on close.
     */
    private static class AutoDisconnectInputStream extends FilterInputStream {

        /**
         * The underlying <code>HttpURLConnection</code>.
         */
        private final HttpURLConnection connection;

        /**
         * Creates an <code>AutoDisconnectInputStream</code>
         * by assigning the  argument <code>in</code>
         * to the field <code>this.in</code> so as
         * to remember it for later use.
         * @param connection the underlying connection to disconnect on close.
         * @param in the underlying input stream, or <code>null</code> if
         * this instance is to be created without an underlying stream.
         */
        protected AutoDisconnectInputStream(final HttpURLConnection connection, final InputStream in) {
            super(in);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }
}
