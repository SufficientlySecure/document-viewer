package org.ebookdroid.ui.viewer.views;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;


public class ViewEffects {

    public static void toggleControls(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            fade(view, View.GONE, 0.0f, view.getWidth());
        } else {
            fade(view, View.VISIBLE, view.getWidth(), 0.0f);
        }
    }

    private static void fade(View view, final int visibility, final float startDelta, final float endDelta) {
        final Animation anim = new TranslateAnimation(0, 0, startDelta, endDelta);
        anim.setDuration(500);
        view.startAnimation(anim);
        view.setVisibility(visibility);
    }
    
}
