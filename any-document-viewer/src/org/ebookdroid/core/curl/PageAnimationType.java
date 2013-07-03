package org.ebookdroid.core.curl;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.R;
import org.ebookdroid.core.SinglePageController;

import org.emdev.utils.enums.ResourceConstant;

public enum PageAnimationType implements ResourceConstant {

    NONE(R.string.pref_animation_type_none, true),

    CURLER(R.string.pref_animation_type_curler_simple, false),

    CURLER_DYNAMIC(R.string.pref_animation_type_curler_dynamic, false),

    SLIDER(R.string.pref_animation_type_slider, true),

    SLIDER2(R.string.pref_animation_type_slider2, true),

    FADER(R.string.pref_animation_type_fader, true),

    SQUEEZER(R.string.pref_animation_type_squeezer, true);

    /** The resource value. */
    private final String resValue;

    private final boolean hardwareAccelSupported;

    /**
     * Instantiates a new page animation type.
     *
     * @param resValue
     *            the res value
     */
    private PageAnimationType(final int resId, final boolean hardwareAccelSupported) {
        this.resValue = EBookDroidApp.context.getString(resId);
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

    public static PageAnimator create(final PageAnimationType type, final SinglePageController singlePageDocumentView) {
        if (type != null) {
            switch (type) {
                case CURLER:
                    return new SinglePageSimpleCurler(singlePageDocumentView);
                case CURLER_DYNAMIC:
                  return new SinglePageDynamicCurler(singlePageDocumentView);
                case SLIDER:
                    return new SinglePageSlider(singlePageDocumentView);
                case SLIDER2:
                    return new SinglePageSlider2(singlePageDocumentView);
                case FADER:
                    return new SinglePageFader(singlePageDocumentView);
                case SQUEEZER:
                    return new SinglePageSqueezer(singlePageDocumentView);
                default:
                    break;
            }
        }
        return new SinglePageDefaultSlider(singlePageDocumentView);
    }
}
