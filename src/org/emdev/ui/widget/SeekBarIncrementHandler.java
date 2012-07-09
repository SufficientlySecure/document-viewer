package org.emdev.ui.widget;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.SeekBar;

public class SeekBarIncrementHandler extends Handler implements OnTouchListener, OnClickListener,
        OnLongClickListener {

    private static final int DELAY = 200;
    private static final int INIT_MULT = 5;
    private static final int NEXT_MULT = 2;

    private SeekBar seekBar;

    boolean started;
    int count;

    public void init(IViewContainer parent, int seekBarId, int minusViewId, int plusViewId) {
        init(parent, (SeekBar) parent.findViewById(minusViewId), minusViewId, plusViewId);
    }

    public void init(IViewContainer parent, SeekBar seekBar, int minusViewId, int plusViewId) {
        this.seekBar = seekBar;

        View minus = parent.findViewById(minusViewId);
        minus.setTag(Integer.valueOf(-1));
        minus.setOnTouchListener(this);
        minus.setOnClickListener(this);
        minus.setOnLongClickListener(this);

        View plus = parent.findViewById(plusViewId);
        plus.setTag(Integer.valueOf(+1));
        plus.setOnTouchListener(this);
        plus.setOnClickListener(this);
        plus.setOnLongClickListener(this);

    }

    @Override
    public void handleMessage(final Message msg) {
        if (started && seekBar != null) {
            count++;
            int delta = msg.what;
            seekBar.incrementProgressBy(delta);
            if (count % (1000 / DELAY) == 0) {
                delta = NEXT_MULT * delta;
            }
            sendMessageDelayed(obtainMessage(delta), DELAY);
        }
    }

    public void startIncrement(final int delta) {
        started = true;
        count = 0;
        handleMessage(obtainMessage(delta));
    }

    public void stopIncrement() {
        started = false;
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            stopIncrement();
        }
        return false;
    }

    @Override
    public void onClick(final View v) {
        if (seekBar != null) {
            final int delta = ((Integer) v.getTag()).intValue();
            seekBar.incrementProgressBy(delta);
        }
    }

    @Override
    public boolean onLongClick(final View v) {
        if (seekBar != null) {
            final int delta = INIT_MULT * ((Integer) v.getTag()).intValue();
            startIncrement(delta);
        }
        return true;
    }
}