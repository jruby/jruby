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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

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
            return new RubyHash(this);
        }

    }

    @CompilationFinal public Map<Object, Object> storage;
    @CompilationFinal public RubyProc defaultBlock;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock) {
        super(rubyClass);
        initialize(defaultBlock);
    }

    public RubyHash(RubyClass rubyClass) {
        super(rubyClass);
        initialize(null);
    }

    @CompilerDirectives.SlowPath
    public void initialize(RubyProc setDefaultBlock) {
        storage = new LinkedHashMap<>();
        defaultBlock = setDefaultBlock;
    }

    @CompilerDirectives.SlowPath
    public void put(Object key, Object value) {
        checkFrozen();

        if (key instanceof RubyString) {
            key = RubyString.fromJavaString(getRubyClass().getContext().getCoreLibrary().getStringClass(), key.toString());
            ((RubyString) key).frozen = true;
        }

        storage.put(key, value);
    }

    @CompilerDirectives.SlowPath
    public Object get(Object key) {
        return storage.get(key);
    }

    public Map<Object, Object> getMap() {
        return storage;
    }

}
