package org.ebookdroid.fb2droid.codec;

import android.text.TextPaint;

public class CustomTextPaint extends TextPaint {

    public float spaceSize;

    public void initMeasures() {
        spaceSize = measureText(" ");
    }

}
