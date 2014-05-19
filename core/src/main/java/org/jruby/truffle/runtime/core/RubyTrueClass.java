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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;

/**
 * Represents the Ruby {@code TrueClass} class.
 */
public class RubyTrueClass extends RubyObject implements Unboxable {

    public RubyTrueClass(RubyClass objectClass) {
        super(objectClass);
    }

    /**
     * Convert a value to a boolean, without doing any lookup.
     */
    @Deprecated
    public static boolean toBoolean(Object value) {
        RubyNode.notDesignedForCompilation();

        assert value != null;

        if (value instanceof NilPlaceholder) {
            return false;
        }

        if (value instanceof Boolean) {
            return (boolean) value;
        }

        if (value instanceof RubyTrueClass) {
            return true;
        }

        if (value instanceof RubyFalseClass) {
            return false;
        }

        return true;
    }

    public Object unbox() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RubyTrueClass || (other instanceof Boolean && (boolean) other);
    }

    @Override
    public int hashCode() {
        return Boolean.TRUE.hashCode();
    }

}
