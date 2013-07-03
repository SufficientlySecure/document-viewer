package org.emdev.common.textmarkup;

import android.text.TextPaint;
import android.util.SparseArray;

import org.emdev.common.fonts.typeface.TypefaceEx;

public class CustomTextPaintContainer {

    private final SparseArray<CustomTextPaint> paints = new SparseArray<CustomTextPaint>();

    private final TextPaint defPaint;

    public CustomTextPaintContainer(final TextPaint defPaint) {
        this.defPaint = defPaint;
    }

    public final CustomTextPaint getTextPaint(final TypefaceEx face, final int textSize) {
        final int key = (face.id & 0x0000FFFF) + ((textSize & 0x0000FFFF) << 16);
        CustomTextPaint paint = paints.get(key);
        if (paint == null) {
            paint = new CustomTextPaint(key, face, textSize);
            if (defPaint == null) {
                paint = new CustomTextPaint(key, face, textSize);
            } else {
                paint = new CustomTextPaint(defPaint, key, face, textSize);
            }
            paints.append(key, paint);
        }
        return paint;
    }

}
