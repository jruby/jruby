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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.interop.ForeignAccessArguments;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.interop.InteropPredicate;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyHash extends RubyBasicObject {

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int storeSize;
    private Entry firstInSequence;
    private Entry lastInSequence;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int storeSize, Entry firstInSequence) {
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

    public void setStore(Object store, int storeSize, Entry firstInSequence, Entry lastInSequence) {
        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
    }

    public int getSize() {
        return storeSize;
    }

    public void setSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public Entry getFirstInSequence() {
        return firstInSequence;
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
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyHash(rubyClass, null, null, null, 0, null);
        }

    }

}
