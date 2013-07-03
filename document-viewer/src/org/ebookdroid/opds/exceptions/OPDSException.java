package org.ebookdroid.opds.exceptions;

import org.emdev.utils.LengthUtils;

public class OPDSException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 5328207114120530250L;

    public OPDSException() {
    }

    public OPDSException(final String detailMessage) {
        super(detailMessage);
    }

    public OPDSException(final Throwable throwable) {
        super(throwable);
    }

    public OPDSException(final String detailMessage, final Throwable throwable) {
        super(detailMessage, throwable);
    }

    public String getErrorMessage() {
        String message = LengthUtils.safeString(this.getLocalizedMessage(), this.getClass().getName());
        for (Throwable cause = this.getCause(); cause != null; cause = cause.getCause()) {
            message = LengthUtils.safeString(cause.getLocalizedMessage(), message);
        }
        return message;
    }

}
