package org.ebookdroid.droids.fb2.codec;

import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_X;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_WIDTH;

import org.ebookdroid.common.settings.AppSettings;

import android.graphics.Bitmap;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.emdev.common.fonts.FontManager;
import org.emdev.common.fonts.SystemFontProvider;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.common.textmarkup.CustomTextPaintContainer;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupEndDocument;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.image.DiskImageData;
import org.emdev.common.textmarkup.image.IImageData;
import org.emdev.common.textmarkup.image.MemoryImageData;
import org.emdev.common.textmarkup.line.Image;
import org.emdev.common.textmarkup.line.LineStream;
import org.emdev.utils.LengthUtils;

public class ParsedContent {

    private static final CustomTextPaintContainer DEFAULT_PAINTS = new CustomTextPaintContainer(null);

    final ArrayList<MarkupElement> docMarkup = new ArrayList<MarkupElement>();

    final HashMap<String, ArrayList<MarkupElement>> streams = new HashMap<String, ArrayList<MarkupElement>>();

    private final TreeMap<String, Image> images = new TreeMap<String, Image>();
    private final TreeMap<String, LineStream> notes = new TreeMap<String, LineStream>();

    private String cover;

    public TypefaceEx[] fonts;
    public TypefaceEx mono;
    public final CustomTextPaintContainer paints;

    public ParsedContent() {
        paints = DEFAULT_PAINTS;
    }

    public ParsedContent(TextPaint defPaint) {
        paints = new CustomTextPaintContainer(defPaint);
    }

    public void loadFonts() {
        final String fontPack = AppSettings.current().fb2FontPack;
        final FontFamilyType family = FontFamilyType.SERIF;
        loadFonts(fontPack, family);
    }

    public void loadFonts(final String fontPack, final FontFamilyType family) {
        final FontStyle[] styles = FontStyle.values();
        fonts = new TypefaceEx[styles.length];
        for (final FontStyle style : styles) {
            final TypefaceEx font = FontManager.getFont(fontPack, family, style);
            fonts[style.ordinal()] = font;
            this.paints.getTextPaint(font, TextStyle.TEXT.getFontSize());
            // System.out.println("Preloaded: " + font);
        }
        mono = FontManager.getFont(SystemFontProvider.SYSTEM_FONT_PACK, FontFamilyType.MONO, FontStyle.REGULAR);
        // System.out.println("Preloaded: " + mono);
    }

    public void clear() {
        docMarkup.clear();
        for (final Entry<String, ArrayList<MarkupElement>> entry : streams.entrySet()) {
            final ArrayList<MarkupElement> value = entry.getValue();
            if (value != null) {
                value.clear();
            }
        }
        streams.clear();
    }

    public void recycle() {
        for (final Image image : images.values()) {
            image.data.recycle();
        }
        images.clear();
    }

    public ArrayList<MarkupElement> getMarkupStream(final String streamName) {
        if (streamName == null) {
            return docMarkup;
        }
        ArrayList<MarkupElement> stream = streams.get(streamName);
        if (stream == null) {
            stream = new ArrayList<MarkupElement>();
            streams.put(streamName, stream);
        }
        return stream;
    }

    public void addImage(final String tmpBinaryName, final String encoded) {
        if (tmpBinaryName != null && encoded != null) {
            IImageData data = new MemoryImageData(encoded);
            if (AppSettings.current().fb2CacheImagesOnDisk) {
                data = new DiskImageData((MemoryImageData) data);
            }
            images.put("I" + tmpBinaryName, new Image(data, true));
            images.put("O" + tmpBinaryName, new Image(data, false));
        }
    }

    public void addImage(final String tmpBinaryName, final char[] tmpBinary, final int tmpBinaryStart,
            final int tmpBinaryLength) {
        if (tmpBinaryName != null && tmpBinary != null && tmpBinaryLength > 0) {
            IImageData data = new MemoryImageData(tmpBinary, tmpBinaryStart, tmpBinaryLength);
            if (AppSettings.current().fb2CacheImagesOnDisk) {
                data = new DiskImageData((MemoryImageData) data);
            }
            images.put("I" + tmpBinaryName, new Image(data, true));
            images.put("O" + tmpBinaryName, new Image(data, false));
        }
    }

    public Image getImage(final String name, final boolean inline) {
        if (name == null) {
            return null;
        }
        Image img = images.get((inline ? "I" : "O") + name);
        if (img == null && name.startsWith("#")) {
            img = images.get((inline ? "I" : "O") + name.substring(1));
        }
        return img;
    }

    public LineStream getNote(final String noteName, boolean hyphenEnabled) {
        LineStream note = notes.get(noteName);
        if (note != null) {
            return note;
        }

        ArrayList<MarkupElement> stream = getMarkupStream(noteName);
        if (LengthUtils.isEmpty(stream) && noteName.startsWith("#")) {
            stream = getMarkupStream(noteName.substring(1));
        }
        if (stream != null) {
            note = createLines(stream, PAGE_WIDTH - 2 * MARGIN_X, JustificationMode.Justify, hyphenEnabled);
            notes.put(noteName, note);
        }

        return note;
    }

    public LineStream createLines(final List<MarkupElement> markup, final int maxLineWidth, final JustificationMode jm,
            final boolean hyphenEnabled) {

        final LineStream lines = new LineStream(this, maxLineWidth, jm, hyphenEnabled);
        if (LengthUtils.isNotEmpty(markup)) {
            for (final MarkupElement me : markup) {
                if (me instanceof MarkupEndDocument) {
                    break;
                }
                me.publishToLines(lines);
            }
        }
        return lines;
    }

    public LineStream getStreamLines(final String streamName, final int maxWidth, final JustificationMode jm,
            final boolean hyphenEnabled) {

        final ArrayList<MarkupElement> stream = getMarkupStream(streamName);
        if (stream != null) {
            return createLines(stream, maxWidth, jm, hyphenEnabled);
        }
        return null;
    }

    public void setCover(final String value) {
        this.cover = value;
    }

    public Bitmap getCoverImage() {
        final Image image = getImage(cover, false);
        if (image != null) {
            return image.data.getBitmap();
        }
        return null;
    }

}
