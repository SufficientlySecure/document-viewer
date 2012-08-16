/* 
 * Copyright (C) 2002-2011 XimpleWare, info@ximpleware.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.ximpleware;

/**
 * A fast, unsynchronized, chunk-based long buffer for storing LCs and VTD.
 * Creation date: (7/17/03 6:07:46 PM)
 */
public class FastLongBuffer implements ILongBuffer {

    /*
     * bufferArrayList is a resizable array list of int buffers
     */
    private final arrayList bufferArrayList;

    /**
     * Total capacity of the IntBuffer
     */
    private int capacity;

    /**
     * Page size of the incremental growth of the Int Buffer
     */
    private final int pageSize;
    /**
     * Total number of integers in the IntBuffer
     */
    protected int size;
    private final int exp;
    private final int r;

    /**
     * FastLongBuffer constructor comment.
     */
    public FastLongBuffer() {
        size = 0;
        capacity = 0;
        pageSize = 1024;
        exp = 10;
        r = 1023;
        bufferArrayList = new arrayList();
    }

    /**
     * Construct a FastLongBuffer instance with specified page size
     * 
     * @param e
     *            int (so that pageSize = (1<<e))
     */
    public FastLongBuffer(final int e) {
        if (e <= 0) {
            throw new IllegalArgumentException();
        }
        capacity = size = 0;
        pageSize = (1 << e);
        exp = e;
        r = pageSize - 1;
        bufferArrayList = new arrayList();
    }

    /**
     * Construct a FastLongBuffer instance with specified page size
     * 
     * @param e
     *            int (so that pageSize = (1<<e))
     * @param c
     *            int (suggest initial capacity of ArrayList
     */
    public FastLongBuffer(final int e, final int c) {
        if (e <= 0) {
            throw new IllegalArgumentException();
        }
        capacity = size = 0;
        pageSize = (1 << e);
        exp = e;
        r = pageSize - 1;
        bufferArrayList = new arrayList(c);
    }

    /**
     * Append single long to the end of array buffer.
     * 
     * @param long_array
     *            long[]
     */
    public void append(final long[] long_array) {
        if (long_array == null) {
            throw new NullPointerException();
        }
        // no additional buffer space needed
        int lastBufferIndex;
        long[] lastBuffer;
        if (bufferArrayList.size == 0) {
            lastBuffer = new long[pageSize];
            bufferArrayList.add(lastBuffer);
            lastBufferIndex = 0;
            capacity = pageSize;
        } else {
            lastBufferIndex = Math.min((size >> exp),// +(((size&r)==0)? 0:1),
                    bufferArrayList.size - 1);
            lastBuffer = (long[]) bufferArrayList.oa[lastBufferIndex];
        }

        if ((this.size + long_array.length) < this.capacity) {
            // get the last buffer from the bufferListArray
            // obtain the starting offset in that buffer to which the data is to be copied
            // update length
            if (this.size + long_array.length < ((lastBufferIndex + 1) << exp)) {
                System.arraycopy(long_array, 0, lastBuffer, size & r, long_array.length);
            } else {
                int offset = pageSize - (size & r);
                // copy the first part
                System.arraycopy(long_array, 0, lastBuffer, size & r, offset);
                // copy the middle part
                final int l = long_array.length - offset;
                final int k = (l) >> exp;
                int z;
                for (z = 1; z <= k; z++) {
                    System.arraycopy(long_array, offset, bufferArrayList.oa[lastBufferIndex + z], 0, pageSize);
                    offset += pageSize;
                }
                // copy the last part
                System.arraycopy(long_array, offset, bufferArrayList.oa[lastBufferIndex + z], 0, l & r);
            }
            size += long_array.length;
            return;
        } else // new buffers needed
        {

            // compute the number of additional buffers needed
            // int n =
            // ((int) ((long_array.length + size) / pageSize))
            // + (((long_array.length + size) % pageSize) > 0 ? 1 : 0)
            // - (int) (capacity / pageSize);
            final int n = ((long_array.length + size) >> exp) + (((long_array.length + size) & r) > 0 ? 1 : 0)
                    - (capacity >> exp);
            // create these buffers
            // add to bufferArrayList
            // System.arraycopy(long_array, 0, lastBuffer, size % pageSize, capacity - size);
            System.arraycopy(long_array, 0, lastBuffer, size & r, capacity - size);

            for (int i = 0; i < n; i++) {
                final long[] newBuffer = new long[pageSize];
                if (i < n - 1) {
                    // full copy
                    System.arraycopy(long_array, pageSize * i + capacity - size, newBuffer, 0, pageSize);
                } else {
                    // last page
                    System.arraycopy(long_array, pageSize * i + capacity - size, newBuffer, 0, long_array.length + size
                            - pageSize * i - capacity);
                }
                bufferArrayList.add(newBuffer);
            }
            // update length
            size += long_array.length;
            // update capacity
            capacity += n * pageSize;
            // update
        }
    }

    /**
     * Append an integer to the end of this array buffer
     * 
     * @param i
     *            long
     */
    public final void append(final long i) {
        // long[] lastBuffer;
        // int lastBufferIndex;
        /*
         * if (bufferArrayList.size == 0) {
         * lastBuffer = new long[pageSize];
         * bufferArrayList.add(lastBuffer);
         * capacity = pageSize;
         * } else {
         * lastBufferIndex = Math.min((size>>exp),//+(((size&r)==0)? 0:1),
         * bufferArrayList.size - 1);
         * lastBuffer = (long[]) bufferArrayList.oa[lastBufferIndex];
         * //lastBuffer = (long[]) bufferArrayList.get(bufferArrayList.size() - 1);
         * }
         */
        if (this.size < this.capacity) {
            // get the last buffer from the bufferListArray
            // obtain the starting offset in that buffer to which the data is to be copied
            // update length
            // System.arraycopy(long_array, 0, lastBuffer, size % pageSize, long_array.length);
            // lastBuffer[size % pageSize] = i;
            ((long[]) bufferArrayList.oa[bufferArrayList.size - 1])[size & r] = i;
            size += 1;
        } else // new buffers needed
        {
            final long[] newBuffer = new long[pageSize];
            size++;
            capacity += pageSize;
            bufferArrayList.add(newBuffer);
            newBuffer[0] = i;
        }
    }

