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
 * Abstract long buffer storage interface
 * Creation date: (11/23/03 6:13:38 PM)
 */
public interface ILongBuffer {

    /**
     * Get the long at the given index.
     * Creation date: (11/23/03 6:29:53 PM)
     * 
     * @return long
     * @param index
     *            int
     */
    long longAt(int index);

    /**
     * Get the lower 32 bit of the long at given index.
     * Creation date: (11/23/03 6:45:27 PM)
     * 
     * @return int
     * @param index
     *            int
     */
    int lower32At(int index);

    /**
     * Modify the entry at given index with a new val.
     * Creation date: (11/23/03 6:42:57 PM)
     * 
     * @param index
     *            int
     * @param newVal
     *            long
     */
    void modifyEntry(int index, long newVal);

    /**
     * Get the number of long in the buffer.
     * Creation date: (11/23/03 7:22:25 PM)
     * 
     * @return int
     */
    int size();

    /**
     * Get the upper 32 bit of the long at the given index.
     * Creation date: (11/23/03 6:45:27 PM)
     * 
     * @return int
     * @param index
     *            int
     */
    int upper32At(int index);
}
