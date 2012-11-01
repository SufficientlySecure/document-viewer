package org.emdev.common.textmarkup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.emdev.common.textmarkup.line.TextElement;
import org.emdev.common.xml.TextProvider;

public class Words {

    public static int words = 0;
    public static int uniques = 0;

    static final FB2Word key = new FB2Word();

    final Map<FB2Word, TextElement> all = new HashMap<FB2Word, TextElement>(32 * 1024);

    final LinkedList<TextProvider> buffers = new LinkedList<TextProvider>();

    public TextElement get(final TextProvider text, final int st, final int len, final RenderingStyle style) {
        words++;

        key.reuse(text.chars, st, len);

        TextElement e = all.get(key);
        if (e == null) {
            char[] arr = null;
            int index = 0;
            if (text.persistent) {
                arr = text.chars;
                index = st;
            } else {
                if (!buffers.isEmpty()) {
                    TextProvider b = buffers.getFirst();
                    index = b.append(text.chars, st, len);
                    if (index != -1) {
                        arr = b.chars;
                    }
                }
                if (arr == null) {
                    TextProvider b = new TextProvider(4 * 1024);
                    index = b.append(text.chars, st, len);
                    arr = b.chars;
                    buffers.addFirst(b);
                }
            }

            e = new TextElement(arr, index, len, style);
            all.put(new FB2Word(arr, index, len, key.hash), e);
            uniques++;
        }
        return e;
    }

    public void recycle() {
        all.clear();

        for (TextProvider b : buffers) {
            b.recycle();
        }
        buffers.clear();
    }

    public static void clear() {
        words = 0;
        uniques = 0;
    }

    static class FB2Word {

        int hash;
        char[] chars;
        int start;
        int length;

        public FB2Word() {
        }

        public FB2Word(final char[] ch, final int st, final int len, final int hash) {
            this.chars = ch;
            this.hash = hash;
            this.length = len;
            this.start = st;
        }

        public void reuse(final char[] ch, final int st, final int len) {
            this.chars = ch;
            this.start = st;
            this.length = len;

            int h = 0;
            int off = st;
            for (int i = 0; i < len; i++) {
                h = 31 * h + ch[off++];
            }
            hash = h;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof FB2Word) {
                final FB2Word that = (FB2Word) obj;
                if (this.hash != that.hash) {
                    return false;
                }
                if (this.length != that.length) {
                    return false;
                }
                for (int i = 0; i < this.length; i++) {
                    final char c1 = this.chars[i + this.start];
                    final char c2 = that.chars[i + that.start];
                    if (c1 != c2) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
