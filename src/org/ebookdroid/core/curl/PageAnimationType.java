package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageDocumentView;

public enum PageAnimationType {

    NONE("None"),

    CURLER("Curler"),

    SLIDER("Slider");

    /** The resource value. */
    private final String resValue;

    /** The _values. */
    private static PageAnimationType[] _values = values();

    /**
     * Instantiates a new page animation type.
     *
     * @param resValue
     *            the res value
     */
    private PageAnimationType(final String resValue) {
        this.resValue = resValue;
    }

    /**
     * Gets the resource value.
     *
     * @return the resource value
     */
    public String getResValue() {
        return resValue;
    }

    public static PageAnimationType get(final String resValue) {
        for (final PageAnimationType t : _values) {
            if (t.getResValue().equalsIgnoreCase(resValue)) {
                return t;
            }
        }
        return NONE;
    }

    public static PageAnimator create(final PageAnimationType type, final SinglePageDocumentView singlePageDocumentView) {
        if (type != null) {
            switch (type) {
                case CURLER:
                    return new SinglePageCurler(singlePageDocumentView);
                case SLIDER:
                    return new SinglePageSlider(singlePageDocumentView);
                default:
                    break;
            }
        }
        return null;
    }
}
