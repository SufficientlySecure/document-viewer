package org.ebookdroid.core;

public enum DecodeMode {
    /**
*
*/
    NATIVE_RESOLUTION("Native"),
    /**
*
*/
    NORMAL("Normal"),
    /**
*
*/
    LOW_MEMORY("Low");

    /** The _values. */
    private static DecodeMode[] _values = values();

    private final String resValue;

    private DecodeMode(final String resValue) {
        this.resValue = resValue;
    }

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
    public static DecodeMode getByResValue(final String resValue) {
        for (final DecodeMode pa : _values) {
            if (pa.resValue.equals(resValue)) {
                return pa;
            }
        }
        return NORMAL;
    }
}
