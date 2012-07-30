package org.ebookdroid.opds.exceptions;

public class HttpRequestFailed extends OPDSException {

    /**
     *
     */
    private static final long serialVersionUID = 4165914806432018449L;

    public final int code;
    public final String reason;

    public HttpRequestFailed(final int code, final String reason) {
        this.code = code;
        this.reason = reason;
    }

}
