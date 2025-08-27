package com.growingio.android.sdk.utils;

import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * An value in a <tt>WeakSet</tt> will automatically be removed when
 * it is no longer in ordinary use.
 *
 *
 * <strong>This class not thread-safe</strong>
 * <strong>此类暂时没有支持所有的方法， 如有需要， 请自行添加</strong>
 * @see java.util.WeakHashMap
 * Created by liangdengke on 2018/8/6.
 */
public class WeakSet<T> implements Set<T> {

    private transient WeakHashMap<T, Object> map;
    private static final Object PRESENT = new Object();

    @Override
    public int size() {
        if (map == null)
            return 0;
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (isEmpty() || o == null)
            return false;
        return map.containsKey(o);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        if (isEmpty())
            return EmptyIterator.EMPTY_ITERATOR;
        return map.keySet().iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("method toArray() not supported");
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        throw new UnsupportedOperationException("method toArray(T[] a) not supported");
    }

    @Override
    public boolean add(T t) {
        if (t == null){
            throw new IllegalArgumentException("The argument t can't be null");
        }
        if (map == null)
            map = new WeakHashMap<>();
        return map.put(t, PRESENT) != null;
    }

    @Override
    public boolean remove(Object o) {
        if (isEmpty() || o == null)
            return false;
        return map.remove(o) == PRESENT;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("method containsAll not supported");
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        throw new UnsupportedOperationException("method addAll not supported now");
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("method retainAll not supported now");
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("method removeAll not supported now");
    }

    @Override
    public void clear() {
        if (map != null)
            map.clear();
    }

    private static class NonEmptyIterator<E> implements Iterator<E>{

        private final Iterator<WeakReference<E>> iterator;

        private NonEmptyIterator(Iterator<WeakReference<E>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next().get();
        }
    }

    private static class EmptyIterator<E> implements Iterator<E>{

        private static EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new UnsupportedOperationException("EmptyIterator should not call this method directly");
        }
    }
}
