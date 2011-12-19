package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;

import java.lang.reflect.Constructor;

public enum DocumentViewMode {

    VERTICALL_SCROLL("Vertical scroll", ContiniousDocumentView.class),

    HORIZONTAL_SCROLL("Horizontal scroll", HScrollDocumentView.class),

    SINGLE_PAGE("Single page", SinglePageDocumentView.class);

    private final LogContext LCTX = LogContext.ROOT.lctx("View");

    /** The resource value. */
    private final String resValue;

    private Constructor<? extends IDocumentViewController> c;

    private DocumentViewMode(final String res, final Class<? extends IDocumentViewController> clazz) {
        this.resValue = res;
        try {
            this.c = clazz.getConstructor(IViewerActivity.class);
        } catch (final Exception e) {
            LCTX.e("Cannot find appropriate view controller constructor: ", e);
            this.c = null;
        }
    }

    public IDocumentViewController create(final IViewerActivity base) {
        if (c != null) {
            try {
                return c.newInstance(base);
            } catch (final Exception e) {
                LCTX.e("Cannot find instanciate view controller: ", e);
            }
        }
        return null;
    }

    public String getResValue() {
        return resValue;
    }

    /**
     * Gets the by resource value.
     * 
     * @param resValue
     *            the resource value
     * @return the enum value or @null
     */
    public static DocumentViewMode getByResValue(final String resValue) {
        for (final DocumentViewMode vm : values()) {
            if (vm.resValue.equals(resValue)) {
                return vm;
            }
        }
        return null;
    }

    public static DocumentViewMode getByOrdinal(final int ord) {
        if (0 <= ord && ord < values().length) {
            return values()[ord];
        }
        return VERTICALL_SCROLL;
    }
}
