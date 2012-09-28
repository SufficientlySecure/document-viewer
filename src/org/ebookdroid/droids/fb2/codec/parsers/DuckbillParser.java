package org.ebookdroid.droids.fb2.codec.parsers;

import org.ebookdroid.droids.fb2.codec.FB2Tag;
import org.ebookdroid.droids.fb2.codec.handlers.IContentHandler;

import java.util.Arrays;

public class DuckbillParser {

    public void parse(final char[] xmlChars, final int length, final IContentHandler handler) throws Exception {
        final XmlReader r = new XmlReader(xmlChars, length);

        int charsStart = -1;

        while (r.XmlOffset < length) {
            // Check if START_TAG, END_TAG or COMMENT tokens
            if (r.skipChar('<')) {
                // Process text of parent element in case of START_TAG or current element in case of END_TAG
                if (charsStart != -1) {
                    if (!handler.skipCharacters()) {
                        handler.characters(r.XmlDoc, charsStart, r.XmlOffset - 1 - charsStart, true);
                    }
                    charsStart = -1;
                }
                // Check for COMMENT token
                r.push();
                if (r.skipChar('!') && r.skipChar('-') && r.skipChar('-')) {
                    // Process COMMENT token
                    r.pop();
                    r.skipComment();
                    continue;
                }
                r.pop();

                // Check for END_TAG token
                if (r.skipChar('/')) {
                    // Process END_TAG token
                    final int tagNameStart = r.XmlOffset;
                    r.skipTagName();
                    final FB2Tag tag = FB2Tag.getTagByName1(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);
                    handler.endElement(tag);
                    r.skipTo('>');
                    r.XmlOffset++;
                    continue;
                }

                // Process START_TAG token
                final int tagNameStart = r.XmlOffset;
                r.skipTagName();
                final FB2Tag tag = FB2Tag.getTagByName1(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);

                // Process tag attributes
                if (handler.parseAttributes(tag)) {
                    final String[] attributes = r.fillAttributes(tag);
                    handler.startElement(tag, attributes);
                } else {
                    handler.startElement(tag);
                }
                r.skipToEndTag();

                // Check for closed tag
                if (r.skipChar('/') && r.skipChar('>')) {
                    // Process closed tag
                    handler.endElement(tag);
                    continue;
                }
            } else {
                // Process text
                if (charsStart == -1) {
                    charsStart = r.XmlOffset;
                }
                // Check for entity
                if (r.XmlDoc[r.XmlOffset] == '&') {
                    r.push();
                    if (r.skipTo(';')) {
                        final int endOfEntity = r.XmlOffset;
                        r.pop();
                        final int startOfEntity = r.XmlOffset;

                        char entity = (char) -1;
                        final String string = new String(r.XmlDoc, r.XmlOffset + 1, endOfEntity - r.XmlOffset - 1);

                        if (r.skipChar('#')) {
                            if (r.skipChar('x') || r.skipChar('X')) {
                                entity = (char) Integer.parseInt(string, 16);
                            } else {
                                entity = (char) Integer.parseInt(string, 10);
                            }
                        } else {
                            final String e = string;
                            if ("qout".equals(e)) {
                                entity = 34;
                            } else if ("amp".equals(e)) {
                                entity = 38;
                            } else if ("apos".equals(e)) {
                                entity = 39;
                            } else if ("lt".equals(e)) {
                                entity = 60;
                            } else if ("gt".equals(e)) {
                                entity = 62;
                            }
                        }
                        if (entity != -1) {
                            r.XmlDoc[startOfEntity] = entity;
                            for (int i = startOfEntity + 1; i <= endOfEntity; i++) {
                                r.XmlDoc[i] = 0;
                            }
                        } else {
                            r.XmlOffset = startOfEntity + 1;
                        }
                    } else {
                        r.pop();
                    }
                }
            }
            // Next token
            r.XmlOffset++;
        }
    }

    private class XmlReader {

        public final char[] XmlDoc;
        public int XmlOffset = 0;
        public final int XmlLength;
        private final int[] stack = new int[1024];
        private int stackOffset = 0;

        public XmlReader(final char[] xmlDoc, final int xmlLength) {
            XmlDoc = xmlDoc;
            XmlLength = xmlLength;
        }

        public boolean skipChar(final char c) {
            if (XmlDoc[XmlOffset] == c) {
                XmlOffset++;
                return true;
            }
            return false;
        }

        public void push() {
            stack[stackOffset++] = XmlOffset;
        }

        public void pop() {
            XmlOffset = stack[--stackOffset];
        }

        public void skipComment() {
            while (XmlOffset < XmlLength) {
                push();
                if (skipChar('-') && skipChar('-') && skipChar('>')) {
                    break;
                }
                pop();
                XmlOffset++;
            }
        }

        public void skipTagName() {
            while (XmlOffset < XmlLength) {
                if (Character.isWhitespace(XmlDoc[XmlOffset])
                        || (XmlDoc[XmlOffset] == '/' && XmlDoc[XmlOffset + 1] == '>') || XmlDoc[XmlOffset] == '>') {
                    break;
                }
                XmlOffset++;
            }
        }

        public boolean skipTo(final char c) {
            while (XmlOffset < XmlLength) {
                if (XmlDoc[XmlOffset] == c) {
                    return true;
                }
                XmlOffset++;
            }
            return false;
        }

        public void skipToEndTag() {
            while (XmlOffset < XmlLength) {
                if ((XmlDoc[XmlOffset] == '/' && XmlDoc[XmlOffset + 1] == '>') || XmlDoc[XmlOffset] == '>') {
                    break;
                }
                XmlOffset++;
            }
        }

        public String[] fillAttributes(final FB2Tag tag) {
            if (tag.attributes.length == 0) {
                return null;
            }
            final String[] res = new String[tag.attributes.length];
            push();
            final int start = XmlOffset;
            skipToEndTag();
            final int end = XmlOffset;
            pop();
            final String attrs = new String(XmlDoc, start, end - start);
            final String[] pairs = attrs.split(" ");
            for (final String pair : pairs) {
                final String[] split = pair.split("=");
                if (split.length == 2) {
                    String attrName = split[0];
                    String attrValue = split[1];
                    final String[] split2 = attrName.split(":");
                    attrName = split2[split2.length - 1];
                    if (attrValue.startsWith("\"") && attrValue.endsWith("\"")) {
                        attrValue = attrValue.substring(1, attrValue.length() - 1);
                    }
                    final int i = Arrays.binarySearch(tag.attributes, attrName);
                    if (i >= 0) {
                        res[i] = attrValue;
                    }
                }
            }
            return res;
        }

    }

}
