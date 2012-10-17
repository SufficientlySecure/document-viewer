package org.emdev.ui.preference;

import org.ebookdroid.R;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.IllegalFormatException;

import org.emdev.ui.widget.IViewContainer;
import org.emdev.ui.widget.SeekBarIncrementHandler;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.WidgetUtils;

public final class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_DEFAULT_VALUE = 50;

    private final int defaultValue;
    private final int maxValue;
    private final int minValue;
    private int currentValue;

    private SeekBar seekBar;
    private TextView text;

    private final SeekBarIncrementHandler handler;

    public SeekBarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        handler = new SeekBarIncrementHandler();

        minValue = WidgetUtils.getIntAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_MIN_VALUE,
                DEFAULT_MIN_VALUE);
        maxValue = WidgetUtils.getIntAttribute(context, attrs, WidgetUtils.EBOOKDROID_NS, WidgetUtils.ATTR_MAX_VALUE,
                DEFAULT_MAX_VALUE);
        defaultValue = WidgetUtils.getIntAttribute(context, attrs, WidgetUtils.ANDROID_NS,
                WidgetUtils.ATTR_DEFAULT_VALUE, DEFAULT_DEFAULT_VALUE);
    }

    public int getValue() {
        return currentValue;
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        try {
            currentValue = Integer.parseInt(getPersistedString(LengthUtils.toString(defaultValue)));
        } catch (NumberFormatException ex) {
            currentValue = minValue;
        }
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.pref_seek_dialog, null);

        try {
            currentValue = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));
        } catch (NumberFormatException ex) {
            currentValue = minValue;
        }

        ((TextView) view.findViewById(R.id.pref_seek_min_value)).setText(Integer.toString(minValue));
        ((TextView) view.findViewById(R.id.pref_seek_max_value)).setText(Integer.toString(maxValue));

        seekBar = (SeekBar) view.findViewById(R.id.pref_seek_bar);
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(currentValue - minValue);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setOnSeekBarChangeListener(this);

        text = (TextView) view.findViewById(R.id.pref_seek_current_value);
        text.setText(Integer.toString(currentValue));

        handler.init(new IViewContainer.ViewBridge(view), seekBar, R.id.pref_seek_bar_minus, R.id.pref_seek_bar_plus);
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
        int value = minValue;
        try {
            value = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));
        } catch (NumberFormatException ex) {
        }
        try {
            return String.format(summary, value);
        } catch (IllegalFormatException ex) {
            System.err.println("Error on summary formatting for " + getKey()+": " + ex.getMessage());
        }
        return summary;
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
}
