/*
 * RubyList.java
 *
 * Created on 28. Oktober 2001, 13:12
 */

package org.jruby.util;

import java.util.*;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyList extends AbstractList {
    private List delegate;

    public RubyList() {
        this(new ArrayList());
    }
    
    public RubyList(RubyObject[] array) {
        this(new ArrayList(Arrays.asList(array)));
    }
    
    public RubyList(List delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate have to be != null");
        }
        
        this.delegate = delegate;
    }

    public RubyList(int size, Object defaultValue) {
        this(new ArrayList(size));
        
        for (int i = 0; i < size; i++) {
            delegate.add(defaultValue);
        }
    }
    
    public int size() {
        return delegate.size();
    }
    
    public Object get(int index) {
        return delegate.get(index);
    }

    public void add(int index, Object element) {
        delegate.add(index, element);
    }
    
    public Object remove(int index) {
        return delegate.remove(index);
    }
    
    public RubyObject[] toRubyArray() {
        return (RubyObject[])delegate.toArray(new RubyObject[delegate.size()]);
    }
    
    public void copy(RubyList other, int len) {
        for (int i = 0; i < len; i++) {
            set(i, other.get(i));
        }
    }
}