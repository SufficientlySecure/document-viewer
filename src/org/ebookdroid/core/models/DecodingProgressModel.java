package org.ebookdroid.core.models;

import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.events.ListenerProxy;

public class DecodingProgressModel extends ListenerProxy {

    private int currentlyDecoding;

    public DecodingProgressModel() {
        super(DecodingProgressListener.class);
    }

    public void increase() {
        currentlyDecoding++;
        this.<DecodingProgressListener> getListener().decodingProgressChanged(currentlyDecoding);
    }

    public void decrease() {
        currentlyDecoding--;
        this.<DecodingProgressListener> getListener().decodingProgressChanged(currentlyDecoding);
    }
}
