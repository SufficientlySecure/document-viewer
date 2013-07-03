package org.emdev.common.xml.parsers;

import java.util.Arrays;

import org.emdev.common.xml.IContentHandler;
import org.emdev.common.xml.IXmlTagFactory;
import org.emdev.common.xml.TextProvider;
import org.emdev.common.xml.tags.XmlTag;
import org.emdev.utils.StringUtils;

public class DuckbillParser {

    public void parse(final TextProvider text, final IXmlTagFactory factory, final IContentHandler handler)
            throws Exception {
        final char[] xmlChars = text.chars;
        final int length = text.size;

        final XmlReader r = new XmlReader(xmlChars, length);

        int charsStart = -1;

        while (r.XmlOffset < length) {
            // Check if START_TAG, END_TAG or COMMENT tokens
            if (r.skipChar('<')) {
                // Process text of parent element in case of START_TAG or current element in case of END_TAG
                if (charsStart != -1) {
                    if (!handler.skipCharacters()) {
                        handler.characters(text, charsStart, r.XmlOffset - 1 - charsStart);
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
                    final XmlTag tag = factory.getTagByName(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);
                    handler.endElement(tag);
                    r.skipTo('>');
                    r.XmlOffset++;
                    continue;
                }

                // Process START_TAG token
                final int tagNameStart = r.XmlOffset;
                r.skipTagName();
                final XmlTag tag = factory.getTagByName(r.XmlDoc, tagNameStart, r.XmlOffset - tagNameStart);

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
                        r.XmlOffset++;

                        char entity = (char) -1;

                        if (r.skipChar('#')) {
                            if (r.skipChar('x') || r.skipChar('X')) {
                                entity = (char) StringUtils.parseInt(r.XmlDoc, r.XmlOffset, endOfEntity - r.XmlOffset,
                                        16);
                            } else {
                                entity = (char) StringUtils.parseInt(r.XmlDoc, r.XmlOffset, endOfEntity - r.XmlOffset,
                                        10);
                            }
                        } else {
                            final int idx = r.XmlOffset;
                            if (r.XmlDoc[idx] == 'q' && r.XmlDoc[idx + 1] == 'o' && r.XmlDoc[idx + 2] == 'u'
                                    && r.XmlDoc[idx + 3] == 't' && r.XmlDoc[idx + 4] == ';') {
                                // quot
                                entity = 34;
                            } else if (r.XmlDoc[idx] == 'a' && r.XmlDoc[idx + 1] == 'm' && r.XmlDoc[idx + 2] == 'p'
                                    && r.XmlDoc[idx + 3] == ';') {
                                // amp
                                entity = 38;
                            } else if (r.XmlDoc[idx] == 'a' && r.XmlDoc[idx + 1] == 'p' && r.XmlDoc[idx + 2] == 'o'
                                    && r.XmlDoc[idx + 3] == 's' && r.XmlDoc[idx + 4] == ';') {
                                // apos
                                entity = 39;
                            } else if (r.XmlDoc[idx] == 'l' && r.XmlDoc[idx + 1] == 't' && r.XmlDoc[idx + 2] == ';') {
                                // lt
                                entity = 60;
                            } else if (r.XmlDoc[idx] == 'g' && r.XmlDoc[idx + 1] == 't' && r.XmlDoc[idx + 2] == ';') {
                                // gt
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
                if (((XmlDoc[XmlOffset] >= 0x1c && XmlDoc[XmlOffset] <= 0x20) || (XmlDoc[XmlOffset] >= 0x9 && XmlDoc[XmlOffset] <= 0xd))
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

        public String[] fillAttributes(final XmlTag tag) {
            if (tag.attributes.length == 0) {
                return null;
            }
            final String[] res = new String[tag.attributes.length];
            push();
            final int start = XmlOffset;
            skipToEndTag();
            final int end = XmlOffset;
            pop();

            String attrs = new String(XmlDoc, start, end - start).trim();

            for (int index = attrs.indexOf("="); index > 0; index = attrs.indexOf("=")) {
                final String[] qName = attrs.substring(0, index).trim().split(":");
                final String attrName = qName[qName.length - 1];

                attrs = attrs.substring(index + 1).trim();
                final char quote = attrs.charAt(0);
                if (quote == '"' || quote == '\'') {
                    final int qIndex = attrs.indexOf(quote, 1);
                    if (qIndex > 0) {
                        final String attrValue = attrs.substring(1, qIndex);
                        final int i = Arrays.binarySearch(tag.attributes, attrName);
                        if (i >= 0) {
                            res[i] = attrValue;
                        }
                        attrs = attrs.substring(qIndex + 1).trim();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            return res;
        }

    }

}
