package org.emdev.common.xml;

import org.emdev.common.xml.tags.XmlTag;


public interface IXmlTagFactory {

    XmlTag getTagByName(final String name);

    XmlTag getTagByName(final char[] ch, final int start, final int length);

}
