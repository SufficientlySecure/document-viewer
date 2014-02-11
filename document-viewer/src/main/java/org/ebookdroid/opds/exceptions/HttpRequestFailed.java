package org.ebookdroid.opds.exceptions;

import org.apache.http.StatusLine;

public class HttpRequestFailed extends OPDSException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4165914806432018449L;

    public final int code;
    public final String reason;

    public HttpRequestFailed(final StatusLine statusLine) {
        super(statusLine.toString());
        this.code = statusLine.getStatusCode();
        this.reason = statusLine.getReasonPhrase();
    }

}
