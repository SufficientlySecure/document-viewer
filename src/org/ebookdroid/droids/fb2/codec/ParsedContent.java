package org.ebookdroid.droids.fb2.codec;

import static org.ebookdroid.droids.fb2.codec.FB2Page.MARGIN_X;
import static org.ebookdroid.droids.fb2.codec.FB2Page.PAGE_WIDTH;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.textmarkup.MarkupEndDocument;
import org.emdev.utils.textmarkup.JustificationMode;
import org.emdev.utils.textmarkup.MarkupElement;
import org.emdev.utils.textmarkup.line.Image;
import org.emdev.utils.textmarkup.line.Line;

public class ParsedContent {

    final ArrayList<MarkupElement> docMarkup = new ArrayList<MarkupElement>();

    final HashMap<String, ArrayList<MarkupElement>> streams = new HashMap<String, ArrayList<MarkupElement>>();

    private final TreeMap<String, Image> images = new TreeMap<String, Image>();
    private final TreeMap<String, ArrayList<Line>> notes = new TreeMap<String, ArrayList<Line>>();

    private String cover;

    public void clear() {
        docMarkup.clear();
        for (Entry<String, ArrayList<MarkupElement>> entry : streams.entrySet()) {
            final ArrayList<MarkupElement> value = entry.getValue();
            if (value != null) {
                value.clear();
            }
        }
    }

    public ArrayList<MarkupElement> getMarkupStream(String streamName) {
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
            images.put("I" + tmpBinaryName, new Image(encoded, true));
            images.put("O" + tmpBinaryName, new Image(encoded, false));
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

    ArrayList<Line> createLines(List<MarkupElement> markup, int maxLineWidth, JustificationMode jm) {
        ArrayList<Line> lines = new ArrayList<Line>();
        if (LengthUtils.isNotEmpty(markup)) {
            LineCreationParams params = new LineCreationParams();
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

    public List<Line> getStreamLines(String streamName, int maxWidth, JustificationMode jm) {
        ArrayList<MarkupElement> stream = getMarkupStream(streamName);
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
            final byte[] data = image.getData();
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }


}
