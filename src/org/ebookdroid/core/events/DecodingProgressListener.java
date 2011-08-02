package org.ebookdroid.core.events;

public interface DecodingProgressListener {

    void decodingProgressChanged(int currentlyDecoding);

    public class DecodingProgressEvent extends SafeEvent<DecodingProgressListener> {

        private final int currentlyDecoding;

        public DecodingProgressEvent(final int currentlyDecoding) {
            this.currentlyDecoding = currentlyDecoding;
        }

        @Override
        public void dispatchSafely(final DecodingProgressListener listener) {
            listener.decodingProgressChanged(currentlyDecoding);
        }
    }
}
