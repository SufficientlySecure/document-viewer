package org.ebookdroid.common.settings.types;

import static android.view.Gravity.*;
import org.sufficientlysecure.viewer.R;

import org.ebookdroid.EBookDroidApp;

import org.emdev.utils.enums.ResourceConstant;

public enum ToastPosition implements ResourceConstant {

    /**
     * 
     */
    Invisible(R.string.pref_toastposition_invisible, 0),
    /**
     * 
     */
    LeftTop(R.string.pref_toastposition_lefttop, LEFT | TOP),
    /**
     * 
     */
    RightTop(R.string.pref_toastposition_righttop, RIGHT | TOP),
    /**
     * 
     */
    LeftBottom(R.string.pref_toastposition_leftbottom, LEFT | BOTTOM),
    /**
     * 
     */
    Bottom(R.string.pref_toastposition_bottom, CENTER | BOTTOM),
    /**
     * 
     */
    RightBottom(R.string.pref_toastposition_righbottom, RIGHT | BOTTOM);

    public final int position;
    private final String resValue;

    private ToastPosition(int resId, int position) {
        this.resValue = EBookDroidApp.context.getString(resId);
        this.position = position;
    }

    @Override
    public String getResValue() {
        return resValue;
    }
}
