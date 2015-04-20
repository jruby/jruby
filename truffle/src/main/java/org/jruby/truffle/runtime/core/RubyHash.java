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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

public class RubyHash extends RubyBasicObject {

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int storeSize;
    private Entry firstInSequence;
    private Entry lastInSequence;
    private boolean compareByIdentity;

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
    
    public boolean isCompareByIdentity() {
        return compareByIdentity;
    }
    
    public void setCompareByIdentity(boolean compareByIdentity) {
        this.compareByIdentity = compareByIdentity;
    }

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int storeSize, Entry firstInSequence, Entry lastInSequence) {
        assert verifyStore(store, storeSize, firstInSequence, lastInSequence);
        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
    }

    public boolean verifyStore(Object store, int storeSize, Entry firstInSequence, Entry lastInSequence) {
        assert store == null || store instanceof Object[] || store instanceof Entry[];

        if (store == null) {
            assert storeSize == 0;
            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        if (store instanceof Entry[]) {
            final Entry[] entryStore = (Entry[]) store;

            int foundSize = 0;
            boolean foundFirst = false;
            boolean foundLast = true;

            for (int n = 0; n < entryStore.length; n++) {
                Entry entry = entryStore[n];

                while (entry != null) {
                    foundSize++;

                    if (entry == firstInSequence) {
                        foundFirst = true;
                    }

                    if (entry == lastInSequence) {
                        foundLast = true;
                    }

                    entry = entry.getNextInLookup();
                }
            }

            //assert foundSize == storeSize; Can't do this because sometimes we set the store and then fill it up
            assert firstInSequence == null || foundFirst;
            assert lastInSequence == null || foundLast;
        } else if (store instanceof Object[]) {
            assert ((Object[]) store).length == HashOperations.SMALL_HASH_SIZE * 2 : ((Object[]) store).length;
            
            final Object[] packedStore = (Object[]) store;
            
            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < storeSize) {
                    assert packedStore[n * 2] != null;
                    assert packedStore[n * 2 + 1] != null;
                }
            }
            
            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        return true;
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
