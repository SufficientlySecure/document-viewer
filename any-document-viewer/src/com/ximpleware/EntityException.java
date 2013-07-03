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
 * Signals exception conditions for any invalid entity reference during parsing.
 * Creation date: (2/16/04 4:10:05 PM)
 */
public class EntityException extends ParseException {

    /**
     * 
     */
    private static final long serialVersionUID = -2230354438069457634L;

    /**
     * EntityException constructor comment.
     */
    public EntityException() {
        super();
    }

    /**
     * EntityException constructor comment.
     * 
     * @param s
     *            java.lang.String
     */
    public EntityException(final String s) {
        super(s);
    }
}
