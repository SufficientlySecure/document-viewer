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

//import com.ximpleware.parser.ISO8859_11;
/**
 * VTD Generator implementation.
 * Current support built-in entities only
 * It parses DTD, but doesn't resolve declared entities
 */
public class VTDGenEx {

    class CharReader implements IReader {

        public CharReader() {
        }

        @Override
        final public int getChar() throws EOFException, ParseException, EncodingException {
            if (offset >= endOffset) {
                throw e;
            }
            return XMLDoc[offset++];
        }

        @Override
        final public boolean skipChar(final int ch) throws ParseException, EOFException, EncodingException {
            if (ch == XMLDoc[offset]) {
                offset++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        final public long _getChar(final int offset) {
            final int c = XMLDoc[offset];
            if (c == '\r' && XMLDoc[offset + 1] == '\n') {
                return (2L << 32) | '\n';
            }
            return (1L << 32) | c;
        }

        @Override
        final public char decode(final int offset) {
            return XMLDoc[offset];
        }

    }

    // attr_name_array size
    private final static int ATTR_NAME_ARRAY_SIZE = 16;
    public final static int FORMAT_ASCII = 0;

    public final static int FORMAT_ISO_8859_1 = 1;
    public final static int FORMAT_ISO_8859_10 = 11;
    public final static int FORMAT_ISO_8859_11 = 12;
    public final static int FORMAT_ISO_8859_12 = 13;
    public final static int FORMAT_ISO_8859_13 = 14;
    public final static int FORMAT_ISO_8859_14 = 15;
    public final static int FORMAT_ISO_8859_15 = 16;
    public final static int FORMAT_ISO_8859_16 = 17;
    public final static int FORMAT_ISO_8859_2 = 3;
    public final static int FORMAT_ISO_8859_3 = 4;
    public final static int FORMAT_ISO_8859_4 = 5;
    public final static int FORMAT_ISO_8859_5 = 6;
    public final static int FORMAT_ISO_8859_6 = 7;
    public final static int FORMAT_ISO_8859_7 = 8;
    public final static int FORMAT_ISO_8859_8 = 9;
    public final static int FORMAT_ISO_8859_9 = 10;
    public final static int FORMAT_UTF_16BE = 63;
    public final static int FORMAT_UTF_16LE = 64;

    // encoding format
    public final static int FORMAT_UTF8 = 2;

    public final static int FORMAT_WIN_1250 = 18;
    public final static int FORMAT_WIN_1251 = 19;
    public final static int FORMAT_WIN_1252 = 20;
    public final static int FORMAT_WIN_1253 = 21;
    public final static int FORMAT_WIN_1254 = 22;
    public final static int FORMAT_WIN_1255 = 23;
    public final static int FORMAT_WIN_1256 = 24;
    public final static int FORMAT_WIN_1257 = 25;
    public final static int FORMAT_WIN_1258 = 26;
    public final static int MAX_DEPTH = 254; // maximum depth value
    // max prefix length
    public final static int MAX_PREFIX_LENGTH = (1 << 9) - 1;
    // max Qname length
    public final static int MAX_QNAME_LENGTH = (1 << 11) - 1;
    // max Token length
    public final static int MAX_TOKEN_LENGTH = (1 << 20) - 1;
    private final static int STATE_ATTR_NAME = 3;
    private final static int STATE_ATTR_VAL = 4;
    private final static int STATE_CDATA = 12;
    private final static int STATE_COMMENT = 11;
    private final static int STATE_DEC_ATTR_NAME = 10;
    private final static int STATE_DOC_END = 7; // end of document
    private final static int STATE_DOC_START = 6; // beginning of document
    private final static int STATE_DOCTYPE = 13;
    private final static int STATE_END_COMMENT = 14;
    // comment appear after the last ending tag
    private final static int STATE_END_PI = 15;
    private final static int STATE_END_TAG = 2;
    // internal parser state

    private final static int STATE_LT_SEEN = 0; // encounter the first <
    private final static int STATE_PI_TAG = 8;
    private final static int STATE_PI_VAL = 9;
    private final static int STATE_START_TAG = 1;
    private final static int STATE_TEXT = 5;
    // tag_stack size
    private final static int TAG_STACK_SIZE = 256;
    public final static int TOKEN_ATTR_NAME = 2;
    public final static int TOKEN_ATTR_NS = 3;
    public final static int TOKEN_ATTR_VAL = 4;
    public final static int TOKEN_CDATA_VAL = 11;
    public final static int TOKEN_CHARACTER_DATA = 5;
    public final static int TOKEN_COMMENT = 6;
    public final static int TOKEN_DEC_ATTR_NAME = 9;
    public final static int TOKEN_DEC_ATTR_VAL = 10;
    public final static int TOKEN_DOCUMENT = 13;
    public final static int TOKEN_DTD_VAL = 12;
    public final static int TOKEN_ENDING_TAG = 1;
    public final static int TOKEN_PI_NAME = 7;
    public final static int TOKEN_PI_VAL = 8;
    // private final static int STATE_END_PI_VAL = 17;

    // token type
    public final static int TOKEN_STARTING_TAG = 0;
    private long[] attr_name_array;
    private int attr_count;
    private long[] prefixed_attr_name_array;
    private int[] prefix_URL_array;
    private int prefixed_attr_count;
    protected boolean br; // buffer reuse
    private int ch;
    private int ch_temp;
    private int length1, length2;
    protected int depth;
    // protected int offset_adj; // determine the byte length for ':' for various encoding types

    protected int docLen;
    protected int docOffset;
    protected int encoding;
    // again, in terms of byte, not char as encoded in VTD
    protected int endOffset;
    private int increment;
    protected FastLongBuffer l1Buffer;
    protected FastLongBuffer l2Buffer;
    protected FastIntBuffer l3Buffer;
    protected FastLongBuffer _l3Buffer;
    protected FastLongBuffer _l4Buffer;
    protected FastIntBuffer _l5Buffer;

    protected FastIntBuffer nsBuffer1;
    protected FastLongBuffer nsBuffer2;
    protected FastLongBuffer nsBuffer3;
    protected long currentElementRecord;

    private int last_depth;
    private int last_l1_index;
    private int last_l2_index;

    // namespace aware flag
    protected boolean ns, is_ns;
    protected int offset; // this is byte offset, not char offset as encoded in VTD
    protected boolean ws; // to prserve whitespace or not, default to false

    protected int prev_offset;
    protected IReader r;
    protected int rootIndex;
    protected long[] tag_stack;
    private int temp_offset;
    protected FastLongBuffer VTDBuffer;
    protected int VTDDepth; // Maximum Depth of VTDs
    protected char[] XMLDoc;
    protected EOFException e;

    protected short LcDepth;
    protected boolean singleByteEncoding;

    /**
     * VTDGen constructor method.
     */
    public VTDGenEx() {
        attr_name_array = new long[ATTR_NAME_ARRAY_SIZE];
        prefixed_attr_name_array = new long[ATTR_NAME_ARRAY_SIZE];
        prefix_URL_array = new int[ATTR_NAME_ARRAY_SIZE];
        tag_stack = new long[TAG_STACK_SIZE];

        VTDDepth = 0;
        LcDepth = 3;

        br = false;
        e = new EOFException("permature EOF reached, XML document incomplete");
        ws = false;
        nsBuffer1 = new FastIntBuffer(4);
        nsBuffer2 = new FastLongBuffer(4);
        nsBuffer3 = new FastLongBuffer(4);
        currentElementRecord = 0;
        singleByteEncoding = true;
    }

    /**
     * Clear internal states so VTDGEn can process the next file.
     */
    public void clear() {
        if (br == false) {
            VTDBuffer = null;
            l1Buffer = null;
            l2Buffer = null;
            l3Buffer = null;
            _l3Buffer = null;
            _l4Buffer = null;
            _l5Buffer = null;
        }
        XMLDoc = null;
        offset = temp_offset = 0;
        last_depth = last_l1_index = last_l2_index = 0;
        rootIndex = 0;
        depth = -1;
        increment = 1;
        ch = ch_temp = 0;
        nsBuffer1.size = 0;
        nsBuffer2.size = 0;
        nsBuffer3.size = 0;
        currentElementRecord = 0;
    }

    /**
     * Enable the parser to collect all white spaces, including the trivial white spaces
     * By default, trivial white spaces are ignored
     * 
     * @param b
     */
    public void enableIgnoredWhiteSpace(final boolean b) {
        ws = b;
    }

    /**
     * Set the XMLDoc container. Also set the offset and len of the document
     * with respect to the container.
     * 
     * @param ba
     *            byte[]
     * @param os
     *            int (in byte)
     * @param len
     *            int (in byte)
     */
    public void setDoc(final char[] ba, final int os, final int len) {
        if (ba == null || os < 0 || len == 0 || ba.length < os + len) {
            throw new IllegalArgumentException("Illegal argument for setDoc");
        }
        int a;
        br = false;
        depth = -1;
        increment = 1;
        ch = ch_temp = 0;
        temp_offset = 0;
        XMLDoc = ba;
        docOffset = offset = os;
        docLen = len;
        endOffset = os + len;
        last_l1_index = last_l2_index = last_depth = 0;

        currentElementRecord = 0;
        nsBuffer1.size = 0;
        nsBuffer2.size = 0;
        nsBuffer3.size = 0;
        r = new CharReader();
        /* if (shallowDepth) */{
            int i1 = 8, i2 = 9, i3 = 11;
            if (docLen <= 1024) {
                // a = 1024; //set the floor
                a = 6;
                i1 = 5;
                i2 = 5;
                i3 = 5;
            } else if (docLen <= 4096) {
                a = 7;
                i1 = 6;
                i2 = 6;
                i3 = 6;
            } else if (docLen <= 1024 * 16) {
                a = 8;
                i1 = 7;
                i2 = 7;
                i3 = 7;
            } else if (docLen <= 1024 * 16 * 4) {
                // a = 2048;
                a = 11;
            } else if (docLen <= 1024 * 256) {
                // a = 1024 * 4;
                a = 12;
            } else {
                // a = 1 << 15;
                a = 15;
            }

            VTDBuffer = new FastLongBuffer(a, len >> (a + 1));
            l1Buffer = new FastLongBuffer(i1);
            l2Buffer = new FastLongBuffer(i2);
            l3Buffer = new FastIntBuffer(i3);
        }
    }

    /**
     * Generating VTD tokens and Location cache info. When set to true,
     * VTDGen conforms to XML namespace 1.0 spec
     * 
     * @param NS
     *            boolean Enable namespace or not
     * @throws ParseException
     *             Super class for any exceptions during parsing.
     * @throws EOFException
     *             End of file exception.
     * @throws EntityException
     *             Entity resolution exception.
     * @throws EncodingException
     *             UTF/native encoding exception.
     */
    public void parse(final boolean NS) throws EncodingException, EOFException, EntityException, ParseException {

        // define internal variables
        ns = NS;
        length1 = length2 = 0;
        attr_count = prefixed_attr_count = 0 /* , ch = 0, ch_temp = 0 */;
        int parser_state = STATE_DOC_START;
        // boolean has_amp = false;
        is_ns = false;
        encoding = FORMAT_UTF8;
        boolean helper = false;
        boolean default_ns = false; // true xmlns='abc'
        boolean isXML = false; // true only for xmlns:xml
        singleByteEncoding = true;

        // enter the main finite state machine
        try {
            _writeVTD(0, 0, TOKEN_DOCUMENT, depth);
            while (true) {
                switch (parser_state) {
                    case STATE_LT_SEEN: // if (depth < -1)
                        // throw new ParseException("Other Errors: Invalid depth");
                        temp_offset = offset;
                        ch = r.getChar();
                        if (XMLChar.isNameStartChar(ch)) {
                            depth++;
                            parser_state = STATE_START_TAG;
                        } else {
                            switch (ch) {
                                case '/':
                                    parser_state = STATE_END_TAG;
                                    break;
                                case '?':
                                    parser_state = process_qm_seen();
                                    break;
                                case '!': // three possibility (comment, CDATA, DOCTYPE)
                                    parser_state = process_ex_seen();
                                    break;
                                default:
                                    throw new ParseException("Other Error: Invalid char after <" + formatLineNumber());
                            }
                        }
                        break;

                    case STATE_START_TAG: // name space is handled by
                        do {
                            ch = r.getChar();
                            if (XMLChar.isNameChar(ch)) {
                                if (ch == ':') {
                                    length2 = offset - temp_offset - increment;
                                    if (ns && checkPrefix2(temp_offset, length2)) {
                                        throw new ParseException("xmlns can't be an element prefix "
                                                + formatLineNumber(offset));
                                    }
                                }
                            } else {
                                break;
                            }
                            ch = r.getChar();
                            if (XMLChar.isNameChar(ch)) {
                                if (ch == ':') {
                                    length2 = offset - temp_offset - increment;
                                    if (ns && checkPrefix2(temp_offset, length2)) {
                                        throw new ParseException("xmlns can't be an element prefix "
                                                + formatLineNumber(offset));
                                    }
                                }
                            } else {
                                break;
                            }
                        } while (true);
                        length1 = offset - temp_offset - increment;
                        if (depth > MAX_DEPTH) {
                            throw new ParseException("Other Error: Depth exceeds MAX_DEPTH" + formatLineNumber());
                        }
                        // writeVTD(offset, TOKEN_STARTING_TAG, length2:length1, depth)
                        final long x = ((long) length1 << 32) + temp_offset;
                        tag_stack[depth] = x;

                        // System.out.println(
                        // " " + (temp_offset) + " " + length2 + ":" + length1 + " startingTag " + depth);
                        if (depth > VTDDepth) {
                            VTDDepth = depth;
                        }
                        // if (encoding < FORMAT_UTF_16BE){
                        if (singleByteEncoding) {
                            if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH) {
                                throw new ParseException(
                                        "Token Length Error: Starting tag prefix or qname length too long"
                                                + formatLineNumber());
                            }
                            writeVTD((temp_offset), (length2 << 11) | length1, TOKEN_STARTING_TAG, depth);
                        } else {
                            if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1)) {
                                throw new ParseException(
                                        "Token Length Error: Starting tag prefix or qname length too long"
                                                + formatLineNumber());
                            }
                            writeVTD((temp_offset) >> 1, (length2 << 10) | (length1 >> 1), TOKEN_STARTING_TAG, depth);
                        }
                        if (ns) {
                            if (length2 != 0) {
                                length2 += increment;
                                currentElementRecord = (((long) ((length2 << 16) | length1)) << 32) | temp_offset;
                            } else {
                                currentElementRecord = 0;
                            }

                            if (depth <= nsBuffer1.size - 1) {
                                nsBuffer1.size = depth;
                                final int t = nsBuffer1.intAt(depth - 1) + 1;
                                nsBuffer2.size = t;
                                nsBuffer3.size = t;
                            }
                        }
                        // offset += length1;
                        length2 = 0;
                        if (XMLChar.isSpaceChar(ch)) {
                            ch = getCharAfterS();
                            if (XMLChar.isNameStartChar(ch)) {
                                // seen an attribute here
                                temp_offset = getPrevOffset();
                                parser_state = STATE_ATTR_NAME;
                                break;
                            }
                        }
                        helper = true;
                        if (ch == '/') {
                            depth--;
                            helper = false;
                            ch = r.getChar();
                        }
                        if (ch == '>') {
                            if (ns) {
                                nsBuffer1.append(nsBuffer3.size - 1);
                                if (currentElementRecord != 0) {
                                    qualifyElement();
                                }
                            }

                            // parser_state = processElementTail(helper);
                            if (depth != -1) {
                                temp_offset = offset;
                                // ch = getCharAfterSe(); // consume WSs
                                ch = getCharAfterS(); // consume WSs
                                if (ch == '<') {
                                    if (ws) {
                                        addWhiteSpaceRecord();
                                    }
                                    parser_state = STATE_LT_SEEN;
                                    if (r.skipChar('/')) {
                                        if (helper) {
                                            length1 = offset - temp_offset - (increment << 1);
                                            // if (length1 > 0) {
                                            // if (encoding < FORMAT_UTF_16BE)
                                            if (singleByteEncoding) {
                                                writeVTDText((temp_offset), length1, TOKEN_CHARACTER_DATA, depth);
                                            } else {
                                                writeVTDText((temp_offset) >> 1, (length1 >> 1), TOKEN_CHARACTER_DATA,
                                                        depth);
                                                // }
                                            }
                                        }
                                        parser_state = STATE_END_TAG;
                                        break;
                                    }
                                } else if (XMLChar.isContentChar(ch)) {
                                    // temp_offset = offset;
                                    parser_state = STATE_TEXT;
                                } else {
                                    parser_state = STATE_TEXT;
                                    handleOtherTextChar2(ch);
                                }
                            } else {
                                parser_state = STATE_DOC_END;
                            }
                            break;
                        }
                        throw new ParseException("Starting tag Error: Invalid char in starting tag"
                                + formatLineNumber());

                    case STATE_END_TAG:
                        temp_offset = offset;
                        final int sos = (int) tag_stack[depth];
                        final int sl = (int) (tag_stack[depth] >> 32);

                        offset = temp_offset + sl;

                        if (offset >= endOffset) {
                            throw new EOFException("permature EOF reached, XML document incomplete");
                        }
                        for (int i = 0; i < sl; i++) {
                            if (XMLDoc[sos + i] != XMLDoc[temp_offset + i]) {
                                throw new ParseException("Ending tag error: Start/ending tag mismatch"
                                        + formatLineNumber());
                            }
                        }
                        depth--;
                        ch = getCharAfterS();
                        if (ch != '>') {
                            throw new ParseException("Ending tag error: Invalid char in ending tag "
                                    + formatLineNumber());
                        }

                        if (depth != -1) {
                            temp_offset = offset;
                            ch = getCharAfterS();
                            if (ch == '<') {
                                if (ws) {
                                    addWhiteSpaceRecord();
                                }
                                parser_state = STATE_LT_SEEN;
                            } else if (XMLChar.isContentChar(ch)) {
                                parser_state = STATE_TEXT;
                            } else {
                                handleOtherTextChar2(ch);
                                parser_state = STATE_TEXT;
                            }
                        } else {
                            parser_state = STATE_DOC_END;
                        }
                        break;

                    case STATE_ATTR_NAME:

                        if (ch == 'x') {
                            if (r.skipChar('m') && r.skipChar('l') && r.skipChar('n') && r.skipChar('s')) {
                                ch = r.getChar();
                                if (ch == '=' || XMLChar.isSpaceChar(ch)) {
                                    is_ns = true;
                                    default_ns = true;
                                } else if (ch == ':') {
                                    is_ns = true; // break;
                                    default_ns = false;
                                }
                            }
                        }
                        do {
                            if (XMLChar.isNameChar(ch)) {
                                if (ch == ':') {
                                    length2 = offset - temp_offset - increment;
                                }
                            } else {
                                break;
                            }
                            ch = r.getChar();
                        } while (true);
                        length1 = getPrevOffset() - temp_offset;
                        if (is_ns && ns) {
                            // make sure postfix isn't xmlns
                            if (!default_ns) {
                                if (increment == 1 && (length1 - length2 - 1 == 5)
                                        || (increment == 2 && (length1 - length2 - 2 == 10))) {
                                    disallow_xmlns(temp_offset + length2 + increment);
                                }

                                // if the post fix is xml, signal it
                                if (increment == 1 && (length1 - length2 - 1 == 3)
                                        || (increment == 2 && (length1 - length2 - 2 == 6))) {
                                    isXML = matchXML(temp_offset + length2 + increment);
                                }
                            }
                        }
                        // check for uniqueness here
                        checkAttributeUniqueness();

                        // after checking, write VTD
                        if (is_ns) { // if the prefix is xmlns: or xmlns
                            // if (encoding < FORMAT_UTF_16BE){
                            if (singleByteEncoding) {
                                if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH) {
                                    throw new ParseException(
                                            "Token length overflow error: Attr NS tag prefix or qname length too long"
                                                    + formatLineNumber());
                                }
                                _writeVTD(temp_offset, (length2 << 11) | length1, TOKEN_ATTR_NS, depth);
                            } else {
                                if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1)) {
                                    throw new ParseException(
                                            "Token length overflow error: Attr NS prefix or qname length too long"
                                                    + formatLineNumber());
                                }
                                _writeVTD(temp_offset >> 1, (length2 << 10) | (length1 >> 1), TOKEN_ATTR_NS, depth);
                            }
                            // append to nsBuffer2
                            if (ns) {
                                // unprefixed xmlns are not recorded
                                if (length2 != 0 && !isXML) {
                                    // nsBuffer2.append(VTDBuffer.size() - 1);
                                    final long l = ((long) ((length2 << 16) | length1)) << 32 | temp_offset;
                                    nsBuffer3.append(l); // byte offset and byte
                                    // length
                                }
                            }

                        } else {
                            // if (encoding < FORMAT_UTF_16BE){
                            if (singleByteEncoding) {
                                if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH) {
                                    throw new ParseException(
                                            "Token Length Error: Attr name prefix or qname length too long"
                                                    + formatLineNumber());
                                }
                                _writeVTD(temp_offset, (length2 << 11) | length1, TOKEN_ATTR_NAME, depth);
                            } else {
                                if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1)) {
                                    throw new ParseException(
                                            "Token Length overflow error: Attr name prefix or qname length too long"
                                                    + formatLineNumber());
                                }
                                _writeVTD(temp_offset >> 1, (length2 << 10) | (length1 >> 1), TOKEN_ATTR_NAME, depth);
                            }
                        }
                        /*
                         * System.out.println(
                         * " " + temp_offset + " " + length2 + ":" + length1 + " attr name " + depth);
                         */
                        length2 = 0;
                        if (XMLChar.isSpaceChar(ch)) {
                            ch = getCharAfterS();
                        }
                        if (ch != '=') {
                            throw new ParseException("Error in attr: invalid char" + formatLineNumber());
                        }
                        ch_temp = getCharAfterS();
                        if (ch_temp != '"' && ch_temp != '\'') {
                            throw new ParseException("Error in attr: invalid char (should be ' or \" )"
                                    + formatLineNumber());
                        }
                        temp_offset = offset;
                        parser_state = STATE_ATTR_VAL;
                        break;

                    case STATE_ATTR_VAL:
                        do {
                            ch = r.getChar();
                            if (XMLChar.isValidChar(ch) && ch != '<') {
                                if (ch == ch_temp) {
                                    break;
                                }
                                if (ch == '&') {
                                    final int startOfEntityBody = offset;
                                    final int entity = entityIdentifier();
                                    if (!XMLChar.isValidChar(entity)) {
                                        throw new ParseException("Error in attr: Invalid XML char" + formatLineNumber());
                                    }
                                    XMLDoc[startOfEntityBody - 1] = (char) entity;
                                    for (int i = startOfEntityBody; i < offset; i++) {
                                        XMLDoc[i] = 0;
                                    }
                                    // as in vtd spec, we mark attr val with entities
                                }
                            } else {
                                throw new ParseException("Error in attr: Invalid XML char" + formatLineNumber());
                            }
                        } while (true);

                        length1 = offset - temp_offset - increment;
                        if (ns && is_ns) {
                            if (!default_ns && length1 == 0) {
                                throw new ParseException(" non-default ns URL can't be empty" + formatLineNumber());
                            }
                            // identify nsURL return 0,1,2
                            final int t = identifyNsURL(temp_offset, length1);
                            if (isXML) {// xmlns:xml
                                if (t != 1) {
                                    // URL points to "http://www.w3.org/XML/1998/namespace"
                                    throw new ParseException("xmlns:xml can only point to"
                                            + "\"http://www.w3.org/XML/1998/namespace\"" + formatLineNumber());
                                }

                            } else {
                                if (!default_ns) {
                                    nsBuffer2.append(((long) temp_offset << 32) | length1);
                                }
                                if (t != 0) {
                                    if (t == 1) {
                                        throw new ParseException("namespace declaration can't point to"
                                                + " \"http://www.w3.org/XML/1998/namespace\"" + formatLineNumber());
                                    }
                                    throw new ParseException("namespace declaration can't point to"
                                            + " \"http://www.w3.org/2000/xmlns/\"" + formatLineNumber());
                                }
                            }
                            // no ns URL points to
                            // "http://www.w3.org/2000/xmlns/"

                            // no ns URL points to
                            // "http://www.w3.org/XML/1998/namespace"
                        }

                        if (singleByteEncoding) {
                            // if (encoding < FORMAT_UTF_16BE){
                            if (length1 > MAX_TOKEN_LENGTH) {
                                throw new ParseException("Token Length Error:" + " Attr val too long (>0xfffff)"
                                        + formatLineNumber());
                            }
                            _writeVTD(temp_offset, length1, TOKEN_ATTR_VAL, depth);
                        } else {
                            if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                                throw new ParseException("Token Length Error:" + " Attr val too long (>0xfffff)"
                                        + formatLineNumber());
                            }
                            _writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_ATTR_VAL, depth);
                        }

                        isXML = false;
                        is_ns = false;

                        ch = r.getChar();
                        if (XMLChar.isSpaceChar(ch)) {
                            ch = getCharAfterS();
                            if (XMLChar.isNameStartChar(ch)) {
                                temp_offset = offset - increment;
                                parser_state = STATE_ATTR_NAME;
                                break;
                            }
                        }

                        helper = true;
                        if (ch == '/') {
                            depth--;
                            helper = false;
                            ch = r.getChar();
                        }

                        if (ch == '>') {
                            if (ns) {
                                nsBuffer1.append(nsBuffer3.size - 1);
                                if (prefixed_attr_count > 0) {
                                    qualifyAttributes();
                                }
                                if (prefixed_attr_count > 1) {
                                    checkQualifiedAttributeUniqueness();
                                }
                                if (currentElementRecord != 0) {
                                    qualifyElement();
                                }
                                prefixed_attr_count = 0;
                            }
                            attr_count = 0;
                            // parser_state = processElementTail(helper);
                            if (depth != -1) {
                                temp_offset = offset;
                                // ch = getCharAfterSe();
                                ch = getCharAfterS();

                                if (ch == '<') {
                                    if (ws) {
                                        addWhiteSpaceRecord();
                                    }
                                    parser_state = STATE_LT_SEEN;
                                    if (r.skipChar('/')) {
                                        if (helper) {
                                            length1 = offset - temp_offset - (increment << 1);
                                            // if (length1 > 0) {
                                            if (singleByteEncoding) {
                                                writeVTDText((temp_offset), length1, TOKEN_CHARACTER_DATA, depth);
                                            } else {
                                                writeVTDText((temp_offset) >> 1, (length1 >> 1), TOKEN_CHARACTER_DATA,
                                                        depth);
                                                // }
                                            }
                                        }
                                        parser_state = STATE_END_TAG;
                                        break;
                                    }
                                } else if (XMLChar.isContentChar(ch)) {
                                    // temp_offset = offset;
                                    parser_state = STATE_TEXT;
                                } else {
                                    handleOtherTextChar2(ch);
                                    parser_state = STATE_TEXT;
                                }
                            } else {
                                parser_state = STATE_DOC_END;
                            }
                            break;
                        }

                        throw new ParseException("Starting tag Error: Invalid char in starting tag"
                                + formatLineNumber());

                    case STATE_TEXT:
                        if (depth == -1) {
                            throw new ParseException("Error in text content: Char data at the wrong place"
                                    + formatLineNumber());
                        }
                        do {
                            ch = r.getChar();
                            // System.out.println(""+(char)ch);
                            if (XMLChar.isContentChar(ch)) {
                            } else if (ch == '<') {
                                break;
                            } else {
                                handleOtherTextChar(ch);
                            }
                            ch = r.getChar();
                            if (XMLChar.isContentChar(ch)) {
                            } else if (ch == '<') {
                                break;
                            } else {
                                handleOtherTextChar(ch);
                            }
                        } while (true);

                        length1 = offset - increment - temp_offset;

                        if (singleByteEncoding) {
                            writeVTDText(temp_offset, length1, TOKEN_CHARACTER_DATA, depth);
                        } else {
                            writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CHARACTER_DATA, depth);
                        }

                        // has_amp = true;
                        parser_state = STATE_LT_SEEN;
                        break;
                    case STATE_DOC_START:
                        parser_state = process_start_doc();
                        break;
                    case STATE_DOC_END:
                        // docEnd = true;
                        parser_state = process_end_doc();
                        break;
                    case STATE_PI_TAG:
                        parser_state = process_pi_tag();
                        break;
                    // throw new ParseException("Error in PI: Invalid char");
                    case STATE_PI_VAL:
                        parser_state = process_pi_val();
                        break;

                    case STATE_DEC_ATTR_NAME:
                        parser_state = process_dec_attr();
                        break;

                    case STATE_COMMENT:
                        parser_state = process_comment();
                        break;

                    case STATE_CDATA:
                        parser_state = process_cdata();
                        break;

                    case STATE_DOCTYPE:
                        parser_state = process_doc_type();
                        break;

                    case STATE_END_COMMENT:
                        parser_state = process_end_comment();
                        break;

                    case STATE_END_PI:
                        parser_state = process_end_pi();
                        break;

                    default:
                        throw new ParseException("Other error: invalid parser state" + formatLineNumber());
                }
            }
        } catch (final EOFException e) {
            if (parser_state != STATE_DOC_END) {
                throw e;
            }
            finishUp();
        }
    }

    /**
     * Write white space records that are ignored by default
     */
    private void addWhiteSpaceRecord() {
        if (depth > -1) {
            final int length1 = offset - increment - temp_offset;
            if (length1 != 0) {
                if (singleByteEncoding) {
                    writeVTDText(temp_offset, length1, TOKEN_CHARACTER_DATA, depth);
                } else {
                    writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CHARACTER_DATA, depth);
                }
            }
        }
    }

    /**
     * This method will detect whether the entity is valid or not and increment offset.
     * 
     * @return int
     * @throws com.ximpleware.ParseException
     *             Super class for any exception during parsing.
     * @throws com.ximpleware.EncodingException
     *             UTF/native encoding exception.
     * @throws com.ximpleware.EOFException
     *             End of file exception.
     */
    private int entityIdentifier() throws EntityException, EncodingException, EOFException, ParseException {
        int ch = r.getChar();
        int val = 0;

        switch (ch) {
            case '#':
                ch = r.getChar();
                if (ch == 'x') {
                    while (true) {
                        ch = r.getChar();
                        if (ch >= '0' && ch <= '9') {
                            val = (val << 4) + (ch - '0');
                        } else if (ch >= 'a' && ch <= 'f') {
                            val = (val << 4) + (ch - 'a' + 10);
                        } else if (ch >= 'A' && ch <= 'F') {
                            val = (val << 4) + (ch - 'A' + 10);
                        } else if (ch == ';') {
                            return val;
                        } else {
                            throw new EntityException("Errors in char reference: Illegal char following &#x.");
                        }
                    }
                } else {
                    while (true) {
                        if (ch >= '0' && ch <= '9') {
                            val = val * 10 + (ch - '0');
                        } else if (ch == ';') {
                            break;
                        } else {
                            throw new EntityException("Errors in char reference: Illegal char following &#.");
                        }
                        ch = r.getChar();
                    }
                }
                if (!XMLChar.isValidChar(val)) {
                    throw new EntityException("Errors in entity reference: Invalid XML char.");
                }
                return val;
                // break;

            case 'a':
                ch = r.getChar();
                if (ch == 'm') {
                    if (r.getChar() == 'p' && r.getChar() == ';') {
                        // System.out.println(" entity for &");
                        return '&';
                    } else {
                        throw new EntityException("Errors in Entity: Illegal builtin reference");
                    }
                } else if (ch == 'p') {
                    if (r.getChar() == 'o' && r.getChar() == 's' && r.getChar() == ';') {
                        // System.out.println(" entity for ' ");
                        return '\'';
                    } else {
                        throw new EntityException("Errors in Entity: Illegal builtin reference");
                    }
                } else {
                    throw new EntityException("Errors in Entity: Illegal builtin reference");
                }

            case 'q':
                if (r.getChar() == 'u' && r.getChar() == 'o' && r.getChar() == 't' && r.getChar() == ';') {
                    return '"';
                } else {
                    throw new EntityException("Errors in Entity: Illegal builtin reference");
                }
            case 'l':
                if (r.getChar() == 't' && r.getChar() == ';') {
                    return '<';
                } else {
                    throw new EntityException("Errors in Entity: Illegal builtin reference");
                }
                // break;
            case 'g':
                if (r.getChar() == 't' && r.getChar() == ';') {
                    return '>';
                } else {
                    throw new EntityException("Errors in Entity: Illegal builtin reference");
                }
            default:
                throw new EntityException("Errors in Entity: Illegal entity char");
        }
        // return val;
    }

    /**
     * Write the remaining portion of LC info
     * 
     */
    private void finishUp() {
        if (last_depth == 1) {
            l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
        } else if (last_depth == 2) {
            l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
        }
    }

    /**
     * Format the string indicating the position (line number:offset)of the offset if
     * there is an exception.
     * 
     * @return java.lang.String indicating the line number and offset of the exception
     */
    private String formatLineNumber() {
        return formatLineNumber(offset);
    }

    private String formatLineNumber(final int os) {
        int so = docOffset;
        int lineNumber = 0;
        int lineOffset = 0;

        if (encoding < FORMAT_UTF_16BE) {
            while (so <= os - 1) {
                if (XMLDoc[so] == '\n') {
                    lineNumber++;
                    lineOffset = so;
                }
                // lineOffset++;
                so++;
            }
            lineOffset = os - lineOffset;
        } else if (encoding == FORMAT_UTF_16BE) {
            while (so <= os - 2) {
                if (XMLDoc[so + 1] == '\n' && XMLDoc[so] == 0) {
                    lineNumber++;
                    lineOffset = so;
                }
                so += 2;
            }
            lineOffset = (os - lineOffset) >> 1;
        } else {
            while (so <= os - 2) {
                if (XMLDoc[so] == '\n' && XMLDoc[so + 1] == 0) {
                    lineNumber++;
                    lineOffset = so;
                }
                so += 2;
            }
            lineOffset = (os - lineOffset) >> 1;
        }
        return "\nLine Number: " + (lineNumber + 1) + " Offset: " + (lineOffset - 1);
    }

    /**
     * The entity ignorant version of getCharAfterS.
     * 
     * @return int
     * @throws ParseException
     * @throws EncodingException
     * @throws com.ximpleware.EOFException
     */
    final private int getCharAfterS() throws ParseException, EncodingException, EOFException {
        int n;

        do {
            n = r.getChar();
            if ((n == ' ' || n == '\n' || n == '\t' || n == '\r')) {
                // if (XMLChar.isSpaceChar(n) ) {
            } else {
                return n;
            }
            n = r.getChar();
            if ((n == ' ' || n == '\n' || n == '\t' || n == '\r')) {
            } else {
                return n;
                /*
                 * if (n == ' ' || n == '\n' || n =='\t'|| n == '\r' ) {
                 * } else
                 * return n;
                 */
            }
        } while (true);
        // throw new EOFException("should never come here");
    }

    /**
     * This method returns the VTDNav object after parsing, it also cleans
     * internal state so VTDGen can process the next file.
     * 
     * @return com.ximpleware.VTDNav
     */
    public VTDNavEx getNav() {
        // call VTDNav constructor
        final VTDNavEx vn = new VTDNavEx(rootIndex, encoding, ns, VTDDepth, XMLDoc, VTDBuffer, l1Buffer, l2Buffer,
                l3Buffer, docOffset, docLen);
        clear();
        return vn;
    }

    /**
     * Get the offset value of previous character.
     * 
     * @return int
     * @throws ParseException
     *             Super class for exceptions during parsing.
     */
    private int getPrevOffset() throws ParseException {
        int prevOffset = offset;
        int temp;
        switch (encoding) {
            case FORMAT_UTF8:
                do {
                    prevOffset--;
                } while (XMLDoc[prevOffset] < 0 && ((XMLDoc[prevOffset] & (byte) 0xc0) == (byte) 0x80));
                return prevOffset;
            case FORMAT_ASCII:
            case FORMAT_ISO_8859_1:
            case FORMAT_ISO_8859_2:
            case FORMAT_ISO_8859_3:
            case FORMAT_ISO_8859_4:
            case FORMAT_ISO_8859_5:
            case FORMAT_ISO_8859_6:
            case FORMAT_ISO_8859_7:
            case FORMAT_ISO_8859_8:
            case FORMAT_ISO_8859_9:
            case FORMAT_ISO_8859_10:
            case FORMAT_ISO_8859_11:
            case FORMAT_ISO_8859_13:
            case FORMAT_ISO_8859_14:
            case FORMAT_ISO_8859_15:
            case FORMAT_WIN_1250:
            case FORMAT_WIN_1251:
            case FORMAT_WIN_1252:
            case FORMAT_WIN_1253:
            case FORMAT_WIN_1254:
            case FORMAT_WIN_1255:
            case FORMAT_WIN_1256:
            case FORMAT_WIN_1257:
            case FORMAT_WIN_1258:
                return offset - 1;
            case FORMAT_UTF_16LE:
                temp = (XMLDoc[offset] & 0xff) << 8 | (XMLDoc[offset + 1] & 0xff);
                if (temp < 0xd800 || temp > 0xdfff) {
                    return offset - 2;
                } else {
                    return offset - 4;
                }
            case FORMAT_UTF_16BE:
                temp = (XMLDoc[offset] & 0xff) << 8 | (XMLDoc[offset + 1] & 0xff);
                if (temp < 0xd800 || temp > 0xdfff) {
                    return offset - 2;
                } else {
                    return offset - 4;
                }
            default:
                throw new ParseException("Other Error: Should never happen");
        }
    }

    private void checkQualifiedAttributeUniqueness() throws ParseException {
        // TODO Auto-generated method stub
        int preLen1, os1, postLen1, URLLen1, URLOs1, preLen2, os2, postLen2, URLLen2, URLOs2, k;
        for (int i = 0; i < prefixed_attr_count; i++) {
            preLen1 = (int) ((prefixed_attr_name_array[i] & 0xffff0000L) >> 16);
            postLen1 = (int) ((prefixed_attr_name_array[i] & 0xffffL)) - preLen1 - increment;
            os1 = (int) (prefixed_attr_name_array[i] >> 32) + preLen1 + increment;
            URLLen1 = nsBuffer2.lower32At(prefix_URL_array[i]);
            URLOs1 = nsBuffer2.upper32At(prefix_URL_array[i]);
            for (int j = i + 1; j < prefixed_attr_count; j++) {
                // prefix of i matches that of j
                preLen2 = (int) ((prefixed_attr_name_array[j] & 0xffff0000L) >> 16);
                postLen2 = (int) ((prefixed_attr_name_array[j] & 0xffffL)) - preLen2 - increment;
                os2 = (int) (prefixed_attr_name_array[j] >> 32) + preLen2 + increment;
                // System.out.println(new String(XMLDoc,os1, postLen1)
                // +" "+ new String(XMLDoc, os2, postLen2));
                if (postLen1 == postLen2) {
                    k = 0;
                    for (; k < postLen1; k++) {
                        // System.out.println(i+" "+(char)(XMLDoc[os+k])+"<===>"+(char)(XMLDoc[preOs+k]));
                        if (XMLDoc[os1 + k] != XMLDoc[os2 + k]) {
                            break;
                        }
                    }
                    if (k == postLen1) {
                        // found the match
                        URLLen2 = nsBuffer2.lower32At(prefix_URL_array[j]);
                        URLOs2 = nsBuffer2.upper32At(prefix_URL_array[j]);
                        // System.out.println(" URLOs1 ===>" + URLOs1);
                        // System.out.println("nsBuffer2 ===>"+nsBuffer2.longAt(i)+" i==>"+i);
                        // System.out.println("URLLen2 "+ URLLen2+" URLLen1 "+ URLLen1+" ");
                        if (matchURL(URLOs1, URLLen1, URLOs2, URLLen2)) {
                            throw new ParseException(" qualified attribute names collide " + formatLineNumber(os2));
                        }
                    }
                }
            }
            // System.out.println("======");
        }
    }

    private void qualifyAttributes() throws ParseException {
        final int i1 = nsBuffer3.size - 1;
        int j = 0, i = 0;
        // two cases:
        // 1. the current element has no prefix, look for xmlns
        // 2. the current element has prefix, look for xmlns:something
        while (j < prefixed_attr_count) {
            final int preLen = (int) ((prefixed_attr_name_array[j] & 0xffff0000L) >> 16);
            final int preOs = (int) (prefixed_attr_name_array[j] >> 32);
            // System.out.println(new String(XMLDoc, preOs, preLen)+"===");
            i = i1;
            while (i >= 0) {
                final int t = nsBuffer3.upper32At(i);
                // with prefix, get full length and prefix length
                if ((t & 0xffff) - (t >> 16) == preLen + increment) {
                    // doing byte comparison here
                    final int os = nsBuffer3.lower32At(i) + (t >> 16) + increment;
                    // System.out.println(new String(XMLDoc, os, preLen)+"");
                    int k = 0;
                    for (; k < preLen; k++) {
                        // System.out.println(i+" "+(char)(XMLDoc[os+k])+"<===>"+(char)(XMLDoc[preOs+k]));
                        if (XMLDoc[os + k] != XMLDoc[preOs + k]) {
                            break;
                        }
                    }
                    if (k == preLen) {
                        break; // found the match
                    }
                }
                /*
                 * if ( (nsBuffer3.upper32At(i) & 0xffff0000) == 0){
                 * return;
                 * }
                 */
                i--;
            }
            if (i < 0) {
                throw new ParseException("Name space qualification Exception: prefixed attribute not qualified\n"
                        + formatLineNumber(preOs));
            } else {
                prefix_URL_array[j] = i;
            }
            j++;
            // no need to check if xml is the prefix
        }
    }

    // return 0, 1 or 2
    private int identifyNsURL(final int byte_offset, final int length) {
        // TODO Auto-generated method stub
        // URL points to "http://www.w3.org/XML/1998/namespace" return 1
        // URL points to "http://www.w3.org/2000/xmlns/" return 2
        final String URL1 = "2000/xmlns/";
        final String URL2 = "http://www.w3.org/XML/1998/namespace";
        long l;
        int i, t;
        final int g = byte_offset + length;
        int os = byte_offset;
        if (length < 29 || (increment == 2 && length < 58)) {
            return 0;
        }

        for (i = 0; i < 18 && os < g; i++) {
            l = _getCharResolved(os);
            // System.out.println("char ==>"+(char)l);
            if (URL2.charAt(i) != (int) l) {
                return 0;
            }
            os += (int) (l >> 32);
        }

        // store offset value
        t = os;

        for (i = 0; i < 11 && os < g; i++) {
            l = _getCharResolved(os);
            if (URL1.charAt(i) != (int) l) {
                break;
            }
            os += (int) (l >> 32);
        }
        if (os == g) {
            return 2;
        }

        // so far a match
        os = t;
        for (i = 18; i < 36 && os < g; i++) {
            l = _getCharResolved(os);
            if (URL2.charAt(i) != (int) l) {
                return 0;
            }
            os += (int) (l >> 32);
        }
        if (os == g) {
            return 1;
        }

        return 0;
    }

    private boolean matchXML(final int byte_offset) {
        // TODO Auto-generated method stub
        if (encoding < FORMAT_UTF_16BE) {
            if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 'm' && XMLDoc[byte_offset + 2] == 'l') {
                return true;
            }
        } else {
            if (encoding == FORMAT_UTF_16LE) {
                if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 0 && XMLDoc[byte_offset + 2] == 'm'
                        && XMLDoc[byte_offset + 3] == 0 && XMLDoc[byte_offset + 4] == 'l'
                        && XMLDoc[byte_offset + 5] == 0) {
                    return true;
                }
            } else {
                if (XMLDoc[byte_offset] == 0 && XMLDoc[byte_offset + 1] == 'x' && XMLDoc[byte_offset + 2] == 0
                        && XMLDoc[byte_offset + 3] == 'm' && XMLDoc[byte_offset + 4] == 0
                        && XMLDoc[byte_offset + 5] == 'l') {
                    return true;
                }
            }
        }
        return false;
    }

    private void disallow_xmlns(final int byte_offset) throws ParseException {
        // TODO Auto-generated method stub
        if (encoding < FORMAT_UTF_16BE) {
            if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 'm' && XMLDoc[byte_offset + 2] == 'l'
                    && XMLDoc[byte_offset + 3] == 'n' && XMLDoc[byte_offset + 4] == 's') {
                throw new ParseException("xmlns as a ns prefix can't be re-declared" + formatLineNumber(byte_offset));
            }

        } else {
            if (encoding == FORMAT_UTF_16LE) {
                if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 0 && XMLDoc[byte_offset + 2] == 'm'
                        && XMLDoc[byte_offset + 3] == 0 && XMLDoc[byte_offset + 4] == 'l'
                        && XMLDoc[byte_offset + 5] == 0 && XMLDoc[byte_offset + 6] == 'n'
                        && XMLDoc[byte_offset + 7] == 0 && XMLDoc[byte_offset + 8] == 's'
                        && XMLDoc[byte_offset + 9] == 0) {
                    throw new ParseException("xmlns as a ns prefix can't be re-declared"
                            + formatLineNumber(byte_offset));
                }
            } else {
                if (XMLDoc[byte_offset] == 0 && XMLDoc[byte_offset + 1] == 'x' && XMLDoc[byte_offset + 2] == 0
                        && XMLDoc[byte_offset + 3] == 'm' && XMLDoc[byte_offset + 4] == 0
                        && XMLDoc[byte_offset + 5] == 'l' && XMLDoc[byte_offset + 6] == 0
                        && XMLDoc[byte_offset + 7] == 'n' && XMLDoc[byte_offset + 8] == 0
                        && XMLDoc[byte_offset + 9] == 's') {
                    throw new ParseException("xmlns as a ns prefix can't be re-declared"
                            + formatLineNumber(byte_offset));
                }
            }
        }
    }

    // private

    /**
     * This private method processes CDATA section
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_cdata() throws ParseException, EncodingException, EOFException {
        int parser_state;
        while (true) {
            ch = r.getChar();
            if (XMLChar.isValidChar(ch)) {
                if (ch == ']' && r.skipChar(']')) {
                    while (r.skipChar(']')) {
                        ;
                    }
                    if (r.skipChar('>')) {
                        break;
                    } /*
                       * else
                       * throw new ParseException(
                       * "Error in CDATA: Invalid termination sequence"
                       * + formatLineNumber());
                       */
                }
            } else {
                throw new ParseException("Error in CDATA: Invalid Char" + formatLineNumber());
            }
        }
        length1 = offset - temp_offset - (increment << 1) - increment;
        if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){

            writeVTDText(temp_offset, length1, TOKEN_CDATA_VAL, depth);
        } else {

            writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CDATA_VAL, depth);
        }
        // System.out.println(" " + (temp_offset) + " " + length1 + " CDATA " + depth);
        temp_offset = offset;
        // ch = getCharAfterSe();
        ch = getCharAfterS();

        if (ch == '<') {
            if (ws) {
                addWhiteSpaceRecord();
            }
            parser_state = STATE_LT_SEEN;
        } else if (XMLChar.isContentChar(ch)) {
            // temp_offset = offset-1;
            parser_state = STATE_TEXT;
        } else if (ch == '&') {
            final int startOfEntityBody = offset;
            final int entity = entityIdentifier();
            XMLDoc[startOfEntityBody - 1] = (char) entity;
            for (int i = startOfEntityBody; i < offset; i++) {
                XMLDoc[i] = 0;
            }
            parser_state = STATE_TEXT;
            // temp_offset = offset;
        } else if (ch == ']') {
            // temp_offset = offset-1;
            if (r.skipChar(']')) {
                while (r.skipChar(']')) {
                }
                if (r.skipChar('>')) {
                    throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                }
            }
            parser_state = STATE_TEXT;
        } else {
            throw new ParseException("Other Error: Invalid char in xml" + formatLineNumber());
        }
        return parser_state;
    }

    /**
     * This private method process comment
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_comment() throws ParseException, EncodingException, EOFException {
        int parser_state;
        while (true) {
            ch = r.getChar();
            if (XMLChar.isValidChar(ch)) {
                if (ch == '-' && r.skipChar('-')) {
                    length1 = offset - temp_offset - (increment << 1);
                    break;
                }
            } else {
                throw new ParseException("Error in comment: Invalid Char" + formatLineNumber());
            }
        }
        if (r.getChar() == '>') {
            // System.out.println(" " + (temp_offset) + " " + length1 + " comment " + depth);
            if (singleByteEncoding) {
                writeVTDText(temp_offset, length1, TOKEN_COMMENT, depth);
            } else {
                writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_COMMENT, depth);
            }
            // length1 = 0;
            temp_offset = offset;
            // ch = getCharAfterSe();
            ch = getCharAfterS();
            if (ch == '<') {
                if (ws) {
                    addWhiteSpaceRecord();
                }
                parser_state = STATE_LT_SEEN;
            } else if (XMLChar.isContentChar(ch)) {
                // temp_offset = offset;
                parser_state = STATE_TEXT;
            } else if (ch == '&') {
                final int startOfEntityBody = offset;
                final int entity = entityIdentifier();
                XMLDoc[startOfEntityBody - 1] = (char) entity;
                for (int i = startOfEntityBody; i < offset; i++) {
                    XMLDoc[i] = 0;
                }
                parser_state = STATE_TEXT;
            } else if (ch == ']') {
                if (r.skipChar(']')) {
                    while (r.skipChar(']')) {
                    }
                    if (r.skipChar('>')) {
                        throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                    }
                }
                parser_state = STATE_TEXT;
            } else {
                throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
            }
            return parser_state;
        } else {
            throw new ParseException("Error in comment: Invalid terminating sequence" + formatLineNumber());
        }
    }

    /**
     * This private method processes declaration attributes
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_dec_attr() throws ParseException, EncodingException, EOFException {
        int parser_state;
        if (ch == 'v' && r.skipChar('e') && r.skipChar('r') && r.skipChar('s') && r.skipChar('i') && r.skipChar('o')
                && r.skipChar('n')) {
            ch = getCharAfterS();
            if (ch == '=') {
                /*
                 * System.out.println(
                 * " " + (temp_offset - 1) + " " + 7 + " dec attr name version " + depth);
                 */
                if (singleByteEncoding) {
                    _writeVTD(temp_offset - 1, 7, TOKEN_DEC_ATTR_NAME, depth);
                } else {
                    _writeVTD((temp_offset - 2) >> 1, 7, TOKEN_DEC_ATTR_NAME, depth);
                }
            } else {
                throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
            }
        } else {
            throw new ParseException("XML decl error: should be version" + formatLineNumber());
        }
        ch_temp = getCharAfterS();
        if (ch_temp != '\'' && ch_temp != '"') {
            throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
        }
        temp_offset = offset;
        // support 1.0 or 1.1
        if (r.skipChar('1') && r.skipChar('.') && (r.skipChar('0') || r.skipChar('1'))) {
            /*
             * System.out.println(
             * " " + temp_offset + " " + 3 + " dec attr val (version)" + depth);
             */
            if (singleByteEncoding) {
                _writeVTD(temp_offset, 3, TOKEN_DEC_ATTR_VAL, depth);
            } else {
                _writeVTD(temp_offset >> 1, 3, TOKEN_DEC_ATTR_VAL, depth);
            }
        } else {
            throw new ParseException("XML decl error: Invalid version(other than 1.0 or 1.1) detected"
                    + formatLineNumber());
        }
        if (!r.skipChar(ch_temp)) {
            throw new ParseException("XML decl error: version not terminated properly" + formatLineNumber());
        }
        ch = r.getChar();
        // ? space or e
        if (XMLChar.isSpaceChar(ch)) {
            ch = getCharAfterS();
            temp_offset = offset - increment;
            if (ch == 'e') {
                if (r.skipChar('n') && r.skipChar('c') && r.skipChar('o') && r.skipChar('d') && r.skipChar('i')
                        && r.skipChar('n') && r.skipChar('g')) {
                    ch = r.getChar();
                    if (XMLChar.isSpaceChar(ch)) {
                        ch = getCharAfterS();
                    }
                    if (ch == '=') {
                        /*
                         * System.out.println(
                         * " " + (temp_offset) + " " + 8 + " dec attr name (encoding) " + depth);
                         */
                        if (singleByteEncoding) {
                            _writeVTD(temp_offset, 8, TOKEN_DEC_ATTR_NAME, depth);
                        } else {
                            _writeVTD(temp_offset >> 1, 8, TOKEN_DEC_ATTR_NAME, depth);
                        }
                    } else {
                        throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
                    }
                    ch_temp = getCharAfterS();
                    if (ch_temp != '"' && ch_temp != '\'') {
                        throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
                    }
                    temp_offset = offset;
                    ch = r.getChar();
                    switch (ch) {
                        case 'a':
                        case 'A':
                            if ((r.skipChar('s') || r.skipChar('S')) && (r.skipChar('c') || r.skipChar('C'))
                                    && (r.skipChar('i') || r.skipChar('I')) && (r.skipChar('i') || r.skipChar('I'))
                                    && r.skipChar(ch_temp)) {
                                if (encoding != FORMAT_UTF_16LE && encoding != FORMAT_UTF_16BE) {
                                    // if (must_utf_8) {
                                    // throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
                                    // }
                                    // encoding = FORMAT_ASCII;
                                    // r = new ASCIIReader();
                                    /*
                                     * System.out.println(
                                     * " " + (temp_offset) + " " + 5 + " dec attr val (encoding) " + depth);
                                     */

                                    _writeVTD(temp_offset, 5, TOKEN_DEC_ATTR_VAL, depth);

                                    break;
                                } else {
                                    throw new ParseException("XML decl error: Can't switch encoding to ASCII"
                                            + formatLineNumber());
                                }
                            }
                            throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
                        case 'c':
                        case 'C':
                            // matchCPEncoding();
                            break;
                        case 'i':
                        case 'I':
                            // matchISOEncoding();
                            break;
                        case 'u':
                        case 'U':
                            // matchUTFEncoding();
                            break;
                        // now deal with windows encoding
                        case 'w':
                        case 'W':
                            // matchWindowsEncoding();
                            break;
                        default:
                            throw new ParseException("XML decl Error: invalid encoding" + formatLineNumber());
                    }
                    ch = r.getChar();
                    if (XMLChar.isSpaceChar(ch)) {
                        ch = getCharAfterS();
                    }
                    temp_offset = offset - increment;
                } else {
                    throw new ParseException("XML decl Error: Invalid char" + formatLineNumber());
                }
            }

            if (ch == 's') {
                if (r.skipChar('t') && r.skipChar('a') && r.skipChar('n') && r.skipChar('d') && r.skipChar('a')
                        && r.skipChar('l') && r.skipChar('o') && r.skipChar('n') && r.skipChar('e')) {

                    ch = getCharAfterS();
                    if (ch != '=') {
                        throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
                    }
                    /*
                     * System.out.println(
                     * " " + temp_offset + " " + 3 + " dec attr name (standalone) " + depth);
                     */
                    if (singleByteEncoding) {
                        _writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_NAME, depth);
                    } else {
                        _writeVTD(temp_offset >> 1, 10, TOKEN_DEC_ATTR_NAME, depth);
                    }
                    ch_temp = getCharAfterS();
                    temp_offset = offset;
                    if (ch_temp != '"' && ch_temp != '\'') {
                        throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
                    }
                    ch = r.getChar();
                    if (ch == 'y') {
                        if (r.skipChar('e') && r.skipChar('s') && r.skipChar(ch_temp)) {
                            /*
                             * System.out.println(
                             * " " + (temp_offset) + " " + 3 + " dec attr val (standalone) " + depth);
                             */
                            if (singleByteEncoding) {
                                _writeVTD(temp_offset, 3, TOKEN_DEC_ATTR_VAL, depth);
                            } else {
                                _writeVTD(temp_offset >> 1, 3, TOKEN_DEC_ATTR_VAL, depth);
                            }
                        } else {
                            throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
                        }
                    } else if (ch == 'n') {
                        if (r.skipChar('o') && r.skipChar(ch_temp)) {
                            /*
                             * System.out.println(
                             * " " + (temp_offset) + " " + 2 + " dec attr val (standalone)" + depth);
                             */
                            if (singleByteEncoding) {
                                _writeVTD(temp_offset, 2, TOKEN_DEC_ATTR_VAL, depth);
                            } else {
                                _writeVTD(temp_offset >> 1, 2, TOKEN_DEC_ATTR_VAL, depth);
                            }
                        } else {
                            throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
                        }
                    } else {
                        throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
                    }
                } else {
                    throw new ParseException("XML decl error" + formatLineNumber());
                }
                ch = r.getChar();
                if (XMLChar.isSpaceChar(ch)) {
                    ch = getCharAfterS();
                }
            }
        }

        if (ch == '?' && r.skipChar('>')) {
            temp_offset = offset;
            ch = getCharAfterS();
            if (ch == '<') {
                parser_state = STATE_LT_SEEN;
            } else {
                throw new ParseException("Other Error: Invalid Char in XML" + formatLineNumber());
            }
        } else {
            throw new ParseException("XML decl Error: Invalid termination sequence" + formatLineNumber());
        }
        return parser_state;
    }

    /**
     * This private method process DTD
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_doc_type() throws ParseException, EncodingException, EOFException {
        int z = 1, parser_state;
        while (true) {
            ch = r.getChar();
            if (XMLChar.isValidChar(ch)) {
                if (ch == '>') {
                    z--;
                } else if (ch == '<') {
                    z++;
                }
                if (z == 0) {
                    break;
                }
            } else {
                throw new ParseException("Error in DOCTYPE: Invalid char" + formatLineNumber());
            }
        }
        length1 = offset - temp_offset - increment;
        /*
         * System.out.println(
         * " " + (temp_offset) + " " + length1 + " DOCTYPE val " + depth);
         */
        if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
            if (length1 > MAX_TOKEN_LENGTH) {
                throw new ParseException("Token Length Error:" + " DTD val too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD(temp_offset, length1, TOKEN_DTD_VAL, depth);
        } else {
            if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                throw new ParseException("Token Length Error:" + " DTD val too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_DTD_VAL, depth);
        }
        ch = getCharAfterS();
        if (ch == '<') {
            parser_state = STATE_LT_SEEN;
        } else {
            throw new ParseException("Other Error: Invalid char in xml" + formatLineNumber());
        }
        return parser_state;
    }

    /**
     * This private method process the comment after the root document
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     */
    private int process_end_comment() throws ParseException {
        int parser_state;
        while (true) {
            ch = r.getChar();
            if (XMLChar.isValidChar(ch)) {
                if (ch == '-' && r.skipChar('-')) {
                    length1 = offset - temp_offset - (increment << 1);
                    break;
                }
            } else {
                throw new ParseException("Error in comment: Invalid Char" + formatLineNumber());
            }
        }
        if (r.getChar() == '>') {
            // System.out.println(" " + temp_offset + " " + length1 + " comment " + depth);
            if (singleByteEncoding) {
                writeVTDText(temp_offset, length1, TOKEN_COMMENT, depth);
            } else {
                writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_COMMENT, depth);
            }
            parser_state = STATE_DOC_END;
            return parser_state;
        }
        throw new ParseException("Error in comment: '-->' expected" + formatLineNumber());

    }

    private int process_end_doc() throws ParseException, EncodingException, EOFException {
        int parser_state;
        ch = getCharAfterS();
        /* eof exception should be thrown here for premature ending */
        if (ch == '<') {

            if (r.skipChar('?')) {
                /* processing instruction after end tag of root element */
                temp_offset = offset;
                parser_state = STATE_END_PI;
                return parser_state;
            } else if (r.skipChar('!') && r.skipChar('-') && r.skipChar('-')) {
                // comments allowed after the end tag of the root element
                temp_offset = offset;
                parser_state = STATE_END_COMMENT;
                return parser_state;
            }
        }
        throw new ParseException("Other Error: XML not terminated properly" + formatLineNumber());
    }

    /**
     * This private method processes PI after root document
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_end_pi() throws ParseException, EncodingException, EOFException {
        int parser_state;
        ch = r.getChar();
        if (XMLChar.isNameStartChar(ch)) {
            if ((ch == 'x' || ch == 'X') && (r.skipChar('m') || r.skipChar('M'))
                    && (r.skipChar('l') && r.skipChar('L'))) {
                // temp_offset = offset;
                ch = r.getChar();
                if (XMLChar.isSpaceChar(ch) || ch == '?') {
                    throw new ParseException("Error in PI: [xX][mM][lL] not a valid PI target" + formatLineNumber());
                    // offset = temp_offset;
                }
            }

            while (true) {
                // ch = getChar();
                if (!XMLChar.isNameChar(ch)) {
                    break;
                }
                ch = r.getChar();
            }

            length1 = offset - temp_offset - increment;
            /*
             * System.out.println(
             * ""
             * + (char) XMLDoc[temp_offset]
             * + " "
             * + (temp_offset)
             * + " "
             * + length1
             * + " PI Target "
             * + depth);
             */
            if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
                if (length1 > MAX_TOKEN_LENGTH) {
                    throw new ParseException("Token Length Error:" + "PI name too long (>0xfffff)" + formatLineNumber());
                }
                _writeVTD(temp_offset, length1, TOKEN_PI_NAME, depth);
            } else {
                if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                    throw new ParseException("Token Length Error:" + "PI name too long (>0xfffff)" + formatLineNumber());
                }
                _writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_NAME, depth);
            }
            // length1 = 0;
            temp_offset = offset;
            if (XMLChar.isSpaceChar(ch)) {
                ch = getCharAfterS();

                while (true) {
                    if (XMLChar.isValidChar(ch)) {
                        if (ch == '?') {
                            if (r.skipChar('>')) {
                                parser_state = STATE_DOC_END;
                                break;
                            } else {
                                throw new ParseException("Error in PI: invalid termination sequence"
                                        + formatLineNumber());
                            }
                        }
                    } else {
                        throw new ParseException("Error in PI: Invalid char in PI val" + formatLineNumber());
                    }
                    ch = r.getChar();
                }
                length1 = offset - temp_offset - (increment << 1);
                if (singleByteEncoding) {
                    if (length1 > MAX_TOKEN_LENGTH) {
                        throw new ParseException("Token Length Error:" + "PI val too long (>0xfffff)"
                                + formatLineNumber());
                    }
                    _writeVTD(temp_offset, length1, TOKEN_PI_VAL, depth);
                } else {
                    if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                        throw new ParseException("Token Length Error:" + "PI val too long (>0xfffff)"
                                + formatLineNumber());
                    }
                    _writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_VAL, depth);
                }
                // System.out.println(" " + temp_offset + " " + length1 + " PI val " + depth);
            } else {
                if (singleByteEncoding) {
                    _writeVTD((temp_offset), 0, TOKEN_PI_VAL, depth);
                } else {
                    _writeVTD((temp_offset) >> 1, 0, TOKEN_PI_VAL, depth);
                }
                if ((ch == '?') && r.skipChar('>')) {
                    parser_state = STATE_DOC_END;
                } else {
                    throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
                }
            }
            // parser_state = STATE_DOC_END;
        } else {
            throw new ParseException("Error in PI: invalid char in PI target" + formatLineNumber());
        }
        return parser_state;
    }

    private int process_ex_seen() throws ParseException, EncodingException, EOFException {
        int parser_state;
        boolean hasDTD = false;
        ch = r.getChar();
        switch (ch) {
            case '-':
                if (r.skipChar('-')) {
                    temp_offset = offset;
                    parser_state = STATE_COMMENT;
                    break;
                } else {
                    throw new ParseException("Error in comment: Invalid char sequence to start a comment"
                            + formatLineNumber());
                }
            case '[':
                if (r.skipChar('C') && r.skipChar('D') && r.skipChar('A') && r.skipChar('T') && r.skipChar('A')
                        && r.skipChar('[') && (depth != -1)) {
                    temp_offset = offset;
                    parser_state = STATE_CDATA;
                    break;
                } else {
                    if (depth == -1) {
                        throw new ParseException("Error in CDATA: Wrong place for CDATA" + formatLineNumber());
                    }
                    throw new ParseException("Error in CDATA: Invalid char sequence for CDATA" + formatLineNumber());
                }

            case 'D':
                if (r.skipChar('O') && r.skipChar('C') && r.skipChar('T') && r.skipChar('Y') && r.skipChar('P')
                        && r.skipChar('E') && (depth == -1) && !hasDTD) {
                    hasDTD = true;
                    temp_offset = offset;
                    parser_state = STATE_DOCTYPE;
                    break;
                } else {
                    if (hasDTD == true) {
                        throw new ParseException("Error for DOCTYPE: Only DOCTYPE allowed" + formatLineNumber());
                    }
                    if (depth != -1) {
                        throw new ParseException("Error for DOCTYPE: DTD at wrong place" + formatLineNumber());
                    }
                    throw new ParseException("Error for DOCTYPE: Invalid char sequence for DOCTYPE"
                            + formatLineNumber());
                }
            default:
                throw new ParseException("Other Error: Unrecognized char after <!" + formatLineNumber());
        }
        return parser_state;
    }

    /**
     * This private method processes PI tag
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_pi_tag() throws ParseException, EncodingException, EOFException {
        int parser_state;
        while (true) {
            ch = r.getChar();
            if (!XMLChar.isNameChar(ch)) {
                break;
                // System.out.println(" ch ==> "+(char)ch);
            }
        }

        length1 = offset - temp_offset - increment;
        /*
         * System.out.println(
         * ((char) XMLDoc[temp_offset])
         * + " "
         * + (temp_offset)
         * + " "
         * + length1
         * + " PI Target "
         * + depth);
         */
        // if (encoding < FORMAT_UTF_16BE){
        if (singleByteEncoding) {
            if (length1 > MAX_TOKEN_LENGTH) {
                throw new ParseException("Token Length Error:" + " PI name too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD((temp_offset), length1, TOKEN_PI_NAME, depth);
        } else {
            if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                throw new ParseException("Token Length Error:" + " PI name too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD((temp_offset) >> 1, (length1 >> 1), TOKEN_PI_NAME, depth);
        }
        // length1 = 0;
        // temp_offset = offset;
        /*
         * if (XMLChar.isSpaceChar(ch)) {
         * ch = r.getChar();
         * }
         */
        // ch = r.getChar();
        if (ch == '?') {
            // insert zero length pi name tag
            if (singleByteEncoding) {
                _writeVTD((temp_offset), 0, TOKEN_PI_VAL, depth);
            } else {
                _writeVTD((temp_offset) >> 1, (0), TOKEN_PI_VAL, depth);
            }
            if (r.skipChar('>')) {
                temp_offset = offset;
                // ch = getCharAfterSe();
                ch = getCharAfterS();
                if (ch == '<') {
                    if (ws) {
                        addWhiteSpaceRecord();
                    }
                    parser_state = STATE_LT_SEEN;
                } else if (XMLChar.isContentChar(ch)) {
                    parser_state = STATE_TEXT;
                } else if (ch == '&') {
                    // has_amp = true;
                    final int startOfEntityBody = offset;
                    final int entity = entityIdentifier();
                    XMLDoc[startOfEntityBody - 1] = (char) entity;
                    for (int i = startOfEntityBody; i < offset; i++) {
                        XMLDoc[i] = 0;
                    }
                    parser_state = STATE_TEXT;
                } else if (ch == ']') {
                    if (r.skipChar(']')) {
                        while (r.skipChar(']')) {
                        }
                        if (r.skipChar('>')) {
                            throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                        }
                    }
                    parser_state = STATE_TEXT;
                } else {
                    throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
                }
                return parser_state;
            } else {
                throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
            }
        }
        parser_state = STATE_PI_VAL;
        return parser_state;
    }

    /**
     * This private method processes PI val
     * 
     * @return the parser state after which the parser loop jumps to
     * @throws ParseException
     * @throws EncodingException
     * @throws EOFException
     */
    private int process_pi_val() throws ParseException, EncodingException, EOFException {
        int parser_state;
        if (!XMLChar.isSpaceChar(ch)) {
            throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
        }
        temp_offset = offset;
        ch = r.getChar();
        while (true) {
            if (XMLChar.isValidChar(ch)) {
                // System.out.println(""+(char)ch);
                if (ch == '?') {
                    if (r.skipChar('>')) {
                        break;
                    } /*
                       * else
                       * throw new ParseException(
                       * "Error in PI: invalid termination sequence for PI"
                       * + formatLineNumber());
                       */
                }
            } else {
                throw new ParseException("Errors in PI: Invalid char in PI val" + formatLineNumber());
            }
            ch = r.getChar();
        }
        length1 = offset - temp_offset - (increment << 1);
        /*
         * System.out.println(
         * ((char) XMLDoc[temp_offset])
         * + " "
         * + (temp_offset)
         * + " "
         * + length1
         * + " PI val "
         * + depth);
         */
        // if (length1 != 0)
        if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
            if (length1 > MAX_TOKEN_LENGTH) {
                throw new ParseException("Token Length Error:" + "PI VAL too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD(temp_offset, length1, TOKEN_PI_VAL, depth);
        } else {
            if (length1 > (MAX_TOKEN_LENGTH << 1)) {
                throw new ParseException("Token Length Error:" + "PI VAL too long (>0xfffff)" + formatLineNumber());
            }
            _writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_VAL, depth);
        }
        // length1 = 0;
        temp_offset = offset;
        // ch = getCharAfterSe();
        ch = getCharAfterS();
        if (ch == '<') {
            if (ws) {
                addWhiteSpaceRecord();
            }
            parser_state = STATE_LT_SEEN;
        } else if (XMLChar.isContentChar(ch)) {
            // temp_offset = offset;
            parser_state = STATE_TEXT;
        } else if (ch == '&') {
            // has_amp = true;
            // temp_offset = offset;
            final int startOfEntityBody = offset;
            final int entity = entityIdentifier();
            XMLDoc[startOfEntityBody - 1] = (char) entity;
            for (int i = startOfEntityBody; i < offset; i++) {
                XMLDoc[i] = 0;
            }
            parser_state = STATE_TEXT;
        } else if (ch == ']') {
            if (r.skipChar(']')) {
                while (r.skipChar(']')) {
                }
                if (r.skipChar('>')) {
                    throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                }

            }
            parser_state = STATE_TEXT;
        } else {
            throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
        }
        return parser_state;

    }

    private int process_qm_seen() throws ParseException, EncodingException, EOFException {
        temp_offset = offset;
        ch = r.getChar();
        if (XMLChar.isNameStartChar(ch)) {
            // temp_offset = offset;
            if ((ch == 'x' || ch == 'X') && (r.skipChar('m') || r.skipChar('M'))
                    && (r.skipChar('l') || r.skipChar('L'))) {
                ch = r.getChar();
                if (ch == '?' || XMLChar.isSpaceChar(ch)) {
                    throw new ParseException("Error in PI: [xX][mM][lL] not a valid PI targetname" + formatLineNumber());
                }
                offset = getPrevOffset();
            }
            return STATE_PI_TAG;
        }
        throw new ParseException("Other Error: First char after <? invalid" + formatLineNumber());
    }

    private int process_start_doc() throws ParseException, EncodingException, EOFException {
        final int c = r.getChar();
        if (c == '<') {
            temp_offset = offset;
            // xml decl has to be right after the start of the document
            if (r.skipChar('?') && (r.skipChar('x') || r.skipChar('X')) && (r.skipChar('m') || r.skipChar('M'))
                    && (r.skipChar('l') || r.skipChar('L'))) {
                if (r.skipChar(' ') || r.skipChar('\t') || r.skipChar('\n') || r.skipChar('\r')) {
                    ch = getCharAfterS();
                    temp_offset = offset;
                    return STATE_DEC_ATTR_NAME;
                } else if (r.skipChar('?')) {
                    throw new ParseException("Error in XML decl: Premature ending" + formatLineNumber());
                }
            }
            offset = temp_offset;
            return STATE_LT_SEEN;
        } else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
            if (getCharAfterS() == '<') {
                return STATE_LT_SEEN;
            }
        }
        throw new ParseException("Other Error: XML not starting properly" + formatLineNumber());
    }

    /**
     * Write the VTD and LC into their storage container for where LC depth is 5.
     * 
     * @param offset
     *            int
     * @param length
     *            int
     * @param token_type
     *            int
     * @param depth
     *            int
     */
    private void writeVTD(final int offset, final int length, final int token_type, final int depth) {

        VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);

        switch (depth) {
            case 0:
                rootIndex = VTDBuffer.size - 1;
                break;
            case 1:
                if (last_depth == 1) {
                    l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
                } else if (last_depth == 2) {
                    l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
                }
                last_l1_index = VTDBuffer.size - 1;
                last_depth = 1;
                break;
            case 2:
                if (last_depth == 1) {
                    l1Buffer.append(((long) last_l1_index << 32) + l2Buffer.size);
                } else if (last_depth == 2) {
                    l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
                }
                last_l2_index = VTDBuffer.size - 1;
                last_depth = 2;
                break;

            case 3:
                l3Buffer.append(VTDBuffer.size - 1);
                if (last_depth == 2) {
                    l2Buffer.append(((long) last_l2_index << 32) + l3Buffer.size - 1);
                }
                last_depth = 3;
                break;
            default:
                // rootIndex = VTDBuffer.size() - 1;
        }
    }

    private void _writeVTD(final int offset, final int length, final int token_type, final int depth) {
        VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);
    }

    private void writeVTDText(final int offset, final int length, final int token_type, final int depth) {
        if (length > MAX_TOKEN_LENGTH) {
            int k;
            int r_offset = offset;
            for (k = length; k > MAX_TOKEN_LENGTH; k = k - MAX_TOKEN_LENGTH) {
                VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | MAX_TOKEN_LENGTH) << 32)
                        | r_offset);
                r_offset += MAX_TOKEN_LENGTH;
            }
            VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | k) << 32) | r_offset);
        } else {
            VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);
        }
    }

    /**
     * 
     * @throws ParseException
     */
    private void qualifyElement() throws ParseException {
        int i = nsBuffer3.size - 1;
        // two cases:
        // 1. the current element has no prefix, look for xmlns
        // 2. the current element has prefix, look for xmlns:something

        final int preLen = (int) ((currentElementRecord & 0xffff000000000000L) >> 48);
        final int preOs = (int) currentElementRecord;
        while (i >= 0) {
            final int t = nsBuffer3.upper32At(i);
            // with prefix, get full length and prefix length
            if ((t & 0xffff) - (t >> 16) == preLen) {
                // doing byte comparison here
                final int os = nsBuffer3.lower32At(i) + (t >> 16) + increment;
                int k = 0;
                for (; k < preLen - increment; k++) {
                    if (XMLDoc[os + k] != XMLDoc[preOs + k]) {
                        break;
                    }
                }
                if (k == preLen - increment) {
                    return; // found the match
                }
            }
            /*
             * if ( (nsBuffer3.upper32At(i) & 0xffff0000) == 0){
             * return;
             * }
             */
            i--;
        }
        // no need to check if xml is the prefix
        if (checkPrefix(preOs, preLen)) {
            return;
        }

        // print line # column# and full element name
        throw new ParseException("Name space qualification Exception: Element not qualified\n"
                + formatLineNumber((int) currentElementRecord));
    }

    private boolean checkPrefix(final int os, final int len) {
        // int i=0;
        if (encoding < FORMAT_UTF_16BE) {
            if (len == 4 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 'm' && XMLDoc[os + 2] == 'l') {
                return true;
            }
        } else if (encoding == FORMAT_UTF_16BE) {
            if (len == 8 && XMLDoc[os] == 0 && XMLDoc[os + 1] == 'x' && XMLDoc[os + 2] == 0 && XMLDoc[os + 3] == 'm'
                    && XMLDoc[os + 4] == 0 && XMLDoc[os + 5] == 'l') {
                return true;
            }
        } else {
            if (len == 8 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 0 && XMLDoc[os + 2] == 'm' && XMLDoc[os + 3] == 0
                    && XMLDoc[os + 4] == 'l' && XMLDoc[os + 5] == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPrefix2(final int os, final int len) {
        // int i=0;
        if (encoding < FORMAT_UTF_16BE) {
            if (len == 5 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 'm' && XMLDoc[os + 2] == 'l'
                    && XMLDoc[os + 3] == 'n' && XMLDoc[os + 4] == 's') {
                return true;
            }
        } else if (encoding == FORMAT_UTF_16BE) {
            if (len == 10 && XMLDoc[os] == 0 && XMLDoc[os + 1] == 'x' && XMLDoc[os + 2] == 0 && XMLDoc[os + 3] == 'm'
                    && XMLDoc[os + 4] == 0 && XMLDoc[os + 5] == 'l' && XMLDoc[os + 6] == 0 && XMLDoc[os + 7] == 'n'
                    && XMLDoc[os + 8] == 0 && XMLDoc[os + 9] == 's') {
                return true;
            }
        } else {
            if (len == 10 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 0 && XMLDoc[os + 2] == 'm' && XMLDoc[os + 3] == 0
                    && XMLDoc[os + 4] == 'l' && XMLDoc[os + 5] == 0 && XMLDoc[os + 6] == 'n' && XMLDoc[os + 3] == 0
                    && XMLDoc[os + 8] == 's' && XMLDoc[os + 5] == 0) {
                return true;
            }
        }
        return false;
    }

    private long _getCharResolved(int byte_offset) {

        int ch = 0;
        int val = 0;
        long inc = 2 << (increment - 1);
        final long l = r._getChar(byte_offset);

        ch = (int) l;

        if (ch != '&') {
            return l;
        }

        // let us handle references here
        // currentOffset++;
        byte_offset += increment;
        ch = getCharUnit(byte_offset);
        byte_offset += increment;
        switch (ch) {
            case '#':

                ch = getCharUnit(byte_offset);

                if (ch == 'x') {
                    while (true) {
                        byte_offset += increment;
                        inc += increment;
                        ch = getCharUnit(byte_offset);

                        if (ch >= '0' && ch <= '9') {
                            val = (val << 4) + (ch - '0');
                        } else if (ch >= 'a' && ch <= 'f') {
                            val = (val << 4) + (ch - 'a' + 10);
                        } else if (ch >= 'A' && ch <= 'F') {
                            val = (val << 4) + (ch - 'A' + 10);
                        } else if (ch == ';') {
                            inc += increment;
                            break;
                        }
                    }
                } else {
                    while (true) {
                        ch = getCharUnit(byte_offset);
                        byte_offset += increment;
                        inc += increment;
                        if (ch >= '0' && ch <= '9') {
                            val = val * 10 + (ch - '0');
                        } else if (ch == ';') {
                            break;
                        }
                    }
                }
                break;

            case 'a':
                ch = getCharUnit(byte_offset);
                if (encoding < FORMAT_UTF_16BE) {
                    if (ch == 'm') {
                        if (getCharUnit(byte_offset + 1) == 'p' && getCharUnit(byte_offset + 2) == ';') {
                            inc = 5;
                            val = '&';
                        }
                    } else if (ch == 'p') {
                        if (getCharUnit(byte_offset + 1) == 'o' && getCharUnit(byte_offset + 2) == 's'
                                && getCharUnit(byte_offset + 3) == ';') {
                            inc = 6;
                            val = '\'';
                        }
                    }
                } else {
                    if (ch == 'm') {
                        if (getCharUnit(byte_offset + 2) == 'p' && getCharUnit(byte_offset + 4) == ';') {
                            inc = 10;
                            val = '&';
                        }
                    } else if (ch == 'p') {
                        if (getCharUnit(byte_offset + 2) == 'o' && getCharUnit(byte_offset + 4) == 's'
                                && getCharUnit(byte_offset + 6) == ';') {
                            inc = 12;
                            val = '\'';
                        }
                    }
                }
                break;

            case 'q':

                if (encoding < FORMAT_UTF_16BE) {
                    if (getCharUnit(byte_offset) == 'u' && getCharUnit(byte_offset + 1) == 'o'
                            && getCharUnit(byte_offset + 2) == 't' && getCharUnit(byte_offset + 3) == ';') {
                        inc = 6;
                        val = '\"';
                    }
                } else {
                    if (getCharUnit(byte_offset) == 'u' && getCharUnit(byte_offset + 2) == 'o'
                            && getCharUnit(byte_offset + 4) == 't' && getCharUnit(byte_offset + 6) == ';') {
                        inc = 12;
                        val = '\"';
                    }
                }
                break;
            case 'l':
                if (encoding < FORMAT_UTF_16BE) {
                    if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 1) == ';') {
                        // offset += 2;
                        inc = 4;
                        val = '<';
                    }
                } else {
                    if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 2) == ';') {
                        // offset += 2;
                        inc = 8;
                        val = '<';
                    }
                }
                break;
            case 'g':
                if (encoding < FORMAT_UTF_16BE) {
                    if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 1) == ';') {
                        inc = 4;
                        val = '>';
                    }
                } else {
                    if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 2) == ';') {
                        inc = 8;
                        val = '>';
                    }
                }
                break;
        }

        // currentOffset++;
        return val | (inc << 32);
    }

    // return 0;

    private int getCharUnit(final int byte_offset) {
        return (encoding <= 2) ? XMLDoc[byte_offset] & 0xff : (encoding < FORMAT_UTF_16BE) ? r.decode(byte_offset)
                : (encoding == FORMAT_UTF_16BE) ? ((XMLDoc[byte_offset]) << 8 | XMLDoc[byte_offset + 1])
                        : ((XMLDoc[byte_offset + 1]) << 8 | XMLDoc[byte_offset]);
    }

    private boolean matchURL(final int bos1, final int len1, final int bos2, final int len2) {
        long l1, l2;
        int i1 = bos1, i2 = bos2;
        final int i3 = bos1 + len1, i4 = bos2 + len2;
        // System.out.println("--->"+new String(XMLDoc, bos1, len1)+" "+new String(XMLDoc,bos2,len2));
        while (i1 < i3 && i2 < i4) {
            l1 = _getCharResolved(i1);
            l2 = _getCharResolved(i2);
            if ((int) l1 != (int) l2) {
                return false;
            }
            i1 += (int) (l1 >> 32);
            i2 += (int) (l2 >> 32);
        }
        if (i1 == i3 && i2 == i4) {
            return true;
        }
        return false;
    }

    private void checkAttributeUniqueness() throws ParseException {
        boolean unique = true;
        boolean unequal;
        for (int i = 0; i < attr_count; i++) {
            unequal = false;
            final int prevLen = (int) attr_name_array[i];
            if (length1 == prevLen) {
                final int prevOffset = (int) (attr_name_array[i] >> 32);
                for (int j = 0; j < prevLen; j++) {
                    if (XMLDoc[prevOffset + j] != XMLDoc[temp_offset + j]) {
                        unequal = true;
                        break;
                    }
                }
            } else {
                unequal = true;
            }
            unique = unique && unequal;
        }
        if (!unique && attr_count != 0) {
            throw new ParseException("Error in attr: Attr name not unique" + formatLineNumber());
        }
        unique = true;
        if (attr_count < attr_name_array.length) {
            attr_name_array[attr_count] = ((long) (temp_offset) << 32) | length1;
            attr_count++;
        } else // grow the attr_name_array by 16
        {
            final long[] temp_array = attr_name_array;
            /*
             * System.out.println(
             * "size increase from "
             * + temp_array.length
             * + "  to "
             * + (attr_count + 16));
             */
            attr_name_array = new long[attr_count + ATTR_NAME_ARRAY_SIZE];
            System.arraycopy(temp_array, 0, attr_name_array, 0, attr_count);
            /*
             * for (int i = 0; i < attr_count; i++) {
             * attr_name_array[i] = temp_array[i];
             * }
             */
            attr_name_array[attr_count] = ((long) (temp_offset) << 32) | length1;
            attr_count++;
        }
        // insert prefix attr node into the prefixed_attr_name array
        // xml:something will not be inserted
        // System.out.println(" prefixed attr count ===>"+prefixed_attr_count);
        // System.out.println(" length2 ===>"+length2);
        if (ns && !is_ns && length2 != 0) {
            if ((increment == 1 && length2 == 3 && matchXML(temp_offset))
                    || (increment == 2 && length2 == 6 && matchXML(temp_offset))) {
                return;
            } else if (prefixed_attr_count < prefixed_attr_name_array.length) {
                prefixed_attr_name_array[prefixed_attr_count] = ((long) (temp_offset) << 32) | (length2 << 16)
                        | length1;
                prefixed_attr_count++;
            } else {
                final long[] temp_array1 = prefixed_attr_name_array;
                prefixed_attr_name_array = new long[prefixed_attr_count + ATTR_NAME_ARRAY_SIZE];
                prefix_URL_array = new int[prefixed_attr_count + ATTR_NAME_ARRAY_SIZE];
                System.arraycopy(temp_array1, 0, prefixed_attr_name_array, 0, prefixed_attr_count);
                // System.arraycopy(temp_array1, 0, prefixed_attr_val_array, 0, prefixed_attr_count)
                /*
                 * for (int i = 0; i < attr_count; i++) {
                 * attr_name_array[i] = temp_array[i];
                 * }
                 */
                prefixed_attr_name_array[prefixed_attr_count] = ((long) (temp_offset) << 32) | (length2 << 16)
                        | length1;
                prefixed_attr_count++;
            }
        }
    }

    private void handleOtherTextChar(final int ch) throws ParseException {
        if (ch == '&') {
            final int startOfEntityBody = offset;
            final int entity = entityIdentifier();
            if (!XMLChar.isValidChar(entity)) {
                throw new ParseException("Error in text content: Invalid char in text content " + formatLineNumber());
            }
            XMLDoc[startOfEntityBody - 1] = (char) entity;
            for (int i = startOfEntityBody; i < offset; i++) {
                XMLDoc[i] = 0;
            }
        } else if (ch == ']') {
            if (r.skipChar(']')) {
                while (r.skipChar(']')) {
                }
                if (r.skipChar('>')) {
                    throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                }
            }
        } else {
            throw new ParseException("Error in text content: Invalid char in text content " + formatLineNumber());
        }
    }

    private void handleOtherTextChar2(final int ch) throws ParseException {
        if (ch == '&') {
            final int startOfEntityBody = offset;
            final int entity = entityIdentifier();
            XMLDoc[startOfEntityBody - 1] = (char) entity;
            for (int i = startOfEntityBody; i < offset; i++) {
                XMLDoc[i] = 0;
            }
        } else if (ch == ']') {
            if (r.skipChar(']')) {
                while (r.skipChar(']')) {
                }
                if (r.skipChar('>')) {
                    throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
                }
            }
            // parser_state = STATE_TEXT;
        } else {
            throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
        }
    }
}
