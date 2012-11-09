package org.emdev.utils.collections;

import java.util.Iterator;


public interface TLIterator<E> extends Iterator<E>, Iterable<E> {

    void release();
}
