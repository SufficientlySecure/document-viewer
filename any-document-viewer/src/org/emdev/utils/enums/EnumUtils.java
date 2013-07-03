package org.emdev.utils.enums;

import org.emdev.utils.LengthUtils;
import org.json.JSONObject;

public final class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum<E> & ResourceConstant> E getByResValue(final Class<E> enumClass,
            final String resValue, final E defValue) {

        if (LengthUtils.isNotEmpty(resValue)) {
            for (final E item : enumClass.getEnumConstants()) {
                if (item.getResValue().equalsIgnoreCase(resValue)) {
                    return item;
                }
            }
        }
        return defValue;
    }

    public static <E extends Enum<E>> E getByName(final Class<E> enumClass, final String resValue, final E defValue) {

        if (LengthUtils.isNotEmpty(resValue)) {
            for (final E item : enumClass.getEnumConstants()) {
                if (item.name().equalsIgnoreCase(resValue)) {
                    return item;
                }
            }
        }
        return defValue;
    }

    public static <E extends Enum<E>> E getByName(final Class<E> enumClass, final JSONObject object,
            final String property, final E defValue) {
        final String value = object.optString(property);
        return getByName(enumClass, value, defValue);
    }

}
