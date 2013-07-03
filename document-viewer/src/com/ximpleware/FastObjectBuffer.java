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
 * Fast object array implementation
 * 
 */
public class FastObjectBuffer implements IObjectBuffer {

    /*
     * bufferArrayList is a resizable array list of int buffers
     */
    private final arrayList bufferArrayList;

    /**
     * Total capacity of the ObjectBuffer
     */
    private int capacity;

    /**
     * Page size of the incremental growth of the object Buffer
     */
    private final int pageSize;

    /**
     * Total number of objects in the IntBuffer
     */
    protected int size;
    private final int exp;
    private final int r;

    /**
     * FastIntBuffer constructor comment.
     */
    public FastObjectBuffer() {
        size = 0;
        capacity = 0;
        pageSize = 1024;
        exp = 10;
        r = 1023;
        bufferArrayList = new arrayList();
    }

    /**
     * Constructor with adjustable buffer page size of the value bfz
     * 
     * @param e
     *            int
     */
    public FastObjectBuffer(final int e) {
        if (e < 0) {
            throw new IllegalArgumentException();
        }
        capacity = size = 0;
        pageSize = 1 << e;
        exp = e;
        r = pageSize - 1;
        bufferArrayList = new arrayList();
    }

    /**
     * Append an object array to the end of this buffer instance
     * 
     * @param obj_array
     *            Object[]
     */
    public void append(final Object[] obj_array) {
        if (obj_array == null) {
            throw new NullPointerException();
        }
        // no additional buffer space needed
        int lastBufferIndex;
        Object[] lastBuffer;

        if (bufferArrayList.size == 0) {
            lastBuffer = new Object[pageSize];
            bufferArrayList.add(lastBuffer);
            lastBufferIndex = 0;
            capacity = pageSize;
        } else {
            lastBufferIndex = Math.min((size >> exp),// +(((size&r)==0)? 0:1),
                    bufferArrayList.size - 1);
            lastBuffer = (Object[]) bufferArrayList.get(lastBufferIndex);
        }

        if ((this.size + obj_array.length) < this.capacity) {
            // get the last buffer from the bufferListArray
            // obtain the starting offset in that buffer to which the data is to be copied
            // update length

            // System.arraycopy(input, 0, lastBuffer, size % pageSize, input.length);
            if (this.size + obj_array.length < ((lastBufferIndex + 1) << exp)) {
                System.arraycopy(obj_array, 0, lastBuffer, size & r, obj_array.length);
            } else {
                int offset = pageSize - (size & r);
                // copy the first part
                System.arraycopy(obj_array, 0, lastBuffer, size & r, offset);
                // copy the middle part

                final int l = obj_array.length - offset;
                final int k = (l) >> exp;
                int z;
                for (z = 1; z <= k; z++) {
                    System.arraycopy(obj_array, offset, bufferArrayList.get(lastBufferIndex + z), 0, pageSize);
                    offset += pageSize;
                }
                // copy the last part
                System.arraycopy(obj_array, offset, bufferArrayList.get(lastBufferIndex + z), 0, l & r);
            }
            size += obj_array.length;
            return;
        } else // new buffers needed
        {

            // compute the number of additional buffers needed
            // int n =
            // ((int) ((int_array.length + size) / pageSize))
            // + (((int_array.length + size) % pageSize) > 0 ? 1 : 0)
            // - (int) (capacity / pageSize);
            final int n = ((obj_array.length + size) >> exp) + (((obj_array.length + size) & r) > 0 ? 1 : 0)
                    - (capacity >> exp);
            // create these buffers
            // add to bufferArrayList

            // System.arraycopy(int_array, 0, lastBuffer, size % pageSize, capacity - size);
            System.arraycopy(obj_array, 0, lastBuffer, size & r, capacity - size);

            for (int i = 0; i < n; i++) {
                final Object[] newBuffer = new Object[pageSize];
                if (i < n - 1) {
                    // full copy
                    System.arraycopy(obj_array, pageSize * i + capacity - size, newBuffer, 0, pageSize);
                } else {
                    // last page
                    System.arraycopy(obj_array, pageSize * i + capacity - size, newBuffer, 0, obj_array.length
                            + this.size - capacity - pageSize * i);
                }
                bufferArrayList.add(newBuffer);
            }
            // update length
            size += obj_array.length;
            // update capacity
            capacity += n * pageSize;
            // update
        }
    }

