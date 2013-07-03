package org.emdev.common.xml.tags;

import java.util.HashMap;

import org.emdev.common.xml.IXmlTagFactory;
import org.emdev.utils.collections.SymbolTree;

public class BaseXmlTagFactory implements IXmlTagFactory {

    protected final HashMap<String, XmlTag> tagsByName = new HashMap<String, XmlTag>(256, 0.2f);
    protected final SymbolTree<XmlTag> tagsTree = new SymbolTree<XmlTag>();

    public XmlTag tag(final String name, final byte tag, final boolean processChildren, final boolean processText,
            final String... attributes) {

        final XmlTag t = new XmlTag(name, tag, processChildren, processText, attributes);
        tagsByName.put(t.name, t);
        tagsTree.add(t, t.name);
        return t;
    }

    @Override
    public XmlTag getTagByName(final String name) {
        XmlTag tag = tagsByName.get(name);
        if (tag == null) {
            final String upperCaseName = name.toLowerCase().intern();
            tag = tagsByName.get(upperCaseName);
            if (tag == null) {
                tag = XmlTag.UNKNOWN;
                tagsByName.put(upperCaseName, tag);
            }
            tagsByName.put(name, tag);
        }
        return tag;
    }

    @Override
    public XmlTag getTagByName(final char[] ch, final int start, final int length) {
        final XmlTag t = tagsTree.get(ch, start, length);
        return t != null ? t : XmlTag.UNKNOWN;
    }

}
