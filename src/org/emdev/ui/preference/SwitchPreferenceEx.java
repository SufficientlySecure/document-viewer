package org.emdev.ui.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;

@TargetApi(15)
public class SwitchPreferenceEx extends SwitchPreference {

    private final Listener mListener = new Listener();

    private class Listener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }

            SwitchPreferenceEx.this.setChecked(isChecked);
        }
    }

    public SwitchPreferenceEx(final Context context) {
        super(context);
    }

    public SwitchPreferenceEx(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreferenceEx(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(final View view) {

        final Checkable cview = getCheckableView(view);
        if (cview instanceof CompoundButton) {
            final CompoundButton btn = (CompoundButton) cview;
            btn.setOnCheckedChangeListener(mListener);
        }
        super.onBindView(view);
    }

    protected Checkable getCheckableView(final View view) {
        if (view instanceof Checkable) {
            return (Checkable) view;
        } else if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final View v = group.getChildAt(i);
                if (v instanceof Checkable) {
                    return (Checkable) v;
                }
            }
        }
        return null;
    }
}
