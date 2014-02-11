package org.emdev.common.xml.tags;

import java.util.Arrays;

public class XmlTag {

    public static XmlTag UNKNOWN = new XmlTag("<unknown>", (byte) 0, true, false);

    public final byte tag;
    public final String name;
    public final char[] _name;
    public final boolean processChildren;
    public final boolean processText;
    public final String[] attributes;

    XmlTag(final String name, final byte tag, final boolean processChildren, final boolean processText,
            final String... attributes) {
        this.tag = tag;
        this.name = name;
        this._name = name.toCharArray();
        this.processChildren = processChildren;
        this.processText = processText;
        this.attributes = attributes;

        if (this.attributes.length > 1) {
            Arrays.sort(this.attributes);
        }
    }

}
