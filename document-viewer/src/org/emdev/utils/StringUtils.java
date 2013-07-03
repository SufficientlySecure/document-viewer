package org.emdev.utils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class StringUtils {

    public static final NaturalStringComparator NSC = new NaturalStringComparator();

    public static final NaturalFileComparator NFC = new NaturalFileComparator();

    private StringUtils() {
    }

    /**
     * Cleanup title. Remove from title file extension and (...), [...]
     */
    public static String cleanupTitle(final String in) {
        String out = in;
        try {
            out = in.substring(0, in.lastIndexOf('.'));
            out = out.replaceAll("\\(.*?\\)|\\[.*?\\]", "").replaceAll("_", " ").replaceAll(".fb2$", "").trim();
        } catch (final IndexOutOfBoundsException e) {
        }
        return out;
    }

    public static String md5(final String in) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(in.getBytes());
            final byte[] a = digest.digest();
            final int len = a.length;
            final StringBuilder sb = new StringBuilder(len << 1);
            for (int i = 0; i < len; i++) {
                sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(a[i] & 0x0f, 16));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
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

    public static final int compareNatural(String firstString, String secondString) {
        int firstIndex = 0;
        int secondIndex = 0;

        Collator collator = Collator.getInstance();

        while (true) {
            if (firstIndex == firstString.length() && secondIndex == secondString.length()) {
                return 0;
            }
            if (firstIndex == firstString.length()) {
                return -1;
            }
            if (secondIndex == secondString.length()) {
                return 1;
            }

            if (Character.isDigit(firstString.charAt(firstIndex))
                    && Character.isDigit(secondString.charAt(secondIndex))) {
                int firstZeroCount = 0;
                while (firstString.charAt(firstIndex) == '0') {
                    firstZeroCount++;
                    firstIndex++;
                    if (firstIndex == firstString.length()) {
                        break;
                    }
                }
                int secondZeroCount = 0;
                while (secondString.charAt(secondIndex) == '0') {
                    secondZeroCount++;
                    secondIndex++;
                    if (secondIndex == secondString.length()) {
                        break;
                    }
                }
                if ((firstIndex == firstString.length() || !Character.isDigit(firstString.charAt(firstIndex)))
                        && (secondIndex == secondString.length() || !Character
                                .isDigit(secondString.charAt(secondIndex)))) {
                    continue;
                }
                if ((firstIndex == firstString.length() || !Character.isDigit(firstString.charAt(firstIndex)))
                        && !(secondIndex == secondString.length() || !Character.isDigit(secondString
                                .charAt(secondIndex)))) {
                    return -1;
                }
                if ((secondIndex == secondString.length() || !Character.isDigit(secondString.charAt(secondIndex)))) {
                    return 1;
                }

                int diff = 0;
                do {
                    if (diff == 0) {
                        diff = firstString.charAt(firstIndex) - secondString.charAt(secondIndex);
                    }
                    firstIndex++;
                    secondIndex++;
                    if (firstIndex == firstString.length() && secondIndex == secondString.length()) {
                        return diff != 0 ? diff : firstZeroCount - secondZeroCount;
                    }
                    if (firstIndex == firstString.length()) {
                        if (diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(secondString.charAt(secondIndex)) ? -1 : diff;
                    }
                    if (secondIndex == secondString.length()) {
                        if (diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(firstString.charAt(firstIndex)) ? 1 : diff;
                    }
                    if (!Character.isDigit(firstString.charAt(firstIndex))
                            && !Character.isDigit(secondString.charAt(secondIndex))) {
                        if (diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if (!Character.isDigit(firstString.charAt(firstIndex))) {
                        return -1;
                    }
                    if (!Character.isDigit(secondString.charAt(secondIndex))) {
                        return 1;
                    }
                } while (true);
            } else {
                int aw = firstIndex;
                int bw = secondIndex;
                do {
                    firstIndex++;
                } while (firstIndex < firstString.length() && !Character.isDigit(firstString.charAt(firstIndex)));
                do {
                    secondIndex++;
                } while (secondIndex < secondString.length() && !Character.isDigit(secondString.charAt(secondIndex)));

                String as = firstString.substring(aw, firstIndex);
                String bs = secondString.substring(bw, secondIndex);
                int subwordResult = collator.compare(as, bs);
                if (subwordResult != 0) {
                    return subwordResult;
                }
            }
        }
    }

    @Deprecated
    public static Comparator<? super File> getNaturalFileComparator() {
        return NFC;
    }

    public static int split(char[] str, int begin, int len, int[] outStart, int[] outLength, boolean lineBreaksOnly) {
        if (str == null) {
            return 0;
        }
        if (len == 0) {
            return 0;
        }
        int i = begin, start = begin;
        int index = 0;
        boolean match = false;
        while (i < begin + len) {
            if ((lineBreaksOnly && (str[i] == 0x0D || str[i] == 0x0A)) || (!lineBreaksOnly && (str[i] == 0x20 || str[i] == 0x0D || str[i] == 0x0A || str[i] == 0x09))) {
                if (match) {
                    outStart[index] = start;
                    outLength[index] = i - start;
                    index++;
                    match = false;
                }
                start = ++i;
                continue;
            }
            match = true;
            i++;
        }
        if (match) {
            outStart[index] = start;
            outLength[index] = i - start;
            index++;
        }
        return index;
    }

    public static final class NaturalStringComparator implements Comparator<String> {

        public int compare(String o1, String o2) {
            return compareNatural(o1, o2);
        }
    }

    public static final class NaturalFileComparator implements Comparator<File> {

        public int compare(File o1, File o2) {
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            return compareNatural(o1.getAbsolutePath(), o2.getAbsolutePath());
        }
    }


    public static int parseInt(char[]string, int start, int length, int radix) throws NumberFormatException {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("Invalid radix: " + radix);
        }
        if (string == null) {
            throw new NumberFormatException(new String(string, start, length));
        }
        int i = 0;
        if (length == 0) {
            throw new NumberFormatException(new String(string, start, length));
        }
        boolean negative = string[start+i] == '-';
        if (negative && ++i == length) {
            throw new NumberFormatException(new String(string, start, length));
        }

        return parse(string, start, length, i, radix, negative);
    }

    private static int parse(char[]string, int start, int length, int offset, int radix, boolean negative) throws NumberFormatException {
        int max = Integer.MIN_VALUE / radix;
        int result = 0;
        while (offset < length) {
            int digit = Character.digit(string[start+(offset++)], radix);
            if (digit == -1) {
                throw new NumberFormatException(new String(string, start, length));
            }
            if (max > result) {
                throw new NumberFormatException(new String(string, start, length));
            }
            int next = result * radix - digit;
            if (next > result) {
                throw new NumberFormatException(new String(string, start, length));
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw new NumberFormatException(new String(string, start, length));
            }
        }
        return result;
    }

}
