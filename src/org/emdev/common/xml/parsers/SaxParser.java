package org.emdev.common.xml.parsers;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.emdev.common.xml.IContentHandler;
import org.emdev.common.xml.IXmlTagFactory;
import org.emdev.common.xml.TextProvider;
import org.emdev.common.xml.tags.XmlTag;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxParser extends DefaultHandler {

    private static final SAXParserFactory spf = SAXParserFactory.newInstance();

    private final String[] attrs = new String[2];

    private final XmlTag[] stack = new XmlTag[64];

    private int depth;

    private IContentHandler handler;
    private IXmlTagFactory factory;

    public void parse(final Reader isr, final IXmlTagFactory factory, final IContentHandler handler)
            throws ParserConfigurationException, SAXException, IOException {
        this.handler = handler;
        this.factory = factory;
        this.depth = 0;

        final InputSource is = new InputSource();
        is.setCharacterStream(isr);
        final SAXParser parser = spf.newSAXParser();
        parser.parse(is, this);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {

        final XmlTag tag = factory.getTagByName(qName);
        stack[depth++] = tag;

        if (handler.parseAttributes(tag)) {
            for (int i = 0; i < tag.attributes.length; i++) {
                attrs[i] = attributes.getValue(tag.attributes[i]);
            }
            handler.startElement(tag, attrs);
        } else {
            handler.startElement(tag);
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) {
        if (!handler.skipCharacters()) {
            handler.characters(new TextProvider(false, ch), start, length);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        final XmlTag tag = stack[--depth];
        handler.endElement(tag);
    }
}
