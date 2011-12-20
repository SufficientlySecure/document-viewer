package org.ebookdroid.core;

import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.settings.books.BookSettings;

import java.lang.reflect.Constructor;

public enum DocumentViewMode {

    VERTICALL_SCROLL("Vertical scroll", PageAlign.WIDTH, ContiniousDocumentView.class),

    HORIZONTAL_SCROLL("Horizontal scroll", PageAlign.HEIGHT, HScrollDocumentView.class),

    SINGLE_PAGE("Single page", null, SinglePageDocumentView.class);

    private final LogContext LCTX = LogContext.ROOT.lctx("View");

    /** The resource value. */
    private final String resValue;

    private final PageAlign pageAlign;

    private Constructor<? extends IDocumentViewController> c;

    private DocumentViewMode(final String res, PageAlign pageAlign, final Class<? extends IDocumentViewController> clazz) {
        this.resValue = res;
        this.pageAlign = pageAlign;
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

    public PageAlign getPageAlign(final BookSettings bs) {
        return pageAlign != null ? pageAlign : (bs != null ? bs.pageAlign : PageAlign.AUTO);
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
