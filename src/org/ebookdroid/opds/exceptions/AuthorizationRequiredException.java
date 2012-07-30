package org.ebookdroid.opds.exceptions;


public class AuthorizationRequiredException extends OPDSException {

    /**
     * serial version UID.
     */
    private static final long serialVersionUID = -3974414687166243083L;

    public final String host;
    public final String method;

    public AuthorizationRequiredException(final String host, final String method) {
        this.host = host;
        this.method = method;
    }

}
