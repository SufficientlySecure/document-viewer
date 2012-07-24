package org.emdev.utils;

import android.content.Context;
import android.util.AttributeSet;

public final class WidgetUtils {

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public static final String EBOOKDROID_NS = "http://ebookdroid.org";

    public static final String ATTR_JSON_PROPERTY = "jsonProperty";

    public static final String ATTR_FONT_FAMILY = "fontFamily";

    public static final String ATTR_MIN_VALUE = "minValue";
    public static final String ATTR_MAX_VALUE = "maxValue";
    public static final String ATTR_DEFAULT_VALUE = "defaultValue";

    private WidgetUtils() {
    }

    public static String getStringAttribute(final Context context, final AttributeSet attrs, final String namespace,
            final String name, final String defValue) {
        final int resId = attrs.getAttributeResourceValue(namespace, name, Integer.MIN_VALUE);
        if (resId != Integer.MIN_VALUE) {
            return context.getResources().getString(resId);
        }
        return LengthUtils.safeString(attrs.getAttributeValue(namespace, name), defValue);
    }

    public static Boolean getBooleanAttribute(final Context context, final AttributeSet attrs, final String namespace,
            final String name, final Boolean defValue) {
        final int resId = attrs.getAttributeResourceValue(namespace, name, Integer.MIN_VALUE);
        if (resId != Integer.MIN_VALUE) {
            return context.getResources().getBoolean(resId);
        }
        String str = attrs.getAttributeValue(namespace, name);
        return str != null ? Boolean.valueOf(str) : defValue;
    }

    public static int getIntAttribute(final Context context, final AttributeSet attrs, final String namespace,
            final String name, final int defValue) {
        final int resId = attrs.getAttributeResourceValue(namespace, name, Integer.MIN_VALUE);
        if (resId != Integer.MIN_VALUE) {
            final String string = context.getResources().getString(resId);
            try {
                return Integer.parseInt(string);
            } catch (final NumberFormatException e) {
            }
        }
        return attrs.getAttributeIntValue(namespace, name, defValue);
    }

}
