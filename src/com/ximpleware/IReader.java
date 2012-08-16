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
 * Abstract Reader Class decodes underlying raw bytes.
 * Creation date: (3/5/04 7:18:57 PM)
 */
interface IReader {

    /**
     * Get next available Char from the buffer.
     * Creation date: (3/5/04 7:39:44 PM)
     * 
     * @return boolean
     * @exception com.ximpleware.ParseException
     *                Parse exception.
     * @exception com.ximpleware.EncodingException
     *                Raw char to UCS conversion exception.
     * @exception com.ximpleware.EOFException
     *                The end of file exception.
     */
    int getChar() throws EOFException, ParseException, EncodingException;

    /**
     * Skip the next char if it is a match.
     * Creation date: (3/5/04 7:39:44 PM)
     * 
     * @return boolean
     * @exception com.ximpleware.ParseException
     *                Parse exception.
     * @exception com.ximpleware.EncodingException
     *                Raw char to UCS conversion exception.
     * @exception com.ximpleware.EOFException
     *                The end of file exception.
     */
    boolean skipChar(int ch) throws ParseException, EncodingException, EOFException;

    char decode(int ch);

    long _getChar(int byte_offset);
}
