package org.jruby.util.collections;

import java.util.*;

public class DelegateList extends AbstractList {
    private List delegate = null;
    private int from = 0;
    private int to = 0;
    
    public DelegateList(List delegate, int from, int to) {
        this.delegate = delegate;
        
        if (from > to) {
            throw new IllegalArgumentException("from > to");
        }
        
        if (to > delegate.size()) {
            throw new IllegalArgumentException("to > delegate.size()");
        }

        this.from = from;
        this.to = to;
    }

    /*
     * @see List#get(int)
     */
    public Object get(int index) {
        if (index < from || index >= to) {
            throw new IndexOutOfBoundsException("index must be: from <= index < to");
        }

        return delegate.get(index + from);
    }

    /*
     * @see AbstractCollection#size()
     */
    public int size() {
        return to - from;
    }
}