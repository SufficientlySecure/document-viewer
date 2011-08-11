package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageDocumentView;

public enum PageAnimationType {

    NONE("None", true),

    CURLER("Simple curler", false),

    CURLER_DYNAMIC("Dynamic curler", false),

    SLIDER("Slider", true),

    FADER("Fade in", true),

    SQUEEZER("Squeeze", true);

    /** The _values. */
    private static PageAnimationType[] _values = values();

    /** The resource value. */
    private final String resValue;

    private final boolean hardwareAccelSupported;

    /**
     * Instantiates a new page animation type.
     *
     * @param resValue
     *            the res value
     */
    private PageAnimationType(final String resValue, final boolean hardwareAccelSupported) {
        this.resValue = resValue;
        this.hardwareAccelSupported = hardwareAccelSupported;
    }

    /**
     * Gets the resource value.
     *
     * @return the resource value
     */
    public String getResValue() {
        return resValue;
    }

    public boolean isHardwareAccelSupported() {
        return hardwareAccelSupported;
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
                    return new SinglePageSimpleCurler(singlePageDocumentView);
                case CURLER_DYNAMIC:
                    return new SinglePageDynamicCurler(singlePageDocumentView);
                case SLIDER:
                    return new SinglePageSlider(singlePageDocumentView);
                case FADER:
                    return new SinglePageFader(singlePageDocumentView);
                case SQUEEZER:
                    return new SinglePageSqueezer(singlePageDocumentView);
                default:
                    break;
            }
        }
        return null;
    }
}
