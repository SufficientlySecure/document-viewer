package org.emdev.common.xml;

public class TextProvider {

    public char[] chars;
    public int size;
    public final boolean persistent;

    public TextProvider(final int size) {
        this.chars = new char[size];
        this.persistent = true;
        this.size = 0;
    }

    public TextProvider(final boolean persistent, final char... text) {
        this.chars = text;
        this.persistent = persistent;
        this.size = this.chars.length;
    }

    public TextProvider(final char[] text, int length) {
        this.chars = text;
        this.persistent = true;
        this.size = length;
    }

    public TextProvider(final String text) {
        this.chars = text.toCharArray();
        this.persistent = true;
        this.size = this.chars.length;
    }

    public int append(final char[] ch, final int start, final int len) {
        if (len < chars.length - size) {
            final int index = size;
            System.arraycopy(ch, start, chars, index, len);
            size += len;
            return index;
        }
        return -1;
    }

    public void recycle() {
        chars = null;
    }
}
