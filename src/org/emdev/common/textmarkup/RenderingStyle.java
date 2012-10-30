package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.ParsedContent;

import android.util.SparseArray;

import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.common.textmarkup.line.TextElement;

public class RenderingStyle {

    private static final SparseArray<CustomTextPaint> paints = new SparseArray<CustomTextPaint>();

    public final CustomTextPaint paint;

    public final TextElement defis;

    public final int textSize;
    public final JustificationMode jm;
    public final TypefaceEx face;
    public final Script script;
    public final Strike strike;

    public RenderingStyle(final ParsedContent content, final TextStyle text) {
        this.textSize = text.getFontSize();
        this.jm = JustificationMode.Justify;
        this.face = content.fonts[FontStyle.REGULAR.ordinal()];
        this.paint = getTextPaint(face, this.textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final ParsedContent content, final TypefaceEx face, final TextStyle text) {
        this.textSize = text.getFontSize();
        this.jm = JustificationMode.Justify;
        this.face = face;
        this.paint = getTextPaint(face, this.textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final RenderingStyle old, final Script script) {
        this.textSize = ((script == null) || (script != null && old.script != null)) ? old.textSize
                : old.textSize / 2;
        this.jm = old.jm;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize);
        this.script = script;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final RenderingStyle old, final Strike strike) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize);
        this.script = old.script;
        this.strike = strike;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final RenderingStyle old, final TextStyle text, final JustificationMode jm) {
        this.textSize = text.getFontSize();
        this.jm = jm;
        this.face = old.face;
        this.paint = getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final TextStyle text, final JustificationMode jm, FontStyle style) {
        this.textSize = text.getFontSize();
        this.jm = jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final JustificationMode jm, FontStyle style) {
        this.textSize = old.textSize;
        this.jm = jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final boolean bold) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = content.fonts[(bold ? old.face.style.getBold() : old.face.style.getBase()).ordinal()];
        this.paint = getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final FontStyle style) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(new char[] { '-' }, 0, 1, this);
    }

    public static CustomTextPaint getTextPaint(final ParsedContent content, final int textSize) {
        TypefaceEx tf = content.fonts[FontStyle.REGULAR.ordinal()];
        return getTextPaint(tf, textSize);
    }

    public static final CustomTextPaint getTextPaint(final TypefaceEx face, final int textSize) {
        final int key = (face.id & 0x0000FFFF) + ((textSize & 0x0000FFFF) << 16);
        CustomTextPaint paint = paints.get(key);
        if (paint == null) {
            paint = new CustomTextPaint(key, face, textSize);
            paints.append(key, paint);
        }
        return paint;
    }

    public static enum Script {
        SUB, SUPER;
    }

    public static enum Strike {
        THROUGH, UNDER;
    }
}
