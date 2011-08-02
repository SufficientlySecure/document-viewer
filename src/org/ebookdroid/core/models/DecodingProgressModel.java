package org.ebookdroid.core.models;

import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.events.EventDispatcher;

public class DecodingProgressModel extends EventDispatcher {

    private int currentlyDecoding;

    public void increase() {
        currentlyDecoding++;
        dispatchChanged();
    }

    private void dispatchChanged() {
        dispatch(new DecodingProgressListener.DecodingProgressEvent(currentlyDecoding));
    }

    public void decrease() {
        currentlyDecoding--;
        dispatchChanged();
    }
}
