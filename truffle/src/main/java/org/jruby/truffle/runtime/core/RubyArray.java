/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.interop.ForeignAccessFactory;

import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Implements the Ruby {@code Array} class.
 */
public final class RubyArray extends RubyBasicObject {

    public Object store;
    public int size;

    public RubyArray(RubyClass arrayClass) {
        this(arrayClass, null, 0);
    }

    public RubyArray(RubyClass arrayClass, Object store, int size) {
        super(arrayClass);
        ArrayNodes.setStore(this, store, size);
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Object object : ArrayNodes.slowToArray(this)) {
            if (object instanceof RubyBasicObject) {
                ((RubyBasicObject) object).visitObjectGraph(visitor);
            }
        }
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new ArrayForeignAccessFactory(getContext());
    }

}
