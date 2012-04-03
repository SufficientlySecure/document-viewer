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

public final class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {

    private static final String EBOOKDROID_NS = "http://ebookdroid.org";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final String ATTR_DEFAULT_VALUE = "defaultValue";
    private static final String ATTR_MIN_VALUE = "minValue";
    private static final String ATTR_MAX_VALUE = "maxValue";

    private final int defaultValue;
    private final int maxValue;
    private final int minValue;
    private int currentValue;

    private SeekBar seekBar;
    private TextView text;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        minValue = attrs.getAttributeIntValue(EBOOKDROID_NS, ATTR_MIN_VALUE, 0);
        maxValue = attrs.getAttributeIntValue(EBOOKDROID_NS, ATTR_MAX_VALUE, 100);
        defaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, 50);
    }

    public int getValue() {
        return currentValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        currentValue = Integer.parseInt(getPersistedString(LengthUtils.toString(defaultValue)));
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pref_seek_dialog, null);
        
        currentValue = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));

        ((TextView) view.findViewById(R.id.pref_seek_min_value)).setText(Integer.toString(minValue));
        ((TextView) view.findViewById(R.id.pref_seek_max_value)).setText(Integer.toString(maxValue));

        seekBar = (SeekBar) view.findViewById(R.id.pref_seek_bar);
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(currentValue - minValue);
        seekBar.setOnSeekBarChangeListener(this);

        text = (TextView) view.findViewById(R.id.pref_seek_current_value);
        text.setText(Integer.toString(currentValue));

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = Integer.toString(currentValue);
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
        String summary = super.getSummary().toString();
        int value = Integer.parseInt(getPersistedString(Integer.toString(defaultValue)));
        return String.format(summary, value);
    }

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        currentValue = value + minValue;
        text.setText(Integer.toString(currentValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }
}
