package org.ebookdroid.droids.fb2.codec;

import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_X;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_WIDTH;

import org.ebookdroid.common.settings.AppSettings;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.emdev.common.fonts.FontManager;
import org.emdev.common.fonts.data.FontFamilyType;
import org.emdev.common.fonts.data.FontStyle;
import org.emdev.common.fonts.typeface.TypefaceEx;
import org.emdev.common.textmarkup.JustificationMode;
import org.emdev.common.textmarkup.MarkupElement;
import org.emdev.common.textmarkup.MarkupEndDocument;
import org.emdev.common.textmarkup.RenderingStyle;
import org.emdev.common.textmarkup.TextStyle;
import org.emdev.common.textmarkup.Words;
import org.emdev.common.textmarkup.image.DiskImageData;
import org.emdev.common.textmarkup.image.IImageData;
import org.emdev.common.textmarkup.image.MemoryImageData;
import org.emdev.common.textmarkup.line.Image;
import org.emdev.common.textmarkup.line.Line;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.collections.SparseArrayEx;

public class ParsedContent {

    final ArrayList<MarkupElement> docMarkup = new ArrayList<MarkupElement>();

    final HashMap<String, ArrayList<MarkupElement>> streams = new HashMap<String, ArrayList<MarkupElement>>();

    private final TreeMap<String, Image> images = new TreeMap<String, Image>();
    private final TreeMap<String, ArrayList<Line>> notes = new TreeMap<String, ArrayList<Line>>();

    public final SparseArrayEx<Words> words = new SparseArrayEx<Words>();

    private String cover;

    public TypefaceEx[] fonts;

    public void loadFonts() {
        final FontStyle[] styles = FontStyle.values();
        fonts = new TypefaceEx[styles.length];
        for (final FontStyle style : styles) {
            final TypefaceEx font = FontManager.getFont(AppSettings.current().fb2FontPack, FontFamilyType.SERIF, style);
            System.out.println("Preloaded: " + font);
            fonts[style.ordinal()] = font;
            RenderingStyle.getTextPaint(font, TextStyle.TEXT.getFontSize());
        }
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
        for (final Words w : words) {
            w.recycle();
        }
        words.clear();
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

    public List<Line> getNote(final String noteName) {
        ArrayList<Line> note = notes.get(noteName);
        if (note != null) {
            return note;
        }

        ArrayList<MarkupElement> stream = getMarkupStream(noteName);
        if (LengthUtils.isEmpty(stream) && noteName.startsWith("#")) {
            stream = getMarkupStream(noteName.substring(1));
        }
        if (stream != null) {
            note = createLines(stream, PAGE_WIDTH - 2 * MARGIN_X, JustificationMode.Justify);
            notes.put(noteName, note);
        }

        return note;
    }

    ArrayList<Line> createLines(final List<MarkupElement> markup, final int maxLineWidth, final JustificationMode jm) {
        final ArrayList<Line> lines = new ArrayList<Line>();
        if (LengthUtils.isNotEmpty(markup)) {
            final LineCreationParams params = new LineCreationParams();
            params.jm = jm;
            params.maxLineWidth = maxLineWidth;
            params.content = this;
            for (final MarkupElement me : markup) {
                if (me instanceof MarkupEndDocument) {
                    break;
                }
                me.publishToLines(lines, params);
            }
        }
        return lines;
    }

    public List<Line> getStreamLines(final String streamName, final int maxWidth, final JustificationMode jm) {
        final ArrayList<MarkupElement> stream = getMarkupStream(streamName);
        if (stream != null) {
            return createLines(stream, maxWidth, jm);
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
