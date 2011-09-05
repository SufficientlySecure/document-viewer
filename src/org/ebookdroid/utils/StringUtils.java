package org.ebookdroid.utils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class StringUtils {

    private StringUtils() {
    }

    public static Set<String> split(final String separator, final String value) {
        final Set<String> list = new LinkedHashSet<String>();

        final String[] split = value.split(separator);
        for (final String string : split) {
            if (LengthUtils.isNotEmpty(string)) {
                list.add(string);
            }
        }
        return list;
    }

    public static String merge(final String separator, final String... items) {
        final StringBuffer result = new StringBuffer();
        for (final String item : items) {
            if (LengthUtils.isNotEmpty(item)) {
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(item);
            }
        }
        return result.toString();
    }

    public static String merge(final String separator, final Collection<String> items) {
        final StringBuffer result = new StringBuffer();
        for (final String item : items) {
            if (LengthUtils.isNotEmpty(item)) {
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(item);
            }
        }
        return result.toString();
    }

}
