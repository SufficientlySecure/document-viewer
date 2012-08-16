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

/*
 *
 * This class is created to update VTDNav's implementation with
 * a more thread safe version
 */
package com.ximpleware;

/**
 * The VTD Navigator allows one to navigate XML document represented in VTD
 * records and Location caches. There is one and only one cursor that you can
 * navigate to any part of the tree. If a method operating on a node doesn't
 * accept the node as input, by default it refers to the cursor element. The
 * hierarchy consists entirely of elements.
 */
public class VTDNavEx {

    // Navigation directions
    public final static int ROOT = 0;
    public final static int PARENT = 1;
    public final static int FIRST_CHILD = 2;
    public final static int LAST_CHILD = 3;
    public final static int NEXT_SIBLING = 4;
    public final static int PREV_SIBLING = 5;

    // Navigation directions
    public final static int R = 0;
    public final static int P = 1;
    public final static int FC = 2;
    public final static int LC = 3;
    public final static int NS = 4;
    public final static int PS = 5;

    // token type definitions
    public final static int TOKEN_STARTING_TAG = 0;
    public final static int TOKEN_ENDING_TAG = 1;
    public final static int TOKEN_ATTR_NAME = 2;
    public final static int TOKEN_ATTR_NS = 3;
    public final static int TOKEN_ATTR_VAL = 4;
    public final static int TOKEN_CHARACTER_DATA = 5;
    public final static int TOKEN_COMMENT = 6;
    public final static int TOKEN_PI_NAME = 7;
    public final static int TOKEN_PI_VAL = 8;
    public final static int TOKEN_DEC_ATTR_NAME = 9;
    public final static int TOKEN_DEC_ATTR_VAL = 10;
    public final static int TOKEN_CDATA_VAL = 11;
    public final static int TOKEN_DTD_VAL = 12;
    public final static int TOKEN_DOCUMENT = 13;

    // encoding format definition here
    public final static int FORMAT_UTF8 = 2;
    public final static int FORMAT_ASCII = 0;

    public final static int FORMAT_ISO_8859_1 = 1;
    public final static int FORMAT_ISO_8859_2 = 3;
    public final static int FORMAT_ISO_8859_3 = 4;
    public final static int FORMAT_ISO_8859_4 = 5;
    public final static int FORMAT_ISO_8859_5 = 6;
    public final static int FORMAT_ISO_8859_6 = 7;
    public final static int FORMAT_ISO_8859_7 = 8;
    public final static int FORMAT_ISO_8859_8 = 9;
    public final static int FORMAT_ISO_8859_9 = 10;
    public final static int FORMAT_ISO_8859_10 = 11;
    public final static int FORMAT_ISO_8859_11 = 12;
    public final static int FORMAT_ISO_8859_12 = 13;
    public final static int FORMAT_ISO_8859_13 = 14;
    public final static int FORMAT_ISO_8859_14 = 15;
    public final static int FORMAT_ISO_8859_15 = 16;
    public final static int FORMAT_ISO_8859_16 = 17;

    public final static int FORMAT_WIN_1250 = 18;
    public final static int FORMAT_WIN_1251 = 19;
    public final static int FORMAT_WIN_1252 = 20;
    public final static int FORMAT_WIN_1253 = 21;
    public final static int FORMAT_WIN_1254 = 22;
    public final static int FORMAT_WIN_1255 = 23;
    public final static int FORMAT_WIN_1256 = 24;
    public final static int FORMAT_WIN_1257 = 25;
    public final static int FORMAT_WIN_1258 = 26;

    public final static int FORMAT_UTF_16LE = 64;
    public final static int FORMAT_UTF_16BE = 63;

    // String style
    public final static int STRING_RAW = 0;
    public final static int STRING_REGULAR = 1;
    public final static int STRING_NORMALIZED = 2;

