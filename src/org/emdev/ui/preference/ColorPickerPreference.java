/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emdev.ui.preference;

import org.ebookdroid.R;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.afzkl.colorpicker.views.ColorPanelView;
import org.afzkl.colorpicker.views.ColorPickerView;
import org.afzkl.colorpicker.views.ColorPickerView.OnColorChangedListener;
import org.emdev.utils.LengthUtils;

public class ColorPickerPreference extends DialogPreference implements ColorPickerView.OnColorChangedListener {

    private ColorPickerView mColorPicker;

    private ColorPanelView mOldColor;
    private ColorPanelView mNewColor;

    private OnColorChangedListener mListener;

    private int color;

    public ColorPickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.color_picker_preference_widget);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        readColor(defaultValue);
    }

    private void readColor(final Object defaultValue) {
        try {
            color = Integer.parseInt(getPersistedString(LengthUtils.toString(defaultValue)));
        } catch (final Exception ex) {
            color = 0x00FFFFFF;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View v = view.findViewById(R.id.color_picke_preference_view);
        if (v != null) {
            Drawable background = v.getBackground();
            if (background instanceof GradientDrawable) {
                GradientDrawable grad = (GradientDrawable) background;
                grad.setColor(color);
            }
        }
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_color_picker, null);
        readColor(0x00FFFFFF);
        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mOldColor = (ColorPanelView) layout.findViewById(R.id.old_color_panel);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.new_color_panel);

        ((LinearLayout) mOldColor.getParent()).setPadding(Math.round(mColorPicker.getDrawingOffset()), 0,
                Math.round(mColorPicker.getDrawingOffset()), 0);

        mColorPicker.setOnColorChangedListener(this);

        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);
        setAlphaSliderVisible(true);
        return layout;
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            color = getColor();
            final String value = Integer.toString(color);
            if (callChangeListener(value)) {
                if (shouldPersist()) {
                    persistString(value);
                }
                notifyChanged();
            }
        }
    }

    @Override
    public void onColorChanged(final int color) {

        mNewColor.setColor(color);

        if (mListener != null) {
            mListener.onColorChanged(color);
        }

    }

    public void setAlphaSliderVisible(final boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

}
