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

class arrayList {

    public final static int GROWTH_INC = 16;
    public final static int default_init_capacity = 16;

    public arrayList() {
        oa = new Object[default_init_capacity];
        capacity = default_init_capacity;
    }

    public arrayList(final int initial_capacity) {
        oa = new Object[initial_capacity];
        capacity = initial_capacity;
    }

    final public Object get(final int i) {
        if (i >= size) {
            throw new IndexOutOfBoundsException();
        }
        return oa[i];
    }

    final public void add(final Object o) {
        if (size < capacity) {
            oa[size] = o;
            size++;
        } else {
            final Object[] oa1 = new Object[capacity + GROWTH_INC];
            System.arraycopy(oa, 0, oa1, 0, capacity);
            oa = oa1;
            capacity = capacity + GROWTH_INC;
            oa[size] = o;
            size++;
        }
    }

    final public void clear() {
        size = 0;
    }

    final public int size() {
        return size;
    }

    final public int getCapacity() {
        return capacity;
    }

    protected int size;
    protected int capacity;
    protected Object[] oa;

}
