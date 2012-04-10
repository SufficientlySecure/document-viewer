package org.emdev.ui.preference;

import org.ebookdroid.R;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.WidgetUtils;

public final class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

    private static final String EBOOKDROID_NS = "http://ebookdroid.org";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final String ATTR_MIN_VALUE = "minValue";
    private static final String ATTR_MAX_VALUE = "maxValue";
    private static final String ATTR_DEFAULT_VALUE = "defaultValue";

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_DEFAULT_VALUE = 50;

    private final int defaultValue;
    private final int maxValue;
    private final int minValue;
    private int currentValue;

    private SeekBar seekBar;
    private TextView text;
    private View plus;
    private View minus;

    private final IncrementHandler handler;

    public SeekBarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        handler = new IncrementHandler();
        minValue = WidgetUtils.getIntAttribute(context, attrs, EBOOKDROID_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
        maxValue = WidgetUtils.getIntAttribute(context, attrs, EBOOKDROID_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
        defaultValue = WidgetUtils.getIntAttribute(context, attrs, ANDROID_NS, ATTR_DEFAULT_VALUE,
                DEFAULT_DEFAULT_VALUE);
    }

    public int getValue() {
        return currentValue;
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        currentValue = Integer.parseInt(getPersistedString(LengthUtils.toString(defaultValue)));
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.pref_seek_dialog, null);

        currentValue = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));

        ((TextView) view.findViewById(R.id.pref_seek_min_value)).setText(Integer.toString(minValue));
        ((TextView) view.findViewById(R.id.pref_seek_max_value)).setText(Integer.toString(maxValue));

        seekBar = (SeekBar) view.findViewById(R.id.pref_seek_bar);
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(currentValue - minValue);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setOnSeekBarChangeListener(this);

        text = (TextView) view.findViewById(R.id.pref_seek_current_value);
        text.setText(Integer.toString(currentValue));

        minus = view.findViewById(R.id.pref_seek_bar_minus);
        minus.setTag(Integer.valueOf(-1));
        minus.setOnTouchListener(handler);
        minus.setOnClickListener(handler);
        minus.setOnLongClickListener(handler);

        plus = view.findViewById(R.id.pref_seek_bar_plus);
        plus.setTag(Integer.valueOf(+1));
        plus.setOnTouchListener(handler);
        plus.setOnClickListener(handler);
        plus.setOnLongClickListener(handler);

        return view;
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            final String value = Integer.toString(currentValue);
            if (callChangeListener(value)) {
                if (shouldPersist()) {
                    persistString(value);
                }
                notifyChanged();
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        final String summary = super.getSummary().toString();
        final int value = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));
        return String.format(summary, value);
    }

    @Override
    public void onProgressChanged(final SeekBar seek, final int value, final boolean fromTouch) {
        currentValue = value + minValue;
        text.setText(Integer.toString(currentValue));
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    private class IncrementHandler extends Handler implements OnTouchListener, OnClickListener, OnLongClickListener {

        private static final int DELAY = 200;
        private static final int INIT_MULT = 5;
        private static final int NEXT_MULT = 2;

        boolean started;
        int count;

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
}
