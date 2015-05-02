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
import org.jruby.truffle.runtime.hash.PackedArrayStrategy;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyHash extends RubyBasicObject {

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int size;
    private Entry firstInSequence;
    private Entry lastInSequence;
    private boolean compareByIdentity;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int size, Entry firstInSequence) {
        super(rubyClass);
        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.size = size;
        this.firstInSequence = firstInSequence;
        assert HashOperations.verifyStore(this);
    }

    public RubyProc getDefaultBlock() {
        return defaultBlock;
    }

    public void setDefaultBlock(RubyProc defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public boolean isCompareByIdentity() {
        return compareByIdentity;
    }
    
    public void setCompareByIdentity(boolean compareByIdentity) {
        this.compareByIdentity = compareByIdentity;
    }

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        assert HashOperations.verifyStore(store, size, firstInSequence, lastInSequence);
        this.store = store;
        this.size = size;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int storeSize) {
        this.size = storeSize;
    }

    public Entry getFirstInSequence() {
        return firstInSequence;
    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new HashForeignAccessFactory(getContext());
    }

    public void setFirstInSequence(Entry firstInSequence) {
        this.firstInSequence = firstInSequence;
    }

    public Entry getLastInSequence() {
        return lastInSequence;
    }

    public void setLastInSequence(Entry lastInSequence) {
        this.lastInSequence = lastInSequence;
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

    public static class HashAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyHash(rubyClass, null, null, null, 0, null);
        }

    }

}
