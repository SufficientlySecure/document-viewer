package org.ebookdroid.core.codec;

public class CodecPageInfo {

    private int width; /* page width (in pixels) */
    private int height; /* page height (in pixels) */
    protected int dpi; /* page resolution (in dots per inch) */
    protected int rotation; /* initial page orientation */
    protected int version; /* page version */

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }
}
