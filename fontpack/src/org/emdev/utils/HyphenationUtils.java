package org.emdev.utils;

import android.util.SparseArray;

import java.util.Arrays;
import java.util.LinkedList;

public class HyphenationUtils {

    private static final String x = "йьъЙЬЪ";
    private static final String g = "аеёиоуыэюяaeiouyАЕЁИОУЫЭЮЯAEIOUY";
    private static final String s = "бвгджзклмнпрстфхцчшщbcdfghjklmnpqrstvwxzБВГДЖЗКЛМНПРСТФХЦЧШЩBCDFGHJKLMNPQRSTVWXZ";

    private static final SparseArray<Symbol> classes = new SparseArray<Symbol>();

    private static final HyphenRuleNode ROOT = new HyphenRuleNode(Symbol.N);

    static {
        for (final char c : x.toCharArray()) {
            classes.append(c, Symbol.X);
        }
        for (final char c : g.toCharArray()) {
            classes.append(c, Symbol.G);
        }
        for (final char c : s.toCharArray()) {
            classes.append(c, Symbol.S);
        }

        ROOT.addRule(1, Symbol.X, Symbol.G, Symbol.G);
        ROOT.addRule(1, Symbol.X, Symbol.G, Symbol.S);
        ROOT.addRule(1, Symbol.X, Symbol.S, Symbol.G);
        ROOT.addRule(1, Symbol.X, Symbol.S, Symbol.S);
        ROOT.addRule(2, Symbol.G, Symbol.S, Symbol.S, Symbol.G);
        ROOT.addRule(2, Symbol.G, Symbol.S, Symbol.S, Symbol.S, Symbol.G);
        ROOT.addRule(3, Symbol.G, Symbol.S, Symbol.S, Symbol.S, Symbol.G);
        ROOT.addRule(3, Symbol.G, Symbol.S, Symbol.S, Symbol.S, Symbol.S, Symbol.G);
        ROOT.addRule(2, Symbol.S, Symbol.G, Symbol.G, Symbol.G);
        ROOT.addRule(2, Symbol.S, Symbol.G, Symbol.G, Symbol.S);
        ROOT.addRule(2, Symbol.S, Symbol.G, Symbol.S, Symbol.G);
    }

    private HyphenationUtils() {
    }

    private static final Symbol[] buffer = new Symbol[10];

    public static final int hyphenateWord(final char[] str, final int begin, final int len, final int[] outStart,
            final int[] outLength) {
        if (str == null) {
            return 0;
        }
        if (len == 0) {
            return 0;
        }

        int bufstart = 0;
        int bufcount = 0;
        int partcount = 0;

        // System.out.println("Full word: " + new String(str, begin, len));

        int nextStart = begin;

        for (int i = begin, end = begin + len; i < end; i++) {
            final char c = str[i];
            final Symbol clazz = classes.get(c, Symbol.N);
            if (clazz == Symbol.N) {
                bufcount = 0;
                bufstart = 0;
                continue;
            }

            final int index = (bufstart + bufcount) % buffer.length;
            buffer[index] = clazz;
            bufcount++;

            HyphenRuleNode current = null;
            while (bufcount > 0) {
                current = match(current, bufstart, index, bufcount);
                if (current != null) {
                    if (!current.rulez.isEmpty()) {
                        // Set hyphens from roots
                        int prevRulePosition = 0;
                        int totalUsed = 0;
                        for (final HyphenRule r : current.rulez) {
                            final int thisStart = nextStart;
                            final int ruleStart = prevRulePosition == 0 ? i - bufcount + 1 : nextStart;
                            final int headLen = ruleStart - thisStart;
                            final int used = r.position - prevRulePosition;

                            outStart[partcount] = thisStart;
                            outLength[partcount] = headLen + used;

                            prevRulePosition = r.position;
                            nextStart = outStart[partcount] + outLength[partcount];
                            totalUsed += used;

                            // System.out.println("Part: " + new String(str, outStart[partcount], outLength[partcount]));

                            partcount++;
                        }
                        // Clear buffer
                        bufcount = bufcount - totalUsed;
                        bufstart = (bufstart + totalUsed) % buffer.length;
                        current = null;
                    } else {
                        // Partial match - do nothing
                        break;
                    }
                } else {
                    // No matches - remove first symbol from buffer
                    bufcount = bufcount - 1;
                    bufstart = bufcount == 0 ? 0 : (bufstart + 1) % buffer.length;
                    current = null;
                }
            }
        }

        if (partcount > 0) {
            final int lastStart = outStart[partcount - 1];
            final int lastLen = outLength[partcount - 1];
            final int tailStart = lastStart + lastLen;
            final int tailLen = begin + len - tailStart;
            if (tailLen > 0) {
                outStart[partcount] = tailStart;
                outLength[partcount] = tailLen;
                // System.out.println("Part: " + new String(str, outStart[partcount], outLength[partcount]));
                partcount++;
            }
        }

        return partcount;
    }

    private static HyphenRuleNode match(final HyphenRuleNode current, final int bufstart, final int index,
            final int bufcount) {
        if (current != null) {
            return current.children[buffer[index].ordinal()];
        }
        if (bufcount == 1) {
            return ROOT.children[buffer[index].ordinal()];
        }
        HyphenRuleNode c = ROOT;
        for (int symIndex = 0; symIndex < bufcount && c != null; symIndex++) {
            final int ordinal = buffer[(bufstart + symIndex) % buffer.length].ordinal();
            c = c.children[ordinal];
        }
        return c;
    }

    private static enum Symbol {
        X, S, G, N;

        public static final Symbol[] symbols = values();
    }

    private static final class HyphenRule {

        public final Symbol[] pattern;
        public final int position;

        public HyphenRule(final int position, final Symbol... pattern) {
            this.pattern = pattern;
            this.position = position;
        }

        @Override
        public String toString() {
            return pattern + " : " + position;
        }
    }

    private static final class HyphenRuleNode {

        final Symbol symbol;
        final HyphenRuleNode[] children = new HyphenRuleNode[Symbol.symbols.length];
        final LinkedList<HyphenRule> rulez = new LinkedList<HyphenRule>();

        HyphenRuleNode(final Symbol symbol) {
            this.symbol = symbol;
        }

        void addRule(final int position, final Symbol... pattern) {
            addRule(new HyphenRule(position, pattern), 0);
        }

        void addRule(final HyphenRule rule, final int level) {
            if (level >= rule.pattern.length) {
                rulez.add(rule);
                return;
            }

            final Symbol nextSymbol = rule.pattern[level];
            final int childIndex = nextSymbol.ordinal();

            HyphenRuleNode child = children[childIndex];
            if (child == null) {
                child = new HyphenRuleNode(nextSymbol);
                children[childIndex] = child;
            }
            child.addRule(rule, level + 1);
        }

        @Override
        public String toString() {
            return symbol + ":" + Arrays.toString(children) + " " + rulez;
        }
    }

}
