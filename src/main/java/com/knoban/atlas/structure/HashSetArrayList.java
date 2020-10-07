package com.knoban.atlas.structure;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Alden Bansemer (kNoAPP)
 * @param <T> Generic data type to store
 */
public class HashSetArrayList<T> implements Set<T>, List<T>, RandomAccess, Cloneable, Serializable {

    private HashSet<T> set = new HashSet<>();
    private ArrayList<T> array = new ArrayList<>();

    @Override
    public boolean add(T e) {
        boolean toRet = set.add(e);
        if(toRet) {
            array.add(e);
        }
        return toRet;
    }

    @Override
    public boolean remove(Object o) {
        boolean toRet = set.remove(o);
        if(toRet) {
            array.remove(o);
        }
        return toRet;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        boolean toRet = set.addAll(c);
        if(toRet) {
            array.addAll(c);
        }
        return toRet;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        boolean toRet = set.addAll(c);
        if(toRet) {
            array.addAll(index, c);
        }
        return toRet;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean toRet = set.removeAll(c);
        if(toRet) {
            array.removeAll(c);
        }
        return toRet;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean toRet = set.retainAll(c);
        if(toRet) {
            array.retainAll(c);
        }
        return toRet;
    }

    @Override
    public T get(int index) {
        return array.get(index);
    }

    @Override
    public T set(int index, T element) {
        T toRemove = array.set(index, element);
        if(toRemove != null)
            set.remove(toRemove);
        set.add(element);
        return toRemove;
    }

    @Override
    public void add(int index, T element) {
        array.add(index, element);
        set.add(element);
    }

    @Override
    public T remove(int index) {
        T toRemove = array.remove(index);
        set.remove(toRemove);
        return toRemove;
    }

    @Override
    public int indexOf(Object o) {
        return array.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return array.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return array.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return array.listIterator(index);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return array.subList(fromIndex, toIndex);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        array.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return array.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return array.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return array.parallelStream();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean isEmpty() {
        return array.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return array.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return array.toArray(a);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return array.removeIf(filter) && set.removeIf(filter);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        array.sort(c);
    }

    @Override
    public void clear() {
        array.clear();
        set.clear();
    }
}
