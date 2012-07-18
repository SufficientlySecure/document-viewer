package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.VScrollController;
import org.ebookdroid.core.HScrollController;
import org.ebookdroid.core.SinglePageController;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController;

import java.lang.reflect.Constructor;

import org.emdev.common.log.LogContext;
import org.emdev.common.log.LogManager;
import org.emdev.utils.enums.ResourceConstant;

public enum DocumentViewMode implements ResourceConstant {

    VERTICALL_SCROLL(R.string.pref_viewmode_vertical_scroll, PageAlign.WIDTH, VScrollController.class),

    HORIZONTAL_SCROLL(R.string.pref_viewmode_horizontal_scroll, PageAlign.HEIGHT, HScrollController.class),

    SINGLE_PAGE(R.string.pref_viewmode_single_page, null, SinglePageController.class);

    private final LogContext LCTX = LogManager.root().lctx("View");

    /** The resource value. */
    private final String resValue;

    private final PageAlign pageAlign;

    private Constructor<? extends IViewController> c;

    private DocumentViewMode(final int resId, final PageAlign pageAlign,
            final Class<? extends IViewController> clazz) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.pageAlign = pageAlign;
        try {
            this.c = clazz.getConstructor(IActivityController.class);
        } catch (final Exception e) {
            LCTX.e("Cannot find appropriate view controller constructor: ", e);
            this.c = null;
        }
    }

    public IViewController create(final IActivityController base) {
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

    public static PageAlign getPageAlign(final BookSettings bs) {
        if (bs == null || bs.viewMode == null) {
            return PageAlign.AUTO;
        }
        final PageAlign defAlign = bs.viewMode.pageAlign;
        return defAlign != null ? defAlign : bs.pageAlign;
    }

    public static DocumentViewMode getByOrdinal(final int ord) {
        if (0 <= ord && ord < values().length) {
            return values()[ord];
        }
        return VERTICALL_SCROLL;
    }
}
