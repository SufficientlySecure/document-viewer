package org.ebookdroid.core;

public class OutlineLink implements CharSequence {

    public final String title;
    public final String link;
    public final int level;
    
    public OutlineLink(final String title, final String link, final int level) {
        this.title = title;
        this.level = level;
        this.link = link;
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
