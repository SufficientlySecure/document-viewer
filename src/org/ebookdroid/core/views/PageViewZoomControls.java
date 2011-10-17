package org.ebookdroid.core.views;

import org.ebookdroid.core.events.BringUpZoomControlsListener;
import org.ebookdroid.core.models.ZoomModel;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

public class PageViewZoomControls extends LinearLayout implements BringUpZoomControlsListener {

    public PageViewZoomControls(final Context context, final ZoomModel zoomModel) {
        super(context);
        hide();
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.BOTTOM);
        addView(new ZoomRoll(context, zoomModel));

        zoomModel.addListener(this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        return false;
    }

    @Override
    public void toggleZoomControls() {
        if (getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    private void show() {
        fade(View.VISIBLE, getWidth(), 0.0f);
    }

    private void hide() {
        fade(View.GONE, 0.0f, getWidth());
    }

    private void fade(final int visibility, final float startDelta, final float endDelta) {
        final Animation anim = new TranslateAnimation(0, 0, startDelta, endDelta);
        anim.setDuration(500);
        startAnimation(anim);
        setVisibility(visibility);
    }
}