    /**
     * Append a single object to the end of this buffer Instance
     * 
     * @param obj
     */
    final public void append(final Object obj) {

        // Object[] lastBuffer;
        // int lastBufferIndex;
        /*
         * if (bufferArrayList.size == 0) {
         * lastBuffer = new Object[pageSize];
         * bufferArrayList.add(lastBuffer);
         * capacity = pageSize;
         * } else {
         * lastBufferIndex = Math.min((size>>exp),//+(((size&r)==0)? 0:1),
         * bufferArrayList.size - 1);
         * lastBuffer = (Object[]) bufferArrayList.oa[lastBufferIndex];
         * //lastBuffer = (int[]) bufferArrayList.get(bufferArrayList.size() - 1);
         * }
         */
        if (this.size < this.capacity) {
            // get the last buffer from the bufferListArray
            // obtain the starting offset in that buffer to which the data is to be copied
            // update length
            // System.arraycopy(long_array, 0, lastBuffer, size % pageSize, long_array.length);
            ((Object[]) bufferArrayList.oa[bufferArrayList.size - 1])[size & r] = obj;
            // lastBuffer[size % pageSize] = i;
            size += 1;
        } else // new buffers needed
        {
            final Object[] newBuffer = new Object[pageSize];
            size++;
            capacity += pageSize;
            bufferArrayList.add(newBuffer);
            newBuffer[0] = obj;
        }
    }

    /**
     * Returns the total allocated capacity of this buffer instance.
     * 
     * @return int
     */
    final public int getCapacity() {
        return capacity;
    }

    /**
     * Returns a single object array representing every object in this buffer instance
     * 
     * @return Object[] (null if there isn't anything left in the buffer
     * @param startingOffset
     *            int
     * @param len
     *            int
     * @return Object[]
     */
    public Object[] getObjectArray(final int startingOffset, final int len) {
        if (size <= 0 || startingOffset < 0) {
            throw (new IllegalArgumentException());
        }
        if ((startingOffset + len) > size) {
            throw (new IndexOutOfBoundsException());
        }
        final Object[] result = new Object[len]; // allocate result array

        // int first_index = (int) (startingOffset / pageSize);
        // int last_index = (int) ((startingOffset + len) / pageSize);
        // if ((startingOffset + len) % pageSize == 0) {
        // last_index--;
        // }
        final int first_index = startingOffset >> exp;
        int last_index = (startingOffset + len) >> exp;
        if (((startingOffset + len) & r) == 0) {
            last_index--;
        }

        if (first_index == last_index) {
            // to see if there is a need to go across buffer boundry
            System.arraycopy((bufferArrayList.get(first_index)),
            // startingOffset % pageSize,
                    startingOffset & r, result, 0, len);
        } else {
            int obj_array_offset = 0;
            for (int i = first_index; i <= last_index; i++) {
                final Object[] currentChunk = (Object[]) bufferArrayList.get(i);
                if (i == first_index) // first section
                {
                    System.arraycopy(currentChunk,
                    // startingOffset % pageSize,
                            startingOffset & r, result, 0,
                            // pageSize - (startingOffset % pageSize));
                            pageSize - (startingOffset & r));
                    // int_array_offset += pageSize - (startingOffset) % pageSize;
                    obj_array_offset += pageSize - (startingOffset & r);
                } else if (i == last_index) // last sections
                {
                    System.arraycopy(currentChunk, 0, result, obj_array_offset, len - obj_array_offset);

                } else {
                    System.arraycopy(currentChunk, 0, result, obj_array_offset, pageSize);
                    obj_array_offset += pageSize;
                }
            }
        }
        return result;
    }

    /**
     * Returns the page size of this buffer instance.
     * Creation date: (7/17/03 6:38:02 PM)
     * 
     * @return int
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the object at the location specified by index.
     * 
     * @return int
     * @param index
     *            int
     */
    @Override
    public Object objectAt(final int index) {
        if (index > size - 1) {
            throw new IndexOutOfBoundsException();
        }
        // int pageNum = (int) index / pageSize;
        final int pageNum = index >> exp;
        // System.out.println("page Number is "+pageNum);
        // int offset = index % pageSize;
        final int offset = index & r;
        return ((Object[]) bufferArrayList.get(pageNum))[offset];
    }

    /**
     * Assigns a new int value to location index of the buffer instance.
     * 
     * @param index
     *            int
     * @param newValue
     *            int
     */
    @Override
    public void modifyEntry(final int index, final Object newValue) {

        if (index > size - 1) {
            throw new IndexOutOfBoundsException();
        }

        // ((int[]) bufferArrayList.get((int) (index / pageSize)))[index % pageSize] =
        ((Object[]) bufferArrayList.get((index >> exp)))[index & r] = newValue;

    }

    /**
     * Returns the total number of objects in the buffer instance
     * 
     * @return int
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns the object array corresponding to all objects in this buffer instance
     * 
     * @return Object[] (null if the buffer is empty)
     */
    public Object[] toObjectArray() {
        if (size > 0) {
            int s = size;
            final Object[] resultArray = new Object[size];
            // copy all the content int into the resultArray
            int array_offset = 0;
            for (int i = 0; s > 0; i++) {
                System.arraycopy(bufferArrayList.get(i), 0, resultArray, array_offset, (s < pageSize) ? s : pageSize);
                // (i == (bufferArrayList.size() - 1)) ? size() % pageSize : pageSize);
                s = s - pageSize;
                array_offset += pageSize;
            }
            return resultArray;
        }
        return null;
    }

    /**
     * set the size of object buffer to zero, capacity
     * untouched so object buffer can be reused without
     * any unnecessary and additional allocation
     * 
     */
    public void clear() {
        size = 0;
    }

}
