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

//import java.lang.NullPointerException;
//import com.ximpleware.NavException;

/**
 * This class is used as the global stack in VTDNav class.
 * The VTDNav object instantiates a context Buffer object, then everytime
 * load/store is called, current context information gets pushed/popped to/from the
 * ContextBuffer.
 * Creation date: (11/16/03 4:30:45 PM)
 */
public class ContextBuffer {

    private final arrayList bufferArrayList;
    private int capacity;
    private final int pageSize;
    protected int size;
    private final int incSize;
    private final int n; // for fast divide
    private final int r; // mask for remainder

    /**
     * ContextBuffer constructor comment.
     * inc is the # of int to be pushed/pop to/from the underlying storage
     * 
     * @param i
     *            int
     */
    public ContextBuffer(final int i) {
        super();
        pageSize = 1024;
        n = 10; // 1<<10 == 1024
        r = pageSize - 1;
        incSize = i;
        if (incSize < 0) {
            throw new IllegalArgumentException();
        }
        bufferArrayList = new arrayList();
    }

    /**
     * ContextBuffer constructor comment.
     * incSize is the # of int to be pushed/pop to/from the underlying storage
     * Creation date: (11/16/03 8:02:21 PM)
     * 
     * @param p
     *            int (pageSize equals (1<
     *            <p
     *            )
     * @param i
     *            int
     */
    public ContextBuffer(final int p, final int i) {
        if (p < 0) {
            throw new IllegalArgumentException("invalid Buffer size");
        }
        pageSize = (1 << p);
        r = pageSize - 1;
        n = p;
        incSize = i;
        if (incSize < 0) {
            throw new IllegalArgumentException("context buffer's incremental size must be greater than zero");
        }
        bufferArrayList = new arrayList();
    }

    /**
     * Pop the content value back into an integer array.
     * Creation date: (11/17/03 1:07:42 AM)
     * 
     * @param output
     *            int[]
     * @return boolean If load is successful.
     **/
    public boolean load(final int[] output) {

        if (size < incSize) {
            return false;
        }

        final int startingOffset = size - incSize;
        final int len = incSize;

        // int[] result = new int[len]; // allocate result array
        // if (pageSize != 1) {
        // int first_index = (int) (startingOffset / pageSize);
        // int last_index = (int) ((startingOffset + len) / pageSize);
        // if ((startingOffset + len) % pageSize == 0) {
        final int first_index = (startingOffset >> n);
        int last_index = ((startingOffset + len) >> n);
        if (((startingOffset + len) & r) == 0) {
            last_index--;
        }

        if (first_index == last_index) {
            // to see if there is a need to go across buffer boundry
            System.arraycopy((bufferArrayList.get(first_index)),
            // startingOffset % pageSize,
                    startingOffset & r, output, 0, len);
        } else {
            int int_array_offset = 0;
            for (int i = first_index; i <= last_index; i++) {
                final int[] currentChunk = (int[]) bufferArrayList.get(i);
                if (i == first_index) // first section
                {
                    System.arraycopy(currentChunk,
                    // startingOffset % pageSize
                            startingOffset & r, output, 0,
                            // pageSize - (startingOffset % pageSize));
                            pageSize - (startingOffset & r));
                    // int_array_offset += pageSize - (startingOffset) % pageSize;
                    int_array_offset += pageSize - (startingOffset & r);
                } else if (i == last_index) // last sections
                {
                    System.arraycopy(currentChunk, 0, output, int_array_offset, len - int_array_offset);

                } else {
                    System.arraycopy(currentChunk, 0, output, int_array_offset, pageSize);
                    int_array_offset += pageSize;
                }
            }
        }

        size -= incSize;
        return true;
    }

