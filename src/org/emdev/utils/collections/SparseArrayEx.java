package org.emdev.utils.collections;

import android.util.SparseArray;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SparseArrayEx<T> extends SparseArray<T> implements Iterable<T> {

    private final ThreadLocal<SparseArrayIterator> iterators = new ThreadLocal<SparseArrayEx<T>.SparseArrayIterator>();

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public TLIterator<T> iterator() {
        SparseArrayIterator iter = iterators.get();
        if (iter == null) {
            iter = new SparseArrayIterator();
            return iter;
        }
        iter.remaining = size();
        iter.removalIndex = -1;
        iterators.set(null);
        return iter;
    }

    private class SparseArrayIterator implements TLIterator<T> {

        /** Number of elements remaining in this iteration */
        private int remaining = size();

        /** Index of element that remove() would remove, or -1 if no such elt */
        private int removalIndex = -1;

        private SparseArrayIterator() {
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0) {
                throw new NoSuchElementException();
            }
            removalIndex = size() - remaining;
            remaining--;
            return valueAt(removalIndex);
        }

        @Override
        public void remove() {
            if (removalIndex < 0) {
                throw new IllegalStateException();
            }
            SparseArrayEx.this.remove(keyAt(removalIndex));
            removalIndex = -1;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public void release() {
            iterators.set(this);
        }
    }
}
