package org.jruby.util.collections;

import java.util.*;

public class ExtendedList extends AbstractList {
    private List delegate = null;
    private int start = 0;

    /**
     * Constructor for ExtendedList.
     */
    public ExtendedList() {
        this(new ArrayList(), 0);
    }
    
    public ExtendedList(List delegate) {
        this(delegate, 0);
    }

    public ExtendedList(List delegate, int start) {
        super();
        
        this.delegate = delegate;
        this.start = start;
    }
    
    public ExtendedList(int size, Object defaultValue) {
        this(new ArrayList(size), 0);
        
        for (int i = 0; i < size; i++) {
        	add(defaultValue);
        }
    }

    /*
     * @see List#get(int)
     */
    public Object get(int index) {
        return delegate.get(index + start);
    }

    /*
     * @see AbstractCollection#size()
     */
    public int size() {
        return delegate.size() - start;
    }
    
    public void add(int index, Object element) {
        delegate.add(index + start, element);
    }
    
    public Object set(int index, Object element) {
        return delegate.set(index + start, element);
    }
    
    public Object remove(int index) {
        return delegate.remove(index + start);
    }
    
    public void copy(List other, int len) {
        for (int i = 0; i < len; i++) {
            set(i, other.get(i));
        }
    }
    
    public List getDelegate() {
        return delegate;
    }
}