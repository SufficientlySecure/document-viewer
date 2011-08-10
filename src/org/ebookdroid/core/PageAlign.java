package org.ebookdroid.core;

/**
 * The Enum PageAlign.
 */
public enum PageAlign {
    /** BY WIDTH. */
    WIDTH("By Width"),
    /** BY HEIGHT. */
    HEIGHT("By Height"),
    /** AUTO. */
    AUTO("Auto");

    /** The resource value. */
    private final String resValue;

    /** The _values. */
    private static PageAlign[] _values = values();

    /**
     * Instantiates a new page align object.
     *
     * @param resValue
     *            the res value
     */
    private PageAlign(final String resValue) {
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

    /**
     * Gets the by resource value.
     *
     * @param resValue
     *            the resource value
     * @return the enum value or @null
     */
    public static PageAlign getByResValue(final String resValue) {
        for (final PageAlign pa : _values) {
            if (pa.resValue.equals(resValue)) {
                return pa;
            }
        }
        return null;
    }

}
