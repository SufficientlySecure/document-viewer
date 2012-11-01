package org.ebookdroid.droids.fb2.codec.tags;


import org.emdev.common.xml.tags.BaseXmlTagFactory;
import org.emdev.common.xml.tags.XmlTag;

public class FB2TagFactory extends BaseXmlTagFactory {

    public static final FB2TagFactory instance = new FB2TagFactory();

    private FB2TagFactory() {
    }

    @Override
    public XmlTag getTagByName(final char[] ch, final int start, final int length) {
        switch (length) {
            case 1:
                switch (ch[start]) {
                    case 'p':
                        return FB2Tag.P.tag;
                    case 'v':
                        return FB2Tag.V.tag;
                    case 'a':
                        return FB2Tag.A.tag;
                }
                return XmlTag.UNKNOWN;
            case 2:
                switch (ch[start]) {
                    case 't':
                        switch (ch[start + 1]) {
                            case 'r':
                                return FB2Tag.TR.tag;
                            case 'd':
                                return FB2Tag.TD.tag;
                            case 'h':
                                return FB2Tag.TH.tag;
                        }
                        return XmlTag.UNKNOWN;
                    case 'b':
                        switch (ch[start + 1]) {
                            case 'r':
                                return FB2Tag.BR.tag;
                        }
                        return XmlTag.UNKNOWN;
                    case 'u':
                        switch (ch[start + 1]) {
                            case 'l':
                                return FB2Tag.UL.tag;
                        }
                        return XmlTag.UNKNOWN;
                    case 'l':
                        switch (ch[start + 1]) {
                            case 'i':
                                return FB2Tag.LI.tag;
                        }
                        return XmlTag.UNKNOWN;
                }
                return XmlTag.UNKNOWN;
            case 3:
                if (ch[start] == 's' && ch[start + 1] == 'u') {
                    if (ch[start + 2] == 'p') {
                        return FB2Tag.SUP.tag;
                    } else if (ch[start + 2] == 'b') {
                        return FB2Tag.SUB.tag;
                    }
                }
                return XmlTag.UNKNOWN;
            default:
                final XmlTag t = tagsTree.get(ch, start, length);
                return t != null ? t : XmlTag.UNKNOWN;
            case 0:
                return XmlTag.UNKNOWN;
        }
    }

}