    /**
     * Test the load and store functions.
     * Creation date: (11/19/03 3:51:17 PM)
     * 
     * @param args
     *            java.lang.String[]
     */
    public static void main(final String[] args) {
        try {
            int[] ia = new int[18];

            final ContextBuffer cb = new ContextBuffer(18);
            for (int i = 0; i < 57; i++) {
                for (int j = 0; j < 17; j++) {
                    ia[j] = i;
                }
                ia[17] = -1;
                cb.store(ia);
            }
            // cb.store(ia);
            for (int i = 56; i >= 0; i--) {
                cb.load(ia);
                System.out.println("" + ia[0]);
                if (ia[17] != -1) {
                    System.out.println("store error " + i + " " + 17 + " " + ia[17]);
                }
                for (int j = 16; j >= 0; j--) {
                    if (ia[j] != i) {
                        System.out.println(" store error " + i + " " + j + " " + ia[j]);
                    }
                }
            }
            System.out.println("cb.clear()");
            cb.clear();

            for (int i = 0; i < 157; i++) {
                for (int j = 0; j < 18; j++) {
                    ia[j] = i;
                }
                cb.store(ia);
            }
            // cb.store(ia);
            for (int i = 156; i >= 0; i--) {
                cb.load(ia);
                System.out.println("" + ia[0]);
                for (int j = 17; j >= 0; j--) {
                    if (ia[j] != i) {
                        System.out.println(" store error " + i + " " + j + " " + ia[j]);
                    }
                }
            }
            cb.clear();
            for (int i = 0; i < 257; i++) {
                for (int j = 0; j < 18; j++) {
                    ia[j] = i;
                }
                cb.store(ia);
            }
            // cb.store(ia);
            for (int i = 256; i >= 0; i--) {
                cb.load(ia);
                System.out.println("" + ia[0]);
                for (int j = 17; j >= 0; j--) {
                    if (ia[j] != i) {
                        System.out.println(" store error " + i + " " + j + " " + ia[j]);
                    }
                }
            }

            System.out.println("success");

            System.out.println("test fastIntBuffer");
            ia = new int[1000000];
            for (int k = 0; k < 1000000; k++) {
                ia[k] = k;
            }
            final FastIntBuffer fib = new FastIntBuffer(13);
            // for(int i=0;i<10;i++){
            fib.append(ia);
            // }
            fib.clear();
            fib.append(ia);
            // for(int i=0;i<10;i++){
            // fib.append(ia);
            // }
            final int ib[] = fib.toIntArray();

            for (int i = 0; i < 1000000; i++) {
                if (ib[i] != i) {
                    System.out.println("error occurred at " + i);
                    // break;
                }
            }
            System.out.println("test fastLongBuffer");
            final long[] la = new long[1000000];
            for (int k = 0; k < 1000000; k++) {
                la[k] = k;
            }
            final FastLongBuffer flb = new FastLongBuffer(14);
            flb.append(la);
            flb.clear();
            flb.append(la);
            final long lb[] = flb.toLongArray();
            for (int i = 0; i < 1000000; i++) {
                if (lb[i] != i) {
                    System.out.println("error occurred at " + i);
                }
            }
            System.out.println("success");
        } catch (final Exception e) {
            e.printStackTrace();
            System.out.println(" exception caught ");
        }
    }

    /**
     * Set the context buffer size to zero
     * capacity untouched
     */
    public void clear() {
        size = 0;
    }

    /**
     * Push the array content on to the stack.
     * Creation date: (11/17/03 1:06:43 AM)
     * 
     * @param input
     *            int[]
     */
    public void store(final int[] input) {

        if (input == null) {
            throw new IllegalArgumentException("context buffer can't store a null pointer");
        }

        // if (input.length != incSize) {
        // throw a derivative of runtime exceptions
        // throw new NavException("Store: Input size doesn't match increment size");
        // }

        // no additional buffer space needed
        int lastBufferIndex;
        int[] lastBuffer;

        if (bufferArrayList.size() == 0) {
            lastBuffer = new int[pageSize];
            bufferArrayList.add(lastBuffer);
            lastBufferIndex = 0;
            capacity = pageSize;
        } else {
            lastBufferIndex = Math.min((size >> n),// +(((size&r)==0)? 0:1),
                    bufferArrayList.size() - 1);
            lastBuffer = (int[]) bufferArrayList.get(lastBufferIndex);
        }

        if ((this.size + input.length) < this.capacity) {
            // get the last buffer from the bufferListArray
            // obtain the starting offset in that buffer to which the data is to be copied
            // update length

            // System.arraycopy(input, 0, lastBuffer, size % pageSize, input.length);
            if (this.size + input.length < ((lastBufferIndex + 1) << n)) {
                System.arraycopy(input, 0, lastBuffer, size & r, input.length);
            } else {
                int offset = pageSize - (size & r);
                // copy the first part
                System.arraycopy(input, 0, lastBuffer, size & r, offset);
                // copy the middle part

                final int l = input.length - (offset);
                final int k = (l) >> n;
                int z;
                for (z = 1; z <= k; z++) {
                    System.arraycopy(input, offset, bufferArrayList.get(lastBufferIndex + z), 0, pageSize);
                    offset += pageSize;
                }
                // copy the last part
                System.arraycopy(input, offset, bufferArrayList.get(lastBufferIndex + z), 0, l & r);
            }
            size += input.length;
            return;
            // System.out.println("     --+++  buffer size "+size);
            // size += input.length;
        } else // new buffers needed
        {

            // compute the number of additional buffers needed
            final int k =
            // ((int) ((input.length + size) / pageSize))
            ((input.length + size) >> n)
            // + (((input.length + size) % pageSize) > 0 ? 1 : 0)
                    + (((input.length + size) & r) > 0 ? 1 : 0) - (capacity >> n);
            // create these buffers
            // add to bufferArrayList
            // System.arraycopy(input, 0, lastBuffer, size % pageSize, capacity - size);
            System.arraycopy(input, 0, lastBuffer, size & r, capacity - size);

            for (int i = 0; i < k; i++) {
                final int[] newBuffer = new int[pageSize];
                if (i < k - 1) {
                    // full copy
                    System.arraycopy(input, pageSize * i + capacity - size, newBuffer, 0, pageSize);
                } else {
                    // last page
                    System.arraycopy(input, pageSize * i + capacity - size, newBuffer, 0, (input.length + size)
                            - pageSize * i - capacity);
                }
                bufferArrayList.add(newBuffer);
            }
            // update length
            size += input.length;
            // update capacity
            capacity += (k << n);
            // update
        }

    }
}