    // masks for obtaining various fields from a VTD token
    protected final static long MASK_TOKEN_FULL_LEN = 0x000fffff00000000L;
    protected final static long MASK_TOKEN_PRE_LEN = 0x000ff80000000000L;
    protected final static long MASK_TOKEN_QN_LEN = 0x000007ff00000000L;
    protected static long MASK_TOKEN_OFFSET = 0x000000003fffffffL;
    protected final static long MASK_TOKEN_TYPE = 0xf000000000000000L;
    protected final static long MASK_TOKEN_DEPTH = 0x0ff0000000000000L;

    // tri-state variable for namespace lookup
    protected final static long MASK_TOKEN_NS_MARK = 0x00000000c0000000L;
    protected short maxLCDepthPlusOne = 4;

    protected int rootIndex; // where the root element is at
    protected int nestingLevel;
    protected int[] context; // main navigation tracker aka context object
    protected boolean atTerminal; // this variable is to make vn compatible with
                                  // xpath's data model

    // location cache part
    protected int l2upper;
    protected int l2lower;
    protected int l3upper;
    protected int l3lower;
    protected int l2index;
    protected int l3index;
    protected int l1index;

    // containers
    protected FastLongBuffer vtdBuffer;
    protected FastLongBuffer l1Buffer;
    protected FastLongBuffer l2Buffer;
    protected FastIntBuffer l3Buffer;
    protected char[] XMLDoc;

    // private int recentNS; // most recently visited NS node, experiment for
    // now
    // Hierarchical representation is an array of integers addressing elements
    // tokens
    protected ContextBuffer contextStack;
    protected ContextBuffer contextStack2;// this is reserved for XPath

    protected int LN; // record txt and attrbute for XPath eval purposes
    // the document encoding
    protected int encoding;
    // protected boolean writeOffsetAdjustment;
    // for string to token comparison
    // protected int currentOffset;
    // protected int currentOffset2;

    // whether the navigation is namespace enabled or not.
    protected boolean ns;

    // intermediate buffer for push and pop purposes
    protected int[] stackTemp;
    protected int docOffset;
    // length of the document
    protected int docLen;
    protected int vtdSize; // vtd record count

    protected String name;
    protected int nameIndex;

    protected String localName;
    protected int localNameIndex;
    protected FastIntBuffer fib;// for store string value
    protected boolean shallowDepth;
    protected String URIName;

    protected VTDNavEx() {
    }

    /**
     * Initialize the VTD navigation object.
     * 
     * @param RootIndex
     *            int
     * @param maxDepth
     *            int
     * @param encoding
     *            int
     * @param NS
     *            boolean
     * @param x
     *            byte[]
     * @param vtd
     *            com.ximpleware.ILongBuffer
     * @param l1
     *            com.ximpleware.ILongBuffer
     * @param l2
     *            com.ximpleware.ILongBuffer
     * @param l3
     *            com.ximpleware.IIntBuffer
     * @param so
     *            int starting offset of the document(in byte)
     * @param length
     *            int length of the document (in byte)
     */
    protected VTDNavEx(final int RootIndex, final int enc, final boolean NS, final int depth, final char[] x,
            final FastLongBuffer vtd, final FastLongBuffer l1, final FastLongBuffer l2, final FastIntBuffer l3,
            final int so, // start offset of the starting offset(in byte)
            final int length) // lengnth of the XML document (in byte))
    {
        // initialize all buffers
        if (l1 == null || l2 == null || l3 == null || vtd == null || x == null || depth < 0 || RootIndex < 0 // ||
                                                                                                             // encoding
                                                                                                             // <=
                                                                                                             // FORMAT_UTF8
                // || encoding >= FORMAT_ISO_8859_1
                || so < 0 || length < 0) {
            throw new IllegalArgumentException();
        }

        l1Buffer = l1;
        l2Buffer = l2;
        l3Buffer = l3;
        vtdBuffer = vtd;
        XMLDoc = x;

        encoding = enc;
        // System.out.println("encoding " + encoding);
        rootIndex = RootIndex;
        nestingLevel = depth + 1;
        ns = NS; // namespace aware or not
        if (ns == false) {
            MASK_TOKEN_OFFSET = 0x000000007fffffffL; // this allows xml size to
                                                     // be 2GB
        } else {
            // if there is no namespace
            MASK_TOKEN_OFFSET = 0x000000003fffffffL;
        }

        atTerminal = false; // this variable will only change value during XPath
                            // eval

        // initialize the context object
        this.context = new int[nestingLevel];
        // depth value is the first entry in the context because root is
        // singular.
        context[0] = 0;
        // set the value to zero
        for (int i = 1; i < nestingLevel; i++) {
            context[i] = -1;
        }
        // currentOffset = 0;
        // contextStack = new ContextBuffer(1024, nestingLevel + 7);
        contextStack = new ContextBuffer(10, nestingLevel + 9);
        contextStack2 = new ContextBuffer(10, nestingLevel + 9);
        stackTemp = new int[nestingLevel + 9];

        // initial state of LC variables
        l1index = l2index = l3index = -1;
        l2lower = l3lower = -1;
        l2upper = l3upper = -1;
        docOffset = so;
        docLen = length;
        // System.out.println("offset " + offset + " length " + length);
        // printL2Buffer();
        vtdSize = vtd.size;
        // writeOffsetAdjustment = false;
        // recentNS = -1;
        name = null;
        nameIndex = -1;
        localName = null;
        localNameIndex = -1;
        fib = new FastIntBuffer(5); // page size is 32 ints
        shallowDepth = true;
    }

