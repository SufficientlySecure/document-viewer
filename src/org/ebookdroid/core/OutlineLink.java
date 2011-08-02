package org.ebookdroid.core;

public class OutlineLink implements CharSequence {

    private final String title;
    private final String link;

    public OutlineLink(final String t, final String l) {
        title = t;
        link = l;
    }

    @Override
    public char charAt(final int index) {
        return title.charAt(index);
    }

    @Override
    public int length() {
        return title.length();
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return title.subSequence(start, end);
    }

    @Override
    public String toString() {
        return title;
    }

    public String getLink() {
        return link;
    }

}
