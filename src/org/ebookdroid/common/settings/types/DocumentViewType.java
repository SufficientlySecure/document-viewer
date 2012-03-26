package org.ebookdroid.common.settings.types;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.viewers.BaseView;
import org.ebookdroid.ui.viewer.viewers.SurfaceView;

import java.lang.reflect.Constructor;

import org.emdev.utils.enums.ResourceConstant;

public enum DocumentViewType implements ResourceConstant {

    BASE(R.string.pref_docviewtype_base, BaseView.class),

    SURFACE(R.string.pref_docviewtype_surface, SurfaceView.class);

    private final String resValue;

    private final Class<? extends IView> viewClass;

    private DocumentViewType(final int resId, final Class<? extends IView> viewClass) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.viewClass = viewClass;
    }

    public String getResValue() {
        return resValue;
    }

    public IView create(final IActivityController base) {
        try {
            final Constructor<?> c = viewClass.getConstructor(IActivityController.class);
            return (IView) c.newInstance(base);
        } catch (final Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