    /**
     * Get the depth (>=0) of the current element. Creation date: (11/16/03
     * 6:58:22 PM)
     * 
     * @return int
     */
    final public int getCurrentDepth() {
        return context[0];
    }

    /**
     * Get the index value of the current element. Creation date: (11/16/03
     * 6:40:25 PM)
     * 
     * @return int
     */
    final public int getCurrentIndex() {
        if (atTerminal) {
            return LN;
        }
        return getCurrentIndex2();
        // return (context[0] == 0) ? rootIndex : context[context[0]];
    }

    // this one is used in iterAttr() in autoPilot
    final protected int getCurrentIndex2() {
        switch (context[0]) {
            case -1:
                return 0;
            case 0:
                return rootIndex;
            default:
                return context[context[0]];
        }
    }

    /**
     * Get the encoding of the XML document.
     * 
     * @return int
     */
    final public int getEncoding() {
        return encoding;
    }

    /**
     * Get the maximum nesting depth of the XML document (>0). max depth is
     * nestingLevel -1
     * 
     * @return int
     */
    final public int getNestingLevel() {
        return nestingLevel;
    }

    /**
     * Get root index value , which is the index val of root element
     * 
     * @return int
     */
    final public int getRootIndex() {
        return rootIndex;
    }

    /**
     * Get total number of VTD tokens for the current XML document.
     * 
     * @return int
     */
    final public int getTokenCount() {
        return vtdSize;
    }

