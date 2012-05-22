package org.emdev.utils;

import org.ebookdroid.EBookDroidApp;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class LayoutUtils {

    public static final int FILL_PARENT = ViewGroup.LayoutParams.FILL_PARENT;

    private LayoutUtils() {
    }

    public static View fillInParent(final View parent, final View view) {
        if (parent instanceof FrameLayout) {
            view.setLayoutParams(new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
        } else if (parent instanceof LinearLayout) {
            view.setLayoutParams(new LinearLayout.LayoutParams(FILL_PARENT, FILL_PARENT));
        } else if (parent instanceof AbsListView) {
            view.setLayoutParams(new AbsListView.LayoutParams(FILL_PARENT, FILL_PARENT));
        } else {
            view.setLayoutParams(new ViewGroup.LayoutParams(FILL_PARENT, FILL_PARENT));
        }
        return view;
    }

    public static void maximizeWindow(final Window window) {
        window.setLayout(FILL_PARENT, FILL_PARENT);
    }

    public static int getDeviceSize(final int dipSize) {
        return (int) (dipSize *
                EBookDroidApp.context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
