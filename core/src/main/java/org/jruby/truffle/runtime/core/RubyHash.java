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
            return new RubyHash(this, null, null);
        }

    }

    private RubyProc defaultBlock;
    private Object store;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object store) {
        super(rubyClass);

        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length > 0;
        assert !(store instanceof Object[]) || ((Object[]) store).length <= RubyContext.HASHES_SMALL * 2;

        this.defaultBlock = defaultBlock;
        this.store = store;
    }

    public RubyProc getDefaultBlock() {
        return defaultBlock;
    }

    public Object getStore() {
        return store;
    }

    public void setDefaultBlock(RubyProc defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public void setStore(Object store) {
        assert store == null || store instanceof Object[] || store instanceof LinkedHashMap<?, ?>;
        assert !(store instanceof Object[]) || ((Object[]) store).length > 0;
        assert !(store instanceof Object[]) || ((Object[]) store).length <= RubyContext.HASHES_SMALL * 2;

        this.store = store;
    }

}