    /**
     * Get the depth value of a token (>=0).
     * 
     * @return int
     * @param index
     *            int
     */
    final public int getTokenDepth(final int index) {
        final int i = (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_DEPTH) >> 52);
        if (i != 255) {
            return i;
        }
        return -1;
    }

    /**
     * Get the token length at the given index value please refer to VTD spec
     * for more details Length is in terms of the UTF char unit For prefixed
     * tokens, it is the qualified name length. When ns is not enabled, return
     * the full name length for attribute name and element name When ns is
     * enabled, return an int with upper 16 bit for prefix length, lower 16 bit
     * for qname length
     * 
     * @return int
     * @param index
     *            int
     */
    public int getTokenLength(int index) {
        final int type = getTokenType(index);
        int depth;
        int len = 0;
        long l;
        int temp = 0;
        switch (type) {
            case TOKEN_ATTR_NAME:
            case TOKEN_ATTR_NS:
            case TOKEN_STARTING_TAG:
                l = vtdBuffer.longAt(index);
                return (ns == false) ? (int) ((l & MASK_TOKEN_QN_LEN) >> 32)
                        : ((int) ((l & MASK_TOKEN_QN_LEN) >> 32) | ((int) ((l & MASK_TOKEN_PRE_LEN) >> 32) << 5));
            case TOKEN_CHARACTER_DATA:
            case TOKEN_CDATA_VAL:
            case TOKEN_COMMENT: // make sure this is total length
                depth = getTokenDepth(index);
                do {
                    len = len + (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_FULL_LEN) >> 32);
                    temp = getTokenOffset(index) + (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_FULL_LEN) >> 32);
                    index++;
                } while (index < vtdSize && depth == getTokenDepth(index) && type == getTokenType(index)
                        && temp == getTokenOffset(index));
                // if (int k=0)
                return len;
            default:
                return (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_FULL_LEN) >> 32);
        }

    }

    /**
     * Get the starting offset (unit in native char) of the token at the given
     * index.
     * 
     * @return int
     * @param index
     *            int
     */
    final public int getTokenOffset(final int index) {
        // return (context[0] != 0)
        // ? (int) (vtdBuffer.longAt(context[context[0]]) & MASK_TOKEN_OFFSET)
        // : (int) (vtdBuffer.longAt(rootIndex) & MASK_TOKEN_OFFSET);
        return (int) (vtdBuffer.longAt(index) & MASK_TOKEN_OFFSET);
    }

    /**
     * Get the token type of the token at the given index value. Creation date:
     * (11/16/03 6:41:51 PM)
     * 
     * @return int
     * @param index
     *            int
     */
    final public int getTokenType(final int index) {
        return (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_TYPE) >> 60) & 0xf;
    }

    /**
     * Load the context info from ContextBuffer. Info saved including LC and
     * current state of the context
     * 
     * @return boolean
     * 
     */
    public boolean pop() {
        final boolean b = contextStack.load(stackTemp);
        if (b == false) {
            return false;
        }
        for (int i = 0; i < nestingLevel; i++) {
            context[i] = stackTemp[i];
        }

        l1index = stackTemp[nestingLevel];
        l2index = stackTemp[nestingLevel + 1];
        l3index = stackTemp[nestingLevel + 2];
        l2lower = stackTemp[nestingLevel + 3];
        l2upper = stackTemp[nestingLevel + 4];
        l3lower = stackTemp[nestingLevel + 5];
        l3upper = stackTemp[nestingLevel + 6];
        atTerminal = (stackTemp[nestingLevel + 7] == 1);
        LN = stackTemp[nestingLevel + 8];
        return true;
    }

    /**
     * Store the context info into the ContextBuffer. Info saved including LC
     * and current state of the context Creation date: (11/16/03 7:00:27 PM)
     */
    public void push() {

        for (int i = 0; i < nestingLevel; i++) {
            stackTemp[i] = context[i];
        }
        stackTemp[nestingLevel] = l1index;
        stackTemp[nestingLevel + 1] = l2index;
        stackTemp[nestingLevel + 2] = l3index;
        stackTemp[nestingLevel + 3] = l2lower;
        stackTemp[nestingLevel + 4] = l2upper;
        stackTemp[nestingLevel + 5] = l3lower;
        stackTemp[nestingLevel + 6] = l3upper;
        if (atTerminal) {
            stackTemp[nestingLevel + 7] = 1;
        } else {
            stackTemp[nestingLevel + 7] = 0;
        }
        stackTemp[nestingLevel + 8] = LN;
        contextStack.store(stackTemp);
    }

    /**
     * A generic navigation method. Move the cursor to the element according to
     * the direction constants If no such element, no position change and return
     * false. Creation date: (12/2/03 1:43:50 PM) Legal direction constants are
     * 
     * <pre>
     *    			ROOT               0
     * </pre>
     * 
     * <pre>
     *  		    PARENT  		   1
     * </pre>
     * 
     * <pre>
     *        	    FIRST_CHILD		   2
     * </pre>
     * 
     * <pre>
     *  		    LAST_CHILD 		   3
     * </pre>
     * 
     * <pre>
     *     	  	    NEXT_SIBLING       4
     * </pre>
     * 
     * <pre>
     *       	    PREV_SIBLING       5
     * </pre>
     * 
     * @return boolean
     * @param direction
     *            int
     * @exception com.ximpleware.NavException
     *                When direction value is illegal.
     */
    public boolean toElement(final int direction) throws NavException {
        int size;
        switch (direction) {
            case ROOT: // to document element!
                if (context[0] != 0) {
                    /*
                     * for (int i = 1; i <= context[0]; i++) { context[i] =
                     * 0xffffffff; }
                     */
                    context[0] = 0;
                }
                atTerminal = false;
                l1index = l2index = l3index = -1;
                return true;
            case PARENT:
                if (atTerminal == true) {
                    atTerminal = false;
                    return true;
                }
                if (context[0] > 0) {
                    // context[context[0]] = context[context[0] + 1] =
                    // 0xffffffff;
                    context[context[0]] = -1;
                    context[0]--;
                    return true;
                } else if (context[0] == 0) {
                    context[0] = -1; // to be compatible with XPath Data model
                    return true;
                } else {
                    return false;
                }
            case FIRST_CHILD:
            case LAST_CHILD:
                if (atTerminal) {
                    return false;
                }
                switch (context[0]) {
                    case -1:
                        context[0] = 0;
                        return true;
                    case 0:
                        if (l1Buffer.size > 0) {
                            context[0] = 1;
                            l1index = (direction == FIRST_CHILD) ? 0 : (l1Buffer.size - 1);
                            context[1] = l1Buffer.upper32At(l1index);
                            // (int) (vtdToken >> 32);
                            return true;
                        } else {
                            return false;
                        }
                    case 1:
                        l2lower = l1Buffer.lower32At(l1index);
                        if (l2lower == -1) {
                            return false;
                        }
                        context[0] = 2;
                        l2upper = l2Buffer.size - 1;
                        size = l1Buffer.size;
                        for (int i = l1index + 1; i < size; i++) {
                            final int temp = l1Buffer.lower32At(i);
                            if (temp != 0xffffffff) {
                                l2upper = temp - 1;
                                break;
                            }
                        }
                        // System.out.println(" l2 upper: " + l2upper + " l2
                        // lower : " + l2lower);
                        l2index = (direction == FIRST_CHILD) ? l2lower : l2upper;
                        context[2] = l2Buffer.upper32At(l2index);
                        return true;

                    case 2:
                        l3lower = l2Buffer.lower32At(l2index);
                        if (l3lower == -1) {
                            return false;
                        }
                        context[0] = 3;

                        l3upper = l3Buffer.size - 1;
                        size = l2Buffer.size;
                        for (int i = l2index + 1; i < size; i++) {
                            final int temp = l2Buffer.lower32At(i);
                            if (temp != 0xffffffff) {
                                l3upper = temp - 1;
                                break;
                            }
                        }
                        // System.out.println(" l3 upper : " + l3upper + " l3
                        // lower : " + l3lower);
                        l3index = (direction == FIRST_CHILD) ? l3lower : l3upper;
                        context[3] = l3Buffer.intAt(l3index);

                        return true;

                    default:
                        if (direction == FIRST_CHILD) {
                            size = vtdBuffer.size;
                            int index = context[context[0]] + 1;
                            while (index < size) {
                                final long temp = vtdBuffer.longAt(index);
                                final int token_type = (int) ((MASK_TOKEN_TYPE & temp) >> 60) & 0xf;

                                if (token_type == TOKEN_STARTING_TAG) {
                                    final int depth = (int) ((MASK_TOKEN_DEPTH & temp) >> 52);
                                    if (depth <= context[0]) {
                                        return false;
                                    } else if (depth == (context[0] + 1)) {
                                        context[0] += 1;
                                        context[context[0]] = index;
                                        return true;
                                    }
                                }

                                index++;
                            } // what condition
                            return false;
                        } else {
                            int index = context[context[0]] + 1;
                            int last_index = -1;
                            size = vtdBuffer.size;
                            while (index < size) {
                                final long temp = vtdBuffer.longAt(index);
                                final int depth = (int) ((MASK_TOKEN_DEPTH & temp) >> 52);
                                final int token_type = (int) ((MASK_TOKEN_TYPE & temp) >> 60) & 0xf;

                                if (token_type == TOKEN_STARTING_TAG) {
                                    if (depth <= context[0]) {
                                        break;
                                    } else if (depth == (context[0] + 1)) {
                                        last_index = index;
                                    }
                                }

                                index++;
                            }
                            if (last_index == -1) {
                                return false;
                            } else {
                                context[0] += 1;
                                context[context[0]] = last_index;
                                return true;
                            }
                        }
                }

            case NEXT_SIBLING:
            case PREV_SIBLING:
                if (atTerminal) {
                    return false;
                }
                switch (context[0]) {
                    case -1:
                    case 0:
                        return false;
                    case 1:
                        if (direction == NEXT_SIBLING) {
                            if (l1index + 1 >= l1Buffer.size) {
                                return false;
                            }

                            l1index++; // global incremental
                        } else {
                            if (l1index - 1 < 0) {
                                return false;
                            }
                            l1index--; // global incremental
                        }
                        context[1] = l1Buffer.upper32At(l1index);
                        return true;
                    case 2:
                        if (direction == NEXT_SIBLING) {
                            if (l2index + 1 > l2upper) {
                                return false;
                            }
                            l2index++;
                        } else {
                            if (l2index - 1 < l2lower) {
                                return false;
                            }
                            l2index--;
                        }
                        context[2] = l2Buffer.upper32At(l2index);
                        return true;
                    case 3:
                        if (direction == NEXT_SIBLING) {
                            if (l3index + 1 > l3upper) {
                                return false;
                            }
                            l3index++;
                        } else {
                            if (l3index - 1 < l3lower) {
                                return false;
                            }
                            l3index--;
                        }
                        context[3] = l3Buffer.intAt(l3index);
                        return true;
                    default:
                        // int index = context[context[0]] + 1;

                        if (direction == NEXT_SIBLING) {
                            int index = context[context[0]] + 1;
                            size = vtdBuffer.size;
                            while (index < size) {
                                final long temp = vtdBuffer.longAt(index);
                                final int token_type = (int) ((MASK_TOKEN_TYPE & temp) >> 60) & 0xf;

                                if (token_type == TOKEN_STARTING_TAG) {
                                    final int depth = (int) ((MASK_TOKEN_DEPTH & temp) >> 52);
                                    if (depth < context[0]) {
                                        return false;
                                    } else if (depth == (context[0])) {
                                        context[context[0]] = index;
                                        return true;
                                    }
                                }
                                index++;
                            }
                            return false;
                        } else {
                            int index = context[context[0]] - 1;
                            while (index > context[context[0] - 1]) {
                                // scan backforward
                                final long temp = vtdBuffer.longAt(index);
                                final int token_type = (int) ((MASK_TOKEN_TYPE & temp) >> 60) & 0xf;

                                if (token_type == TOKEN_STARTING_TAG) {
                                    final int depth = (int) ((MASK_TOKEN_DEPTH & temp) >> 52);
                                    /*
                                     * if (depth < context[0]) { return false; }
                                     * else
                                     */
                                    if (depth == (context[0])) {
                                        context[context[0]] = index;
                                        return true;
                                    }
                                }
                                index--;
                            } // what condition
                            return false;
                        }
                }

            default:
                throw new NavException("illegal navigation options");
        }

    }

    final public char[] toRawString(final int index, final int[] range) throws NavException {
        final int type = getTokenType(index);
        range[0] = getTokenOffset(index);
        if (type == TOKEN_STARTING_TAG || type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS) {
            range[1] = getTokenLength(index) & 0xffff;
        } else {
            range[1] = getTokenLength(index);
        }
        return XMLDoc;
    }
}
