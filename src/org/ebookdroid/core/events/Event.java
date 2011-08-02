package org.ebookdroid.core.events;

public interface Event<T> {

    void dispatchOn(Object listener);
}
