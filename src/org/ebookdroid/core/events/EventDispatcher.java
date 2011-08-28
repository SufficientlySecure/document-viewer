package org.ebookdroid.core.events;

import java.util.ArrayList;

public class EventDispatcher {

    private final ArrayList<Object> listeners = new ArrayList<Object>();

    public void dispatch(final Event<?> event) {
        for (final Object listener : listeners) {
            event.dispatchOn(listener);
        }
    }

    public void addEventListener(final Object listener) {
        listeners.add(listener);
    }

    public void removeEventListener(final Object listener) {
        listeners.remove(listener);
    }
}
