/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;

import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents the Ruby {@code Hash} class.
 */
public class RubyHash extends RubyObject {

    /**
     * The class from which we create the object that is {@code Hash}. A subclass of
     * {@link org.jruby.truffle.runtime.core.RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyHash} rather than a normal {@link org.jruby.truffle.runtime.core.RubyBasicObject}.
     */
    public static class RubyHashClass extends RubyClass {

        public RubyHashClass(RubyClass objectClass) {
            super(null, objectClass, "Hash");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyHash(this, null, null, 0);
        }

    }

    private RubyProc defaultBlock;
    private Object store;
    private int storeSize;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object store, int storeSize) {
        super(rubyClass);

        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length == RubyContext.HASHES_SMALL * 2;
        assert !(store instanceof Object[]) || storeSize <= RubyContext.HASHES_SMALL;

        this.defaultBlock = defaultBlock;
        this.store = store;
        this.storeSize = storeSize;
    }

    public RubyProc getDefaultBlock() {
        return defaultBlock;
    }

    public Object getStore() {
        return store;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setDefaultBlock(RubyProc defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public void setStore(Object store, int storeSize) {
        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length == RubyContext.HASHES_SMALL * 2;
        assert !(store instanceof Object[]) || storeSize <= RubyContext.HASHES_SMALL;


        this.store = store;
        this.storeSize = storeSize;
    }

    public void setStoreSize(int storeSize) {
        assert storeSize <= RubyContext.HASHES_SMALL;
        this.storeSize = storeSize;
    }

}
