package org.ebookdroid.core.curl;

import org.ebookdroid.core.SinglePageDocumentView;

public enum PageAnimationType {

    NONE,

    CURLER,

    SLIDER;

    public static PageAnimationType get(final String name) {
        for (final PageAnimationType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
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
