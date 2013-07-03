package org.ebookdroid.opds.xml;

import org.ebookdroid.common.settings.OpdsSettings;
import org.ebookdroid.opds.IEntryBuilder;
import org.ebookdroid.opds.model.BookDownloadLink;
import org.ebookdroid.opds.model.Content;
import org.ebookdroid.opds.model.Feed;
import org.ebookdroid.opds.model.Link;
import org.ebookdroid.opds.model.LinkKind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.emdev.common.xml.IContentHandler;
import org.emdev.common.xml.TextProvider;
import org.emdev.common.xml.parsers.DuckbillParser;
import org.emdev.common.xml.tags.XmlTag;
import org.emdev.utils.LengthUtils;

public class OPDSContentHandler implements IContentHandler {

    final Feed feed;
    final IEntryBuilder builder;

    private boolean inEntry;
    private boolean grabContent;

    private final StringBuilder buf = new StringBuilder();
    private final Map<String, String> values = new HashMap<String, String>();
    private final Map<String, Link> facets = new LinkedHashMap<String, Link>();

    private Link feedLink;

    private Link bookThumbnail;
    private List<BookDownloadLink> bookLinks;

    private final Set<String> unsupportedTypes = new HashSet<String>();

    public OPDSContentHandler(final Feed feed, final IEntryBuilder builder) {
        this.feed = feed;
        this.builder = builder;
    }

    public void parse(final String content) throws Exception {
        TextProvider text = new TextProvider(content);
        DuckbillParser p = new DuckbillParser();
        p.parse(text, OPDSTagFactory.instance, this);
    }

    @Override
    public void startElement(final XmlTag tag, final String... attributes) {
        buf.setLength(0);
        if (inEntry) {
            if (tag == OPDSTag.CONTENT.tag) {
                values.put("content@type", attributes[0]);
                grabContent = true;
            } else if (tag == OPDSTag.LINK.tag) {
                final String ref = attributes[0];
                final String rel = attributes[1];
                final String title = attributes[2];
                final String type = attributes[3];

                final LinkKind kind = LinkKind.valueOf(rel, type);
                switch (kind) {
                    case FEED:
                        feedLink = new Link(kind, ref, rel, type);
                        break;
                    case FACET_FEED:
                        if (LengthUtils.isNotEmpty(title)) {
                            facets.put(title, new Link(kind, ref, rel, type));
                        }
                        break;
                    case BOOK_DOWNLOAD:
                        final BookDownloadLink bdl = new BookDownloadLink(kind, ref, rel, type);
                        if (bdl.bookType == null) {
                            if (unsupportedTypes.add(type)) {
                                final String entryTitle = values.get("title");
                                System.out.println(entryTitle + ": Unsupported mime type: " + type);
                            }
                        }

                        final OpdsSettings s = OpdsSettings.current();
                        if (!s.filterTypes || bdl.bookType != null && (!bdl.isZipped || s.downloadArchives)) {
                            if (bookLinks == null) {
                                bookLinks = new LinkedList<BookDownloadLink>();
                            }
                            bookLinks.add(bdl);
                        }
                        break;
                    case BOOK_THUMBNAIL:
                        bookThumbnail = new Link(kind, ref, rel, type);
                        break;
                    default:
                        break;
                }
            } else {
                grabContent = tag == OPDSTag.ID.tag || tag == OPDSTag.TITLE.tag;
            }
        } else {
            if (tag == OPDSTag.ENTRY.tag) {
                inEntry = true;
                values.clear();
                facets.clear();
                feedLink = null;
                bookThumbnail = null;
                bookLinks = null;
            } else if (tag == OPDSTag.LINK.tag) {
                final String ref = attributes[0];
                final String rel = attributes[1];
                final String type = attributes[3];
                final LinkKind kind = LinkKind.valueOf(rel, type);
                if (kind == LinkKind.NEXT_FEED) {
                    feed.next = new Feed(feed.parent, ref, feed.title, feed.content);
                    feed.next.link = new Link(kind, ref, rel, type);
                    feed.next.next = null;
                    feed.next.prev = feed;
                }
            }
        }
    }

    @Override
    public void endElement(final XmlTag tag) {
        if (inEntry) {
            if (grabContent) {
                values.put(tag.name, buf.toString());
            } else if (tag == OPDSTag.ENTRY.tag) {
                inEntry = false;
                final String contentString = values.get("content");
                final String contentType = values.get("content@type");
                final Content content = contentString != null ? new Content(contentType, contentString) : null;
                final String entryId = values.get("id");
                final String entryTitle = values.get("title");
                if (LengthUtils.isNotEmpty(bookLinks)) {
                    feed.books.add(builder.newBook(feed, entryId, entryTitle, content, bookThumbnail, bookLinks));
                } else if (feedLink != null || !facets.isEmpty()) {
                    feed.children.add(builder.newFeed(feed, entryId, entryTitle, content, feedLink, facets));
                }
                values.clear();
                facets.clear();
            }
        }
        grabContent = false;
        buf.setLength(0);
    }

    @Override
    public boolean parseAttributes(final XmlTag tag) {
        return LengthUtils.isNotEmpty(tag.attributes);
    }

    @Override
    public boolean skipCharacters() {
        return !grabContent;
    }

    @Override
    public void characters(final TextProvider p, final int start, final int length) {
        buf.append(p.chars, start, length);
    }
}
