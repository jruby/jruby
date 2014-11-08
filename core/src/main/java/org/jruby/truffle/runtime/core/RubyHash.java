/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.util.cli.Options;

/**
 * Represents the Ruby {@code Hash} class.
 */
public class RubyHash extends RubyObject {

    public static final int HASHES_SMALL = Options.TRUFFLE_HASHES_SMALL.load();

    /**
     * The class from which we create the object that is {@code Hash}. A subclass of
     * {@link org.jruby.truffle.runtime.core.RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyHash} rather than a normal {@link org.jruby.truffle.runtime.core.RubyBasicObject}.
     */
    public static class RubyHashClass extends RubyClass {

        public RubyHashClass(RubyClass objectClass) {
            super(null, objectClass, "Hash");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyHash(this, null, null, null, 0);
        }

    }

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int storeSize;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int storeSize) {
        super(rubyClass);

        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length == HASHES_SMALL * 2;
        assert !(store instanceof Object[]) || storeSize <= HASHES_SMALL;

        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.storeSize = storeSize;
    }

    public RubyProc getDefaultBlock() {
        return defaultBlock;
    }

    public Object getDefaultValue() {
        return defaultValue;
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

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setStore(Object store, int storeSize) {
        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length == HASHES_SMALL * 2;
        assert !(store instanceof Object[]) || storeSize <= HASHES_SMALL;


        this.store = store;
        this.storeSize = storeSize;
    }

    public void setStoreSize(int storeSize) {
        assert storeSize <= HASHES_SMALL;
        this.storeSize = storeSize;
    }

    public Map<Object, Object> slowToMap() {
        if (store == null) {
            return Collections.EMPTY_MAP;
        } if (store instanceof Object[]) {
            final Map<Object, Object> map = new HashMap<>();

            for (int n = 0; n < storeSize; n++) {
                map.put(((Object[]) store)[n * 2], ((Object[]) store)[n * 2 + 1]);
            }

            return map;
        } else if (store instanceof LinkedHashMap) {
            return (LinkedHashMap<Object, Object>) store;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Map.Entry<Object, Object> entry : slowToMap().entrySet()) {
            getContext().getCoreLibrary().box(entry.getKey()).visitObjectGraph(visitor);
            getContext().getCoreLibrary().box(entry.getValue()).visitObjectGraph(visitor);
        }
    }

}
