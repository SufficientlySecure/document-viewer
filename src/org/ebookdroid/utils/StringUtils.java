package org.ebookdroid.utils;

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

    @Deprecated
    public static Comparator<? super String> getNaturalComparator() {
        return NSC;
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

    public static int split(char[] str, int begin, int len, int[] outStart, int[] outLength) {
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
            if (str[i] == 0x20 || str[i] == 0x0D || str[i] == 0x0A || str[i] == 0x09) {
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


    private final static String x = "йьъЙЬЪ";
    private final static String g = "аеёиоуыэюяaeiouyАЕЁИОУЫЭЮЯAEIOUY";
    private final static String s = "бвгджзклмнпрстфхцчшщbcdfghjklmnpqrstvwxzБВГДЖЗКЛМНПРСТФХЦЧШЩBCDFGHJKLMNPQRSTVWXZ";
    private final static HyphenRule[] rules = { new HyphenRule("xgg", 1), new HyphenRule("xgs", 1), new HyphenRule("xsg", 1), new HyphenRule("xss", 1), new HyphenRule("gssssg", 3),
            new HyphenRule("gsssg", 3), new HyphenRule("gsssg", 2), new HyphenRule("sgsg", 2), new HyphenRule("gssg", 2), new HyphenRule("sggg", 2), new HyphenRule("sggs", 2) };


    public static final String[] hyphenateWord(String text) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (x.indexOf(c) != -1) {
                sb.append("x");
            } else if (g.indexOf(c) != -1) {
                sb.append("g");
            } else if (s.indexOf(c) != -1) {
                sb.append("s");
            } else {
                sb.append(c);
            }
        }
        String hyphenatedText = sb.toString();
        for (HyphenRule rule : rules) {
            int index = hyphenatedText.indexOf(rule.pattern);
            while (index != -1) {
                int actualIndex = index + rule.position;
                hyphenatedText = hyphenatedText.substring(0, actualIndex) + "-" + hyphenatedText.substring(actualIndex);
                text = text.substring(0, actualIndex) + "-" + text.substring(actualIndex);
                index = hyphenatedText.indexOf(rule.pattern);
            }
        }
        return text.split("-");
    }

    private final static class HyphenRule {
        public String pattern;
        public int position;

        public HyphenRule(String pattern, int position) {
            this.pattern = pattern;
            this.position = position;
        }
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

}
