package org.jruby.util;

import java.util.ArrayList;
import java.util.Collection;

public class UnsynchronizedStack extends ArrayList {
    private static final long serialVersionUID = 5627466606696890874L;

    public UnsynchronizedStack() {
        super();
    }

    public UnsynchronizedStack(int initialCapacity) {
        super(initialCapacity);
    }

    public UnsynchronizedStack(Collection c) {
        super(c);
    }

    public void push(Object o) {
        add(o);
    }
    
    public Object peek() {
        return get(size() - 1);
    }
    
    public Object pop() {
        return remove(size() - 1);
    }
}
