package org.ebookdroid.fb2droid.codec;

import org.ebookdroid.EBookDroidApp;

import android.graphics.Typeface;
import android.util.SparseArray;

class RenderingStyle {

    public static final Typeface NORMAL_TF = Typeface.createFromAsset(EBookDroidApp.getAppContext().getAssets(),
            "fonts/academy.ttf");
    public static final Typeface ITALIC_TF = Typeface.createFromAsset(EBookDroidApp.getAppContext().getAssets(),
            "fonts/academyi.ttf");

    public static final int TEXT_SIZE = 24;
    public static final int MAIN_TITLE_SIZE = 2 * TEXT_SIZE;
    public static final int SECTION_TITLE_SIZE = (3 * TEXT_SIZE) / 2;
    public static final int SUBTITLE_SIZE = (5 * TEXT_SIZE) / 4;
    public static final int FOOTNOTE_SIZE = (5 * TEXT_SIZE) / 6;

    private static final SparseArray<CustomTextPaint> paints = new SparseArray<CustomTextPaint>();
    private static final SparseArray<RenderingStyle> styles = new SparseArray<RenderingStyle>();

    final int textSize;
    final JustificationMode jm;
    final boolean bold;
    final Typeface face;
    final CustomTextPaint paint;
    final boolean superScript;
    final boolean subScript;

    public RenderingStyle(final int textSize) {
        this.textSize = textSize;
        this.jm = JustificationMode.Justify;
        this.bold = false;
        this.face = RenderingStyle.NORMAL_TF;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.bold = old.bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final int textSize) {
        this.textSize = textSize;
        this.jm = old.jm;
        this.bold = old.bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final int textSize, final boolean superScript,
            final boolean subScript) {
        this.textSize = textSize;
        this.jm = old.jm;
        this.bold = old.bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = superScript;
        this.subScript = subScript;
    }

    public RenderingStyle(final RenderingStyle old, final int textSize, final JustificationMode jm) {
        this.textSize = textSize;
        this.jm = jm;
        this.bold = old.bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final int textSize, final JustificationMode jm, final boolean bold,
            final Typeface face) {
        this.textSize = textSize;
        this.jm = jm;
        this.bold = bold;
        this.face = face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final JustificationMode jm) {
        this.textSize = old.textSize;
        this.jm = jm;
        this.bold = old.bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final JustificationMode jm, final Typeface face) {
        this.textSize = old.textSize;
        this.jm = jm;
        this.bold = old.bold;
        this.face = face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final boolean bold) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.bold = bold;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final RenderingStyle old, final Typeface face) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.bold = old.bold;
        this.face = face;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public RenderingStyle(final int textSize, final boolean bold, final boolean italic) {
        this.textSize = textSize;
        this.jm = JustificationMode.Justify;
        this.bold = bold;
        this.face = italic ? ITALIC_TF : NORMAL_TF;
        this.paint = getTextPaint(face, textSize, bold);
        this.superScript = false;
        this.subScript = false;
    }

    public CustomTextPaint getTextPaint() {
        return paint;
    }

    public static CustomTextPaint getTextPaint(final int textSize) {
        return getTextPaint(NORMAL_TF, textSize, false);
    }

    private static final CustomTextPaint getTextPaint(final Typeface face, final int textSize, final boolean bold) {
        final int key = (textSize & 0x0FFF) + (face == ITALIC_TF ? 1 << 14 : 0) + (bold ? 1 << 15 : 0);
        CustomTextPaint paint = paints.get(key);
        if (paint == null) {
            paint = new CustomTextPaint();
            paint.setTextSize(textSize);
            paint.setTypeface(face);
            paint.setFakeBoldText(bold);
            paint.setAntiAlias(true);
            paint.initMeasures();
            paints.append(key, paint);
        }
        return paint;
    }

    public static RenderingStyle getStyle(final int textSize, final boolean bold, final boolean italic) {
        final int key = (textSize & 0x0FFF) + (italic ? 1 << 14 : 0) + (bold ? 1 << 15 : 0);
        RenderingStyle style = styles.get(key);
        if (style == null) {
            style = new RenderingStyle(textSize, bold, italic);

            styles.append(key, style);
        }
        return style;
    }
}
