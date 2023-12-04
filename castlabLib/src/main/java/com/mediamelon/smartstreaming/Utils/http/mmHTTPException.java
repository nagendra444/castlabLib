package com.mediamelon.smartstreaming.Utils.http;

/**
 * Runtime exception wrapping the real exception thrown by HttpUrlConnection et al.
 *
 * @author hgoebl
 */
public class mmHTTPException extends RuntimeException {

    private mmHTTPResponse response;

    public mmHTTPException(String message) {
        super(message);
    }

    public mmHTTPException(String message, mmHTTPResponse response) {
        super(message);
        this.response = response;
    }

    public mmHTTPException(String message, Throwable cause) {
        super(message, cause);
    }

    public mmHTTPException(Throwable cause) {
        super(cause);
    }

    /**
     * Get the Response object
     * (only available if exception has been raised by {@link com.mediamelon.smartstreaming.Utils.http.mmHTTPRequest#ensureSuccess()}.
     *
     * @return the <code>Response</code> object filled with error information like statusCode and errorBody.
     */
    public mmHTTPResponse getResponse() {
        return response;
    }
}
