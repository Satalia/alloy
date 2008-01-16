/*
 * Alloy Analyzer 4 -- Copyright (c) 2006-2008, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.io.Serializable;

/**
 * This implements an unmodifiable list.
 *
 * <p><b>Thread Safety:</b>  Safe.
 *
 * @param <T> - the type of element
 */

public final class ConstList<T> implements Serializable, List<T> {

    /**
     * This implements a modifiable list that can be used to construct a ConstList.
     *
     * <p><b>Thread Safety:</b>  Not safe.
     *
     * @param <T> - the type of element
     */
    public static final class TempList<T> {
        /** The underlying list. */
        private final ArrayList<T> list;
        /** Nonnull iff this list is no longer modifiable. */
        private ConstList<T> clist=null;
        /** Construct a new empty modifiable TempList. */
        public TempList()                          { this.list = new ArrayList<T>(); }
        /** Construct a new empty modifiable TempList with an initial capacity of n. */
        public TempList(int n)                     { this.list = new ArrayList<T>(n>0?n:0); }
        /** Construct a new modifiable TempList with the initial content being n references to the given elem (if n<=0, the initial list is empty) */
        public TempList(int n, T elem)             { this.list = new ArrayList<T>(n>0?n:0); while(n>0) {list.add(elem); n--;} }
        /** Construct a new modifiable TempList with the initial content equal to the given collection. */
        public TempList(Collection<? extends T> collection)  { this.list = new ArrayList<T>(collection); }
        /** Returns a String representation. */
        @Override public String toString()         { return list.toString(); }
        /** Returns the size of the list. */
        public int size()                          { return list.size(); }
        /** Returns true if the element is in the list. */
        public boolean contains(Object elem)       { return list.contains(elem); }
        /** Returns the i-th element. */
        public T get(int index)                    { return list.get(index); }
        /** Sort the list using the given comparator. */
        public void sort(Comparator<T> comparator) { if (clist!=null) throw new UnsupportedOperationException(); Collections.sort(list,comparator); }
        /** Removes then returns the i-th element. */
        public T remove(int index)                 { if (clist!=null) throw new UnsupportedOperationException(); return list.remove(index); }
        /** Removes the first occurrence of the element (if it exists). */
        public boolean remove(T elem)              { if (clist!=null) throw new UnsupportedOperationException(); return list.remove(elem); }
        /** Add the given element at the given index. */
        public void add(int index, T elem)         { if (clist!=null) throw new UnsupportedOperationException(); list.add(index,elem); }
        /** Append the given element to the list. */
        public void add(T elem)                    { if (clist!=null) throw new UnsupportedOperationException(); list.add(elem); }
        /** Append the elements in the given collection to the list. */
        public void addAll(Collection<T> all)      { if (clist!=null) throw new UnsupportedOperationException(); list.addAll(all); }
        /** Change the i-th element to be the given element. */
        public void set(int index, T elem)         { if (clist!=null) throw new UnsupportedOperationException(); list.set(index,elem); }
        /** Turns this TempList unmodifiable, then construct a ConstList backed by this TempList. */
        public ConstList<T> makeConst() { if (clist==null) { if (list.isEmpty()) clist=make(); else clist=new ConstList<T>(true,list); } return clist; }
    }

    /** This ensures the class can be serialized reliably. */
    private static final long serialVersionUID = 1L;

    /** The underlying Collections.unmodifiableList. */
    private final List<T> list;

    /** This caches an unmodifiable empty list. */
    private static final ConstList<Object> emptylist = new ConstList<Object>(true, new ArrayList<Object>(0));

    /** Construct an unmodifiable list with the given list as its backing store. */
    private ConstList(boolean makeReadOnly, List<T> list) {
        this.list = makeReadOnly ? Collections.unmodifiableList(list) : list;
    }

    /** Return an unmodifiable empty list. */
    @SuppressWarnings("unchecked")
    public static<T> ConstList<T> make() {
        return (ConstList<T>) emptylist;
    }

    /**
     * Return an unmodifiable list consisting of "n" references to "elem".
     * (If n<=0, we'll return an unmodifiable empty list)
     */
    public static<T> ConstList<T> make(int n, T elem) {
        if (n<=0) return make();
        ArrayList<T> ans=new ArrayList<T>(n);
        while(n>0) { ans.add(elem); n--; }
        return new ConstList<T>(true, ans);
    }

    /**
     * Return an unmodifiable list with the same elements as the given collection.
     * (If collection==null, we'll return an unmodifiable empty list)
     */
    public static<T> ConstList<T> make(Iterable<T> collection) {
        if (collection instanceof ConstList) return (ConstList<T>)collection;
        if (collection==null) return make();
        ArrayList<T> ans=null;
        Iterator<? extends T> it=collection.iterator();
        while(it.hasNext()) {
            if (ans==null) ans=new ArrayList<T>();
            ans.add(it.next());
        }
        if (ans==null) return make(); else return new ConstList<T>(true, ans);
    }

    /**
     * Return an unmodifiable list with the same elements as the given collection.
     * (If collection==null, we'll return an unmodifiable empty list)
     */
    public static<T> ConstList<T> make(Collection<T> collection) {
        if (collection instanceof ConstList) return (ConstList<T>)collection;
        if (collection==null || collection.isEmpty()) return make();
        return new ConstList<T>(true, new ArrayList<T>(collection));
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object that) {
        if (this==that) return true;
        if (!(that instanceof List)) return false;
        return list.equals(that);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() { return list.hashCode(); }

    /** {@inheritDoc} */
    @Override public String toString() { return list.toString(); }

    /** {@inheritDoc} */
    public boolean contains(Object item) { return list.contains(item); }

    /** {@inheritDoc} */
    public boolean containsAll(Collection<?> collection) { return list.containsAll(collection); }

    /** {@inheritDoc} */
    public T get(int i) { return list.get(i); }

    /** {@inheritDoc} */
    public int size() { return list.size(); }

    /** {@inheritDoc} */
    public boolean isEmpty() { return list.isEmpty(); }

    /** {@inheritDoc} */
    public Iterator<T> iterator() { return list.iterator(); }

    /** {@inheritDoc} */
    public Object[] toArray() { return list.toArray(); }

    /** {@inheritDoc} */
    public <E> E[] toArray(E[] a) { return list.toArray(a); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean add(T o) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean remove(Object o) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean addAll(int index, Collection<? extends T> c) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public void clear() { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public T set(int index, T element) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public void add(int index, T element) { throw new UnsupportedOperationException(); }

    /** This list is readonly, so this method always throws UnsupportedOperationException. */
    public T remove(int index) { throw new UnsupportedOperationException(); }

    /** {@inheritDoc} */
    public int indexOf(Object o) { return list.indexOf(o); }

    /** {@inheritDoc} */
    public int lastIndexOf(Object o) { return list.lastIndexOf(o); }

    /** {@inheritDoc} */
    public ListIterator<T> listIterator() { return list.listIterator(); }

    /** {@inheritDoc} */
    public ListIterator<T> listIterator(int index) { return list.listIterator(index); }

    /** {@inheritDoc} */
    public ConstList<T> subList(int from, int to) {
        if (from<0) from=0;
        if (to>size()) to=size();
        if (from==0 && to==size()) return this;
        return new ConstList<T>(false, list.subList(from,to));
    }
}
