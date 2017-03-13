package ru.ifmo.ctddev.turaev.arrayset;

import java.util.*;
import static java.util.Collections.binarySearch;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private List<E> data;
    private Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(ArraySet<E> other) {
        this(other.data, other.comparator);
    }

    private ArraySet(List<E> subList, Comparator<? super E> comparator) {
        data = subList;
        this.comparator = comparator;
    }


    public ArraySet(Collection<? extends E> other) {
        this(other, null);
    }

    public ArraySet(Collection<? extends E> other, Comparator<? super E> comparator) {
        this.comparator = comparator;
        Set<E> buffer  = new TreeSet<>(comparator);
        buffer.addAll(other);
        data = new ArrayList<>(buffer);
    }


    private int binarySearchHelper(E e, int posit, int negat) {
        int pos = binarySearch(data, e, comparator);
        return pos >= 0 ? pos + posit : -pos - 1 + negat;
    }

    private E getOrNull(int pos) {
        return 0 <= pos && pos < data.size() ? data.get(pos) : null;
    }

    @Override
    public E lower(E e) {
        return getOrNull(binarySearchHelper(e, -1, -1));
    }

    @Override
    public E floor(E e) {
        return getOrNull(binarySearchHelper(e, 0, -1));
    }

    @Override
    public E ceiling(E e) {
        return getOrNull(binarySearchHelper(e, 0, 0));
    }


    @Override
    public E higher(E e) {
        return getOrNull(binarySearchHelper(e, 1, 0));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is not mutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is not mutable");
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean contains(Object o) {
        return binarySearch(data, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedArrayList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedArrayList<>(data).iterator();
    }



    @Override
    public NavigableSet<E> subSet(E e, boolean b, E e1, boolean b1) {
        int left = binarySearchHelper(e, b ? 0 : 1, 0);
        int right = binarySearchHelper(e1, b1 ? 0 : -1, -1);
        return left >= right + 1 ? Collections.emptyNavigableSet() : new ArraySet<>(data.subList(left, right + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E e, boolean b) {
        return data.isEmpty() ? Collections.emptyNavigableSet() : subSet(data.get(0), true, e, b);
    }

    @Override
    public NavigableSet<E> tailSet(E e, boolean b) {
        return data.isEmpty() ? Collections.emptyNavigableSet() : subSet(e, b, data.get(size() - 1), true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E e, E e1) {
        return subSet(e, true, e1, false);
    }

    @Override
    public SortedSet<E> headSet(E e) {
        return headSet(e, false);
    }

    @Override
    public SortedSet<E> tailSet(E e) {
        return tailSet(e, true);
    }

    @Override
    public E first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public E last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(data.size() - 1);
    }

    private class ReversedArrayList<G> extends AbstractList<G> {
        final boolean isReversed;
        final List<G> data;

        private ReversedArrayList(List<G> data) {
            if (data instanceof ReversedArrayList) {
                ReversedArrayList<G> temp = (ReversedArrayList<G>) data;
                this.isReversed = !temp.isReversed;
                this.data = temp.data;
            } else {
                this.isReversed = true;
                this.data = data;
            }
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public G get(int i) {
            return !isReversed ? data.get(i) : data.get(size() - i - 1);
        }
    }
}


