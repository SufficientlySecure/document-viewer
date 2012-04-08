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

    public SeekBarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        minValue = WidgetUtils.getIntAttribute(context, attrs, EBOOKDROID_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
        maxValue = WidgetUtils.getIntAttribute(context, attrs, EBOOKDROID_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
        defaultValue = WidgetUtils.getIntAttribute(context, attrs, ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_DEFAULT_VALUE);
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
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        // TODO Auto-generated method stub

    }
}
