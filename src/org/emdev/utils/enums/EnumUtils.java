package org.emdev.utils.enums;

import org.emdev.utils.LengthUtils;

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
}
