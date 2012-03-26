package org.ebookdroid.common.settings.types;

import static android.view.Gravity.*;
import static org.ebookdroid.R.string.*;

import org.ebookdroid.EBookDroidApp;

import org.emdev.utils.enums.ResourceConstant;

public enum ToastPosition implements ResourceConstant {

    /**
     * 
     */
    Invisible(pref_toastposition_invisible, 0),
    /**
     * 
     */
    LeftTop(pref_toastposition_lefttop, LEFT | TOP),
    /**
     * 
     */
    RightTop(pref_toastposition_righttop, RIGHT | TOP),
    /**
     * 
     */
    LeftBottom(pref_toastposition_leftbottom, LEFT | BOTTOM),
    /**
     * 
     */
    Bottom(pref_toastposition_bottom, CENTER | BOTTOM),
    /**
     * 
     */
    RightBottom(pref_toastposition_righbottom, RIGHT | BOTTOM);

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
