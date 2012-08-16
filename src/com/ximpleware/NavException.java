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
 * This exception is for any exception conditions during navigation phase.
 * Creation date: (11/19/03 11:42:28 AM)
 */
public class NavException extends VTDException {

    /**
     * 
     */
    private static final long serialVersionUID = -7038334946509536256L;

    /**
     * NavException constructor comment.
     */
    public NavException() {
        super();
    }

    /**
     * NavException constructor comment.
     * 
     * @param s
     *            java.lang.String
     */
    public NavException(final String s) {
        super(s);
    }
}
