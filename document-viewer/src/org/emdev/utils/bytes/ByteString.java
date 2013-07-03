package org.emdev.utils.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.LinkedList;

public class ByteString implements Comparator<ByteString> {

    private final byte[] value;

    private final int offset;

    private final int count;

    private int hashCode;

    private String string;

    public ByteString(final byte[] bytes) {
        this.value = new byte[bytes.length];
        this.offset = 0;
        this.count = bytes.length;
        if (count > 0) {
            System.arraycopy(bytes, 0, this.value, 0, count);
        }
    }

    public ByteString(final byte[] bytes, final int offset, final int count) {
        this.value = new byte[count];
        this.offset = 0;
        this.count = count;
        System.arraycopy(bytes, offset, this.value, 0, count);
    }

    public ByteString(final ByteString bytes, final int offset, final int count) {
        this.value = bytes.value;
        this.offset = bytes.offset + offset;
        this.count = count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public int length() {
        return count;
    }

    public int indexOf(final int value) {
        return indexOf(value, 0);
    }

    public int indexOf(final int value, final int start) {
        for (int i = start; i < count; i++) {
            if (this.value[i + offset] == value) {
                return i;
            }
        }
        return -1;
    }

    public LinkedList<ByteString> split(final int separator) {
        final LinkedList<ByteString> list = new LinkedList<ByteString>();
        int start = 0;
        for (int index = indexOf(separator, start); index != -1; index = indexOf(separator, start)) {
            list.add(new ByteString(this, start, index - start));
            start = index + 1;
        }
        if (list.isEmpty()) {
            list.add(this);
        } else if (start != this.count) {
            list.add(new ByteString(this, start, this.count - start));
        }
        return list;
    }

    public byte[] toBytes() {
        final byte[] bytes = new byte[count];
        System.arraycopy(this.value, this.offset, bytes, 0, count);
        return bytes;
    }

    public void writeTo(final OutputStream out) throws IOException {
        out.write(this.value, this.offset, this.count);
    }

    @Override
    public int hashCode() {
        int hash = hashCode;
        if (hash == 0) {
            if (count == 0) {
                return 0;
            }
            final int end = count + offset;
            final byte[] chars = value;
            for (int i = offset; i < end; ++i) {
                hash = 31 * hash + chars[i];
            }
            hashCode = hash;
        }
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ByteString) {
            final ByteString that = (ByteString) obj;
            if (this.count != that.count) {
                return false;
            }
            if (this.hashCode() != that.hashCode()) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                if (this.value[i + this.offset] != that.value[i + that.offset]) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public int compare(final ByteString lhs, final ByteString rhs) {
        if (lhs == rhs) {
            return 0;
        }

        if (lhs == null && rhs != null) {
            return -1;
        }
        if (lhs != null && rhs == null) {
            return +1;
        }

        int eqres = 0;
        int mincount = lhs.count;
        if (lhs.count < rhs.count) {
            eqres = -1;
            mincount = lhs.count;
        } else if (lhs.count > rhs.count) {
            eqres = 01;
            mincount = rhs.count;
        }
        for (int i = 0; i < mincount; i++) {
            final byte lb = lhs.value[i + lhs.offset];
            final byte rb = rhs.value[i + rhs.offset];
            if (lb < rb) {
                return -1;
            } else if (lb > rb) {
                return +1;
            }
        }

        return eqres;
    }

    @Override
    public String toString() {
        if (string == null) {
            final StringBuilder buf = new StringBuilder();
            for (int i = this.offset, end = this.offset + this.count; i < end; i++) {
                final byte b = this.value[i];
                if (b >= 0 && b < 128) {
                    buf.append((char) b);
                } else {
                    buf.append("\\x").append(Integer.toHexString(((int) b) & 0xFF));
                }
            }
            string = buf.toString();
        }
        return string;
    }
}
