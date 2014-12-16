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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.hash.Bucket;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyHash extends RubyBasicObject {

    public static class RubyHashClass extends RubyClass {

        public RubyHashClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Hash");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyHash(this, null, null, null, 0, null);
        }

    }

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int storeSize;
    private Bucket firstInSequence;
    private Bucket lastInSequence;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int storeSize, Bucket firstInSequence) {
        super(rubyClass);
        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
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

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int storeSize, Bucket firstInSequence, Bucket lastInSequence) {
        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public Bucket getFirstInSequence() {
        return firstInSequence;
    }

    public void setFirstInSequence(Bucket firstInSequence) {
        this.firstInSequence = firstInSequence;
    }

    public Bucket getLastInSequence() {
        return lastInSequence;
    }

    public void setLastInSequence(Bucket lastInSequence) {
        this.lastInSequence = lastInSequence;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Entry entry : HashOperations.verySlowToEntries(this)) {
            if (entry.getKey() instanceof RubyBasicObject) {
                ((RubyBasicObject) entry.getKey()).visitObjectGraph(visitor);
            }

            if (entry.getValue() instanceof RubyBasicObject) {
                ((RubyBasicObject) entry.getValue()).visitObjectGraph(visitor);
            }
        }
    }

}
