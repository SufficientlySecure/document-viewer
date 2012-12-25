package org.emdev.common.textmarkup;

import org.ebookdroid.droids.fb2.codec.ParsedContent;

import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.common.textmarkup.line.TextElement;
import org.emdev.common.xml.TextProvider;

public class RenderingStyle {

    private static final TextProvider DEFIS = new TextProvider("-");
    private static final TextProvider BULLET = new TextProvider("\u2022 ");

    public final CustomTextPaint paint;

    public final TextElement defis;
    public final TextElement bullet;

    public final int textSize;
    public final JustificationMode jm;
    public final TypefaceEx face;
    public final Script script;
    public final Strike strike;

    public RenderingStyle(final ParsedContent content, final TypefaceEx face, final int textSize) {
        this.textSize = textSize;
        this.jm = JustificationMode.Justify;
        this.face = face;
        this.paint = content.paints.getTextPaint(face, this.textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final TextStyle text) {
        this.textSize = text.getFontSize();
        this.jm = JustificationMode.Justify;
        this.face = content.fonts[FontStyle.REGULAR.ordinal()];
        this.paint = content.paints.getTextPaint(face, this.textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final TypefaceEx face, final TextStyle text) {
        this.textSize = text.getFontSize();
        this.jm = JustificationMode.Justify;
        this.face = face;
        this.paint = content.paints.getTextPaint(face, this.textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final Script script) {
        this.textSize = ((script == null) || (script != null && old.script != null)) ? old.textSize
                : old.textSize / 2;
        this.jm = old.jm;
        this.face = old.face;
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = script;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final Strike strike) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = old.face;
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = old.script;
        this.strike = strike;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final TextStyle text, final JustificationMode jm) {
        this.textSize = text.getFontSize();
        this.jm = jm;
        this.face = old.face;
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final TextStyle text, final JustificationMode jm, FontStyle style) {
        this.textSize = text.getFontSize();
        this.jm = jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final JustificationMode jm, FontStyle style) {
        this.textSize = old.textSize;
        this.jm = jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final boolean bold) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = content.fonts[(bold ? old.face.style.getBold() : old.face.style.getBase()).ordinal()];
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public RenderingStyle(final ParsedContent content, final RenderingStyle old, final FontStyle style) {
        this.textSize = old.textSize;
        this.jm = old.jm;
        this.face = content.fonts[style.ordinal()];
        this.paint = content.paints.getTextPaint(face, textSize);
        this.script = null;
        this.strike = null;

        this.defis = new TextElement(DEFIS, this);
        this.bullet = new TextElement(BULLET, this);
    }

    public static CustomTextPaint getTextPaint(final ParsedContent content, final int textSize) {
        TypefaceEx tf = content.fonts[FontStyle.REGULAR.ordinal()];
        return content.paints.getTextPaint(tf, textSize);
    }

    public static enum Script {
        SUB, SUPER;
    }

    public static enum Strike {
        THROUGH, UNDER;
    }
}
