package org.ebookdroid.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListWrapper<T> implements List<T> {

    private final List<T> target;

    public ListWrapper() {
        this.target = new ArrayList<T>();
    }

    public ListWrapper(List<T> target) {
        this.target = target;
    }

    @Override
    public void add(int location, T object) {
        target.add(location, object);
    }

    @Override
    public boolean add(T object) {
        return target.add(object);
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        return target.addAll(location, collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        return target.addAll(collection);
    }

    @Override
    public void clear() {
        throw new IllegalStateException("Items cannot be deleted");
    }

    @Override
    public boolean contains(Object object) {
        return target.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return target.containsAll(collection);
    }

    @Override
    public T get(int location) {
        return target.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return target.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return target.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            final Iterator<T> i = target.iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                return i.next();
            }

            @Override
            public void remove() {
                throw new IllegalStateException("Items cannot be deleted");
            }
        };
    }

    @Override
    public int lastIndexOf(Object object) {
        return target.lastIndexOf(object);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ListIterator<T>() {

            final ListIterator<T> i = target.listIterator();

            @Override
            public void add(T object) {
                i.add(object);
            }

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public boolean hasPrevious() {
                return i.hasPrevious();
            }

            @Override
            public T next() {
                return i.next();
            }

            @Override
            public int nextIndex() {
                return i.nextIndex();
            }

            @Override
            public T previous() {
                return i.previous();
            }

            @Override
            public int previousIndex() {
                return i.previousIndex();
            }

            @Override
            public void remove() {
                throw new IllegalStateException("Items cannot be deleted");
            }

            @Override
            public void set(T object) {
                throw new IllegalStateException("Items cannot be replaces");
            }
        };
    }

    @Override
    public ListIterator<T> listIterator(final int location) {
        return new ListIterator<T>() {

            final ListIterator<T> i = target.listIterator(location);

            @Override
            public void add(T object) {
                i.add(object);
            }

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public boolean hasPrevious() {
                return i.hasPrevious();
            }

            @Override
            public T next() {
                return i.next();
            }

            @Override
            public int nextIndex() {
                return i.nextIndex();
            }

            @Override
            public T previous() {
                return i.previous();
            }

            @Override
            public int previousIndex() {
                return i.previousIndex();
            }

            @Override
            public void remove() {
                throw new IllegalStateException("Items cannot be deleted");
            }

            @Override
            public void set(T object) {
                throw new IllegalStateException("Items cannot be replaces");
            }
        };
    }

    @Override
    public T remove(int location) {
        throw new IllegalStateException("Items cannot be deleted");
    }

    @Override
    public boolean remove(Object object) {
        throw new IllegalStateException("Items cannot be deleted");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new IllegalStateException("Items cannot be deleted");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new IllegalStateException("Items cannot be deleted");
    }

    @Override
    public T set(int location, T object) {
        throw new IllegalStateException("Items cannot be replaces");
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public List<T> subList(int start, int end) {
        return target.subList(start, end);
    }

    @Override
    public Object[] toArray() {
        return target.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return target.toArray(array);
    }

}
