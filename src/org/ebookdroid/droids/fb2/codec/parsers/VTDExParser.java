package org.ebookdroid.droids.fb2.codec.parsers;

import org.ebookdroid.droids.fb2.codec.FB2Tag;
import org.ebookdroid.droids.fb2.codec.handlers.IContentHandler;

import java.util.Arrays;

import com.ximpleware.NavException;
import com.ximpleware.VTDGenEx;
import com.ximpleware.VTDNavEx;

public class VTDExParser {

    public void parse(final VTDGenEx inStream, final IContentHandler handler) throws NavException {
        final VTDNavEx nav = inStream.getNav();

        final boolean res = nav.toElement(VTDNavEx.ROOT);
        if (!res) {
            return;
        }

        FB2Tag tag = FB2Tag.UNKNOWN;
        int skipUntilSiblingOrParent = -1;

        final int first = nav.getCurrentIndex();
        final int last = nav.getTokenCount();

        final String[] tagAttrs = new String[2];
        final FB2Tag[] tags = new FB2Tag[1024];

        final int[] range = new int[2];

        int maxDepth = -1;
        for (int ci = first; ci < last;) {
            final int depth = nav.getTokenDepth(ci);
            final int type = nav.getTokenType(ci);

            if (skipUntilSiblingOrParent != -1) {
                if (type == VTDNavEx.TOKEN_STARTING_TAG && depth <= skipUntilSiblingOrParent) {
                    skipUntilSiblingOrParent = -1;
                } else {
                    ci++;
                    continue;
                }
            }

            if (type == VTDNavEx.TOKEN_STARTING_TAG) {
                final char[] buf = nav.toRawString(ci, range);
                tag = FB2Tag.getTagByName3(buf, range[0], range[1]);

                for (int d = maxDepth; d >= depth; d--) {
                    if (tags[d] != null) {
                        handler.endElement(tags[d]);
                        tags[d] = null;
                    }
                }
                tags[depth] = tag;
                maxDepth = depth;

                if (tag == FB2Tag.UNKNOWN) {
                    skipUntilSiblingOrParent = depth;
                    ci++;
                    continue;
                }

                if (tag.attributes.length > 0) {
                    ci = fillAtributes(nav, ci + 1, last, tag, tagAttrs);
                    handler.startElement(tag, tagAttrs);
                } else {
                    ci = skipAtributes(nav, ci + 1, last);
                    handler.startElement(tag);
                }
                continue;
            }

            for (int d = maxDepth; d > depth; d--) {
                if (tags[d] != null) {
                    handler.endElement(tags[d]);
                    tags[d] = null;
                }
            }
            maxDepth = depth;

            if (type == VTDNavEx.TOKEN_CHARACTER_DATA || type == VTDNavEx.TOKEN_CDATA_VAL) {
                if (tag.processText && !handler.skipCharacters()) {
                    final char[] buf = nav.toRawString(ci, range);
                    handler.characters(buf, range[0], range[1], true);
                }
                ci++;
                continue;
            }

            ci++;
        }

        for (int d = maxDepth; d >= 0; d--) {
            if (tags[d] != null) {
                handler.endElement(tags[d]);
                tags[d] = null;
            }
        }

        inStream.clear();
    }

    private int skipAtributes(final VTDNavEx nav, final int first, final int last) throws NavException {
        for (int inner = first; inner < last; inner++) {
            final int innerType = nav.getTokenType(inner);
            switch (innerType) {
                case VTDNavEx.TOKEN_ATTR_NAME:
                case VTDNavEx.TOKEN_ATTR_NS:
                case VTDNavEx.TOKEN_ATTR_VAL:
                    break;
                default:
                    return inner;
            }
        }
        return last;
    }

    private int fillAtributes(final VTDNavEx nav, final int first, final int last, final FB2Tag tag,
            final String[] tagAttrs) throws NavException {

        final int[] range = new int[2];

        for (int i = 0; i < tag.attributes.length; i++) {
            tagAttrs[i] = null;
        }

        for (int inner = first; inner < last; inner++) {
            final int innerType = nav.getTokenType(inner);
            switch (innerType) {
                case VTDNavEx.TOKEN_ATTR_NAME:
                    final char[] nameBuf = nav.toRawString(inner, range);
                    final String[] qName = new String(nameBuf, range[0], range[1]).split(":");
                    final String attrName = qName[qName.length - 1];
                    final int attrIndex = Arrays.binarySearch(tag.attributes, attrName);
                    if (attrIndex >= 0) {
                        final char[] valBuf = nav.toRawString(inner + 1, range);
                        final String attrValue = new String(valBuf, range[0], range[1]);
                        tagAttrs[attrIndex] = attrValue;
                    }
                    inner++;
                    break;
                case VTDNavEx.TOKEN_ATTR_NS:
                case VTDNavEx.TOKEN_ATTR_VAL:
                    break;
                default:
                    return inner;
            }
        }

        return last;
    }

}
