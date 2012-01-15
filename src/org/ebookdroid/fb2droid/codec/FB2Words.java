package org.ebookdroid.fb2droid.codec;

import java.nio.CharBuffer;
import java.util.LinkedList;

public class FB2Words {

    public static int words = 0;
    public static int uniques = 0;

    static final FB2Word key = new FB2Word();

    final LinkedList<CharBuffer> buffers = new LinkedList<CharBuffer>();

    public FB2TextElement get(final char[] ch, final int st, final int len, final RenderingStyle style) {
        words++;

        CharBuffer buf = null;
        if (!buffers.isEmpty()) {
            buf = buffers.getFirst();
            if (len > buf.remaining()) {
                buffers.removeFirst();
                buf = null;
            }
        }
        if (buf == null) {
            buf = CharBuffer.allocate(64 * 1024);
            buffers.add(buf);
        }
        int pos = buf.position();
        buf.put(ch, st, len);
        buf.position(pos);
        CharSequence word = buf.subSequence(0, len);
        buf.position(pos + len);

        FB2TextElement e = new FB2TextElement(word, style.paint.measureText(ch, st, len), style);

        uniques++;
        return e;
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
