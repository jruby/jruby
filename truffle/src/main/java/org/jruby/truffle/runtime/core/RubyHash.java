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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyHash extends RubyBasicObject {

    public RubyProc defaultBlock;
    public Object defaultValue;
    public Object store;
    public int size;
    public Entry firstInSequence;
    public Entry lastInSequence;
    public boolean compareByIdentity;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int size, Entry firstInSequence) {
        super(rubyClass);
        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.size = size;
        this.firstInSequence = firstInSequence;
        assert HashOperations.verifyStore(this);
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new HashForeignAccessFactory(getContext());
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (KeyValue keyValue : HashOperations.verySlowToKeyValues(this)) {
            if (keyValue.getKey() instanceof RubyBasicObject) {
                ((RubyBasicObject) keyValue.getKey()).visitObjectGraph(visitor);
            }

            if (keyValue.getValue() instanceof RubyBasicObject) {
                ((RubyBasicObject) keyValue.getValue()).visitObjectGraph(visitor);
            }
        }
    }

}
