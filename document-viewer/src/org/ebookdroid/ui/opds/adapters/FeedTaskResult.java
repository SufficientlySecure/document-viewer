package org.ebookdroid.ui.opds.adapters;

import org.ebookdroid.opds.exceptions.OPDSException;
import org.ebookdroid.opds.model.Feed;

public class FeedTaskResult {

    public Feed feed;
    public OPDSException error;

    public FeedTaskResult(final Feed feed) {
        this.feed = feed;
    }

    public FeedTaskResult(final Feed feed, final OPDSException error) {
        this.feed = feed;
        this.error = error;
    }
}