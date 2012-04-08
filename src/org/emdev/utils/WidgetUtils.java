package org.emdev.utils;

import android.content.Context;
import android.util.AttributeSet;


public final class WidgetUtils {

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