    /**
     * Get the capacity of the buffer.
     * 
     * @return int
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Return a selected chuck of long buffer as a long array.
     * 
     * @return long[]
     * @param startingOffset
     *            int
     * @param len
     *            int
     */
    public long[] getLongArray(final int startingOffset, final int len) {
        if (size <= 0 || startingOffset < 0) {
            throw (new IllegalArgumentException());
        }
        if ((startingOffset + len) > size) {
            throw (new IndexOutOfBoundsException());
        }

        final long[] result = new long[len]; // allocate result array

        final int first_index = (startingOffset >> exp);
        int last_index = ((startingOffset + len) >> exp);

        // if ((startingOffset + len) % pageSize == 0) {
        if (((startingOffset + len) & r) == 0) {
            last_index--;
        }

        if (first_index == last_index) {
            // to see if there is a need to go across buffer boundry
            System.arraycopy((bufferArrayList.oa[first_index]),
            // startingOffset % pageSize,
                    startingOffset & r, result, 0, len);
        } else {
            int long_array_offset = 0;
            for (int i = first_index; i <= last_index; i++) {
                final long[] currentChunk = (long[]) bufferArrayList.oa[i];
                if (i == first_index) // first section
                {
                    System.arraycopy(currentChunk,
                    // startingOffset % pageSize,
                            startingOffset & r, result, 0,
                            // pageSize - (startingOffset % r));
                            pageSize - (startingOffset & r));
                    long_array_offset += pageSize - (startingOffset & r);
                } else if (i == last_index) // last sections
                {
                    System.arraycopy(currentChunk, 0, result, long_array_offset, len - long_array_offset);

                } else {
                    System.arraycopy(currentChunk, 0, result, long_array_offset, pageSize);
                    long_array_offset += pageSize;
                }
            }
        }

        return result;
    }

    /**
     * Get the buffer page size.
     * 
     * @return int
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the long val at given index value.
     * 
     * @return long
     * @param index
     *            int
     */
    @Override
    public final long longAt(final int index) {
        /*
         * if (index >= size) {
         * throw new IndexOutOfBoundsException();
         * }
         */
        final int pageNum = (index >> exp);
        // int offset = index % r;
        final int offset = index & r;
        return ((long[]) bufferArrayList.oa[pageNum])[offset];
    }

    /**
     * Get the lower 32 bit of the integer at the given index.
     * 
     * @return int
     * @param index
     *            int
     */
    @Override
    public final int lower32At(final int index) {
        /*
         * if ( index > size) {
         * throw new IndexOutOfBoundsException();
         * }
         */
        final int pageNum = (index >> exp);
        // int offset = index % pageSize;
        final int offset = index & r;
        return (int) ((long[]) bufferArrayList.oa[pageNum])[offset];
    }

    /**
     * Modify the value at the index to a new val.
     * 
     * @param index
     *            int
     * @param newValue
     *            long
     */
    @Override
    public final void modifyEntry(final int index, final long newValue) {

        /*
         * if ( index > size + 1) {
         * throw new IndexOutOfBoundsException();
         * }
         */
        // ((long[]) bufferArrayList.get((int) (index / pageSize)))[index % pageSize] =
        ((long[]) bufferArrayList.oa[index >> exp])[index & r] = newValue;
    }

    /**
     * Get the total number of longs in the buffer.
     * 
     * @return int
     */
    @Override
    public final int size() {
        return size;
    }

    /**
     * Convert all longs into a long array.
     * 
     * @return long[]
     */
    public long[] toLongArray() {
        if (size > 0) {
            int s = size;
            final long[] resultArray = new long[size];
            // copy all the content int into the resultArray
            int array_offset = 0;
            for (int i = 0; s > 0; i++) {
                System.arraycopy(bufferArrayList.oa[i], 0, resultArray, array_offset, (s < pageSize) ? s : pageSize);
                // (i == (bufferArrayList.size() - 1)) ? size - ((size>>exp)<<exp) : pageSize);
                // (i == (bufferArrayList.size() - 1)) ? (size & r) : pageSize);
                s = s - pageSize;
                array_offset += pageSize;
            }
            return resultArray;
        }
        return null;
    }

    /**
     * Return the upper 32 bit of the long at the index.
     * 
     * @return int
     * @param index
     *            int
     */
    @Override
    public int upper32At(final int index) {
        /*
         * if ( index >= size) {
         * throw new IndexOutOfBoundsException();
         * }
         */
        final int pageNum = (index >> exp);
        final int offset = index & r;
        return (int) ((((long[]) bufferArrayList.oa[pageNum])[offset] & (0xffffffffL << 32)) >> 32);

    }

    /**
     * set teh size of long buffer to zero, capacity
     * untouched so long buffer can be reused without
     * any unnecessary and additional allocation
     * 
     */
    public final void clear() {
        size = 0;
    }

    /**
     * Set the size of FastLongBuffer to newSz if newSz is less than the
     * capacity, otherwise return false
     * 
     * @param newSz
     * @return status of resize
     * 
     */
    public boolean resize(final int newSz) {
        if (newSz <= capacity && newSz >= 0) {
            size = newSz;
            return true;
        } else {
            return false;
        }
    }
}
