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
 * TranscodeException is thrown when there is exception condition (in XMLModifier)
 * for transcoding characters
 * 
 */
public class TranscodeException extends VTDException {

    /**
     * 
     */
    private static final long serialVersionUID = 740066564165219278L;

    public TranscodeException(final String s) {
        super(s);
    }

    public TranscodeException() {
    }
}
