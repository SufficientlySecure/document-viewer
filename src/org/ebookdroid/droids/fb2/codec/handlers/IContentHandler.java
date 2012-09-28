package org.ebookdroid.droids.fb2.codec.handlers;

import org.ebookdroid.droids.fb2.codec.FB2Tag;

public interface IContentHandler {

    boolean parseAttributes(FB2Tag tag);

    void startElement(final FB2Tag tag, final String... attributes);

    boolean skipCharacters();

    void characters(final char[] ch, final int start, final int length, boolean persistent);

    void endElement(final FB2Tag tag);

}
