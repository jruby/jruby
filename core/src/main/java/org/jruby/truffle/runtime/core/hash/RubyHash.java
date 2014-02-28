/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core.hash;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import org.jruby.truffle.runtime.core.*;

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

    @Override
    public Object dup() {
        final RubyHash newHash = new RubyHash(rubyClass);
        newHash.setInstanceVariables(getFields());
        newHash.storage.putAll(storage);
        return newHash;
    }

    public void put(Object key, Object value) {
        checkFrozen();

        if (key instanceof RubyString) {
            key = new RubyString(getRubyClass().getContext().getCoreLibrary().getStringClass(), key.toString());
            ((RubyString) key).frozen = true;
        }

        storage.put(key, value);
    }

    public Object get(Object key) {
        return storage.get(key);
    }

    public Map<Object, Object> getMap() {
        return storage;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{");

        for (Map.Entry<Object, Object> entry : storage.entrySet()) {
            if (builder.length() > 1) {
                builder.append(", ");
            }

            builder.append(entry.getKey().toString());
            builder.append("=>");
            builder.append(entry.getValue().toString());
        }

        builder.append("}");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((storage == null) ? 0 : storage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RubyHash)) {
            return false;
        }
        RubyHash other = (RubyHash) obj;
        if (storage == null) {
            if (other.storage != null) {
                return false;
            }
        } else if (!storage.equals(other.storage)) {
            return false;
        }
        return true;
    }

}
