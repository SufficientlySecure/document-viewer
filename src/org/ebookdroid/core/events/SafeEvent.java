package org.ebookdroid.core.events;

import java.lang.reflect.Method;

public abstract class SafeEvent<T> implements Event<T> {

    private final Class<?> listenerType;

    protected SafeEvent() {
        listenerType = getListenerType();
    }

    private Class<?> getListenerType() {
        for (final Method method : getClass().getMethods()) {
            if ("dispatchSafely".equals(method.getName()) && !method.isSynthetic()) {
                return method.getParameterTypes()[0];
            }
        }
        throw new RuntimeException("Couldn't find dispatchSafely method");
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public final void dispatchOn(final Object listener) {
        if (listenerType.isAssignableFrom(listener.getClass())) {
            dispatchSafely((T) listener);
        }
    }

    public abstract void dispatchSafely(T listener);
}
