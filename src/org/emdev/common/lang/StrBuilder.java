package org.emdev.common.lang;

import java.util.Arrays;

/**
 * The Class StrBuilder.
 *
 * Copy of some methods from java.lang.StringBuilder with access to internal buffer.
 */
public class StrBuilder {

    static final int INITIAL_CAPACITY = 16;

    private char[] value;

    private int count;

    private boolean shared;

    public StrBuilder() {
        value = new char[INITIAL_CAPACITY];
    }

    public StrBuilder(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException();
        }
        value = new char[capacity];
    }

    private void enlargeBuffer(int min) {
        int newCount = ((value.length >> 1) + value.length) + 2;
        char[] newData = new char[min > newCount ? min : newCount];
        System.arraycopy(value, 0, newData, 0, count);
        value = newData;
        shared = false;
    }

    /**
     * Sets the current length to a new value. If the new length is larger than
     * the current length, then the new characters at the end of this object
     * will contain the {@code char} value of {@code \u0000}.
     *
     * @param length
     *            the new length of this StringBuffer.
     * @exception IndexOutOfBoundsException
     *                if {@code length < 0}.
     * @see #length
     */
    public void setLength(int length) {
        if (length < 0) {
            throw new StringIndexOutOfBoundsException("length < 0: " + length);
        }
        if (length > value.length) {
            enlargeBuffer(length);
        } else {
            if (shared) {
                char[] newData = new char[value.length];
                System.arraycopy(value, 0, newData, 0, count);
                value = newData;
                shared = false;
            } else {
                if (count < length) {
                    Arrays.fill(value, count, length, (char) 0);
                }
            }
        }
        count = length;
    }

    /**
     * The current length.
     *
     * @return the number of characters contained in this instance.
     */
    public int length() {
        return count;
    }

    /*
     * Returns the character array.
     */
    public final char[] getValue() {
        return value;
    }

    /*
     * Returns the underlying buffer and sets the shared flag.
     */
    public final char[] shareValue() {
        shared = true;
        return value;
    }

    /**
     * Appends the string representation of the specified subset of the {@code
     * char[]}. The {@code char[]} value is converted to a String according to
     * the rule defined by {@link String#valueOf(char[],int,int)}.
     *
     * @param str
     *            the {@code char[]} to append.
     * @param offset
     *            the inclusive offset index.
     * @param len
     *            the number of characters.
     * @return this builder.
     * @throws ArrayIndexOutOfBoundsException
     *             if {@code offset} and {@code len} do not specify a valid
     *             subsequence.
     * @see String#valueOf(char[],int,int)
     */
    public StrBuilder append(char[] str, int offset, int len) {
        append0(str, offset, len);
        return this;
    }

    /**
     * Appends the string representation of the specified {@code char} value.
     * The {@code char} value is converted to a string according to the rule
     * defined by {@link String#valueOf(char)}.
     *
     * @param c
     *            the {@code char} value to append.
     * @return this builder.
     * @see String#valueOf(char)
     */
    public StrBuilder append(char c) {
        append0(c);
        return this;
    }

    final void append0(char[] chars, int offset, int length) {

        if ((offset | length) < 0 || offset > chars.length || chars.length - offset < length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int newCount = count + length;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        }
        System.arraycopy(chars, offset, value, count, length);
        count = newCount;
    }

    final void append0(char ch) {
        if (count == value.length) {
            enlargeBuffer(count + 1);
        }
        value[count++] = ch;
    }

    @Override
    public String toString() {
        return new String(value, 0, count);
    }


}
