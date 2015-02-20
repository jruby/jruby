/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.hash.FindEntryNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.HashSearchResult;
import org.jruby.truffle.runtime.hash.KeyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CoreClass(name = "Hash")
public abstract class HashNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode equalNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = {"isNull(a)", "isNull(b)"})
        public boolean equalNull(RubyHash a, RubyHash b) {
            return true;
        }

        @Specialization
        public boolean equal(RubyHash a, RubyHash b) {
            notDesignedForCompilation();

            final List<KeyValue> aEntries = HashOperations.verySlowToKeyValues(a);
            final List<KeyValue> bEntries = HashOperations.verySlowToKeyValues(a);

            if (aEntries.size() != bEntries.size()) {
                return false;
            }

            // For each entry in a, check that there is a corresponding entry in b, and don't use entries in b more than once

            final boolean[] bUsed = new boolean[bEntries.size()];

            for (KeyValue aKeyValue : aEntries) {
                boolean found = false;

                for (int n = 0; n < bEntries.size(); n++) {
                    if (!bUsed[n]) {
                        // TODO: cast

                        if ((boolean) DebugOperations.send(getContext(), aKeyValue.getKey(), "eql?", null, bEntries.get(n).getKey())) {
                            bUsed[n] = true;
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "!isRubyHash(b)")
        public boolean equalNonHash(RubyHash a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "[]", onSingleton = true, argumentsAsArray = true)
    public abstract static class ConstructNode extends HashCoreMethodNode {

        private final BranchProfile singleObject = BranchProfile.create();
        private final BranchProfile singleArray = BranchProfile.create();
        private final BranchProfile objectArray = BranchProfile.create();
        private final BranchProfile smallPackedArray = BranchProfile.create();
        private final BranchProfile largePackedArray = BranchProfile.create();
        private final BranchProfile otherArray = BranchProfile.create();
        private final BranchProfile singleOther = BranchProfile.create();
        private final BranchProfile keyValues = BranchProfile.create();

        public ConstructNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstructNode(ConstructNode prev) {
            super(prev);
        }

        @ExplodeLoop
        @Specialization
        public RubyHash construct(Object[] args) {
            if (args.length == 1) {
                singleObject.enter();

                final Object arg = args[0];

                if (arg instanceof RubyArray) {
                    singleArray.enter();

                    final RubyArray array = (RubyArray) arg;

                    if (array.getStore() instanceof Object[]) {
                        objectArray.enter();

                        final Object[] store = (Object[]) array.getStore();

                        // TODO(CS): zero length arrays might be a good specialisation

                        if (store.length <= HashOperations.SMALL_HASH_SIZE) {
                            smallPackedArray.enter();

                            final int size = array.getSize();
                            final Object[] newStore = new Object[HashOperations.SMALL_HASH_SIZE * 2];

                            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                                if (n < size) {
                                    final Object pair = store[n];

                                    if (!(pair instanceof RubyArray)) {
                                        CompilerDirectives.transferToInterpreter();
                                        throw new UnsupportedOperationException();
                                    }

                                    final RubyArray pairArray = (RubyArray) pair;

                                    if (!(pairArray.getStore() instanceof Object[])) {
                                        CompilerDirectives.transferToInterpreter();
                                        throw new UnsupportedOperationException();
                                    }

                                    final Object[] pairStore = (Object[]) pairArray.getStore();

                                    newStore[n * 2] = pairStore[0];
                                    newStore[n * 2 + 1] = pairStore[1];
                                }
                            }

                            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, newStore, size, null);
                        } else {
                            largePackedArray.enter();

                            final List<KeyValue> keyValues = new ArrayList<>();

                            final int size = array.getSize();

                            for (int n = 0; n < size; n++) {
                                final Object pair = store[n];

                                if (!(pair instanceof RubyArray)) {
                                    CompilerDirectives.transferToInterpreter();
                                    throw new UnsupportedOperationException();
                                }

                                final RubyArray pairArray = (RubyArray) pair;

                                if (!(pairArray.getStore() instanceof Object[])) {
                                    CompilerDirectives.transferToInterpreter();
                                    throw new UnsupportedOperationException();
                                }

                                final Object[] pairStore = (Object[]) pairArray.getStore();
                                keyValues.add(new KeyValue(pairStore[0], pairStore[1]));
                            }

                            return HashOperations.verySlowFromEntries(getContext(), keyValues);
                        }
                    } else {
                        otherArray.enter();
                        throw new UnsupportedOperationException("other array");
                    }
                } else {
                    singleOther.enter();
                    throw new UnsupportedOperationException("single other");
                }
            } else {
                keyValues.enter();

                final List<KeyValue> entries = new ArrayList<>();

                for (int n = 0; n < args.length; n += 2) {
                    entries.add(new KeyValue(args[n], args[n + 1]));
                }

                return HashOperations.verySlowFromEntries(getContext(), entries);
            }
        }

    }

    @CoreMethod(names = "[]", required = 1)
    public abstract static class GetIndexNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;
        @Child private YieldDispatchHeadNode yield;
        @Child private FindEntryNode findEntryNode;

        private final BranchProfile notInHashProfile = BranchProfile.create();
        private final BranchProfile useDefaultProfile = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            yield = new YieldDispatchHeadNode(context);
            findEntryNode = new FindEntryNode(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
            yield = prev.yield;
            findEntryNode = prev.findEntryNode;
        }

        @Specialization(guards = "isNull(hash)")
        public Object getNull(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            if (hash.getDefaultBlock() != null) {
                return yield.dispatch(frame, hash.getDefaultBlock(), hash, key);
            } else if (hash.getDefaultValue() != null) {
                return hash.getDefaultValue();
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public Object getPackedArray(VirtualFrame frame, RubyHash hash, Object key) {
            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size && eqlNode.callBoolean(frame, store[n * 2], "eql?", null, key)) {
                    return store[n * 2 + 1];
                }
            }

            notInHashProfile.enter();

            if (hash.getDefaultBlock() != null) {
                useDefaultProfile.enter();
                return yield.dispatch(frame, hash.getDefaultBlock(), hash, key);
            }

            if (hash.getDefaultValue() != null) {
                return hash.getDefaultValue();
            }

            return getContext().getCoreLibrary().getNilObject();

        }

        @Specialization(guards = "isBuckets(hash)")
        public Object getBuckets(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            final HashSearchResult hashSearchResult = findEntryNode.search(frame, hash, key);

            if (hashSearchResult.getEntry() != null) {
                return hashSearchResult.getEntry().getValue();
            }

            notInHashProfile.enter();

            if (hash.getDefaultBlock() != null) {
                useDefaultProfile.enter();
                return yield.dispatch(frame, hash.getDefaultBlock(), hash, key);
            }

            if (hash.getDefaultValue() != null) {
                return hash.getDefaultValue();
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "[]=", required = 2)
    public abstract static class SetIndexNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;

        private final BranchProfile considerExtendProfile = BranchProfile.create();
        private final BranchProfile extendProfile = BranchProfile.create();

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public SetIndexNode(SetIndexNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = "isNull(hash)")
        public Object setNull(RubyHash hash, Object key, Object value) {
            hash.checkFrozen(this);
            final Object[] store = new Object[HashOperations.SMALL_HASH_SIZE * 2];
            store[0] = key;
            store[1] = value;
            hash.setStore(store, 1, null, null);
            return value;
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public Object setPackedArray(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            hash.checkFrozen(this);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size && eqlNode.callBoolean(frame, store[n * 2], "eql?", null, key)) {
                    store[n * 2 + 1] = value;
                    return value;
                }
            }

            considerExtendProfile.enter();

            final int newSize = size + 1;

            if (newSize <= HashOperations.SMALL_HASH_SIZE) {
                extendProfile.enter();
                store[size * 2] = key;
                store[size * 2 + 1] = value;
                hash.setSize(newSize);
                return value;
            }

            CompilerDirectives.transferToInterpreter();

            // TODO(CS): need to watch for that transfer until we make the following fast path

            final List<KeyValue> entries = HashOperations.verySlowToKeyValues(hash);

            hash.setStore(new Entry[HashOperations.capacityGreaterThan(newSize)], newSize, null, null);

            for (KeyValue keyValue : entries) {
                HashOperations.verySlowSetInBuckets(hash, keyValue.getKey(), keyValue.getValue());
            }

            HashOperations.verySlowSetInBuckets(hash, key, value);

            return value;
        }

        @Specialization(guards = "isBuckets(hash)")
        public Object setBuckets(RubyHash hash, Object key, Object value) {
            notDesignedForCompilation();

            if (HashOperations.verySlowSetInBuckets(hash, key, value)) {
                hash.setSize(hash.getSize() + 1);
            }

            return value;
        }

    }

    @CoreMethod(names = "clear")
    public abstract static class ClearNode extends HashCoreMethodNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClearNode(ClearNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyHash emptyNull(RubyHash hash) {
            return hash;
        }

        @Specialization(guards = "!isNull(hash)")
        public RubyHash empty(RubyHash hash) {
            hash.setStore(null, 0, null, null);
            return hash;
        }

    }

    @CoreMethod(names = "default_proc")
    public abstract static class DefaultProcNode extends HashCoreMethodNode {

        public DefaultProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultProcNode(DefaultProcNode prev) {
            super(prev);
        }

        @Specialization
        public Object defaultProc(RubyHash hash) {
            if (hash.getDefaultBlock() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return hash.getDefaultBlock();
            }
        }

    }

    @CoreMethod(names = "delete", required = 1)
    public abstract static class DeleteNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;
        @Child private FindEntryNode findEntryNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            findEntryNode = new FindEntryNode(context, sourceSection);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
            findEntryNode = prev.findEntryNode;
        }

        @Specialization(guards = "isNull(hash)")
        public RubyNilClass deleteNull(RubyHash hash, Object key) {
            hash.checkFrozen(this);
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public Object deletePackedArray(VirtualFrame frame, RubyHash hash, Object key) {
            hash.checkFrozen(this);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size && eqlNode.callBoolean(frame, store[n * 2], "eql?", null, key)) {
                    final Object value = store[n * 2 + 1];

                    // Move the later values down
                    int k = n * 2; // position of the key
                    System.arraycopy(store, k + 2, store, k, (size - n - 1) * 2);

                    hash.setSize(size - 1);

                    return value;
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isBuckets(hash)")
        public Object delete(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            final HashSearchResult hashSearchResult = findEntryNode.search(frame, hash, key);

            if (hashSearchResult.getEntry() == null) {
                return getContext().getCoreLibrary().getNilObject();
            }

            final Entry entry = hashSearchResult.getEntry();

            // Remove from the sequence chain

            if (entry.getPreviousInSequence() == null) {
                hash.setFirstInSequence(entry.getNextInSequence());
            } else {
                entry.getPreviousInSequence().setNextInSequence(entry.getNextInSequence());
            }

            if (entry.getNextInSequence() == null) {
                hash.setLastInSequence(entry.getPreviousInSequence());
            } else {
                entry.getNextInSequence().setPreviousInSequence(entry.getPreviousInSequence());
            }

            // Remove from the lookup chain

            if (hashSearchResult.getPreviousEntry() == null) {
                ((Entry[]) hash.getStore())[hashSearchResult.getIndex()] = entry.getNextInLookup();
            } else {
                hashSearchResult.getPreviousEntry().setNextInLookup(entry.getNextInLookup());
            }

            hash.setSize(hash.getSize() - 1);

            return entry.getValue();
        }

    }

    @CoreMethod(names = { "each", "each_pair" }, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyHash eachNull(RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            return hash;
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyHash eachPackedArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            int count = 0;

            try {
                for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    if (n < size) {
                        yield(frame, block, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), store[n * 2], store[n * 2 + 1]));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return hash;
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyHash eachBuckets(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                yield(frame, block, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), keyValue.getKey(), keyValue.getValue()));
            }

            return hash;
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends HashCoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public boolean emptyNull(RubyHash hash) {
            return true;
        }

        @Specialization(guards = "!isNull(hash)")
        public boolean emptyPackedArray(RubyHash hash) {
            return hash.getSize() == 0;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 1)
    public abstract static class InitializeNode extends HashCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize(RubyHash hash, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            hash.setStore(null, 0, null, null);
            hash.setDefaultBlock(null);
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public RubyNilClass initialize(RubyHash hash, UndefinedPlaceholder defaultValue, RubyProc block) {
            notDesignedForCompilation();
            hash.setStore(null, 0, null, null);
            hash.setDefaultBlock(block);
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public RubyNilClass initialize(RubyHash hash, Object defaultValue, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            hash.setDefaultValue(defaultValue);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends HashCoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(from)")
        public RubyHash dupNull(RubyHash self, RubyHash from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            self.setDefaultBlock(from.getDefaultBlock());
            self.setDefaultValue(from.getDefaultValue());
            self.setStore(null, 0, null, null);

            return self;
        }

        @Specialization(guards = {"!isNull(from)", "!isBuckets(from)"})
        public RubyHash dupPackedArray(RubyHash self, RubyHash from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            final Object[] store = (Object[]) from.getStore();
            self.setStore(Arrays.copyOf(store, HashOperations.SMALL_HASH_SIZE * 2), store.length, null, null);
            self.setDefaultBlock(from.getDefaultBlock());
            self.setDefaultValue(from.getDefaultValue());

            return self;
        }

        @Specialization(guards = "isBuckets(from)")
        public RubyHash dupBuckets(RubyHash self, RubyHash from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            HashOperations.verySlowSetKeyValues(self, HashOperations.verySlowToKeyValues(from));

            return self;
        }

    }

    @CoreMethod(names = {"inspect", "to_s"})
    public abstract static class InspectNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode inspect;

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            inspect = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
            inspect = prev.inspect;
        }

        @Specialization(guards = "isNull(hash)")
        public RubyString inspectNull(RubyHash hash) {
            notDesignedForCompilation();

            return getContext().makeString("{}");
        }

        @Specialization
        public RubyString inspectPackedArray(VirtualFrame frame, RubyHash hash) {
            notDesignedForCompilation();

            final StringBuilder builder = new StringBuilder();

            builder.append("{");

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                if (builder.length() > 1) {
                    builder.append(", ");
                }

                // TODO(CS): to string

                builder.append(inspect.call(frame, keyValue.getKey(), "inspect", null));
                builder.append("=>");
                builder.append(inspect.call(frame, keyValue.getValue(), "inspect", null));
            }

            builder.append("}");

            return getContext().makeString(builder.toString());
        }

    }

    @CoreMethod(names = { "has_key?", "key?", "include?", "member?" }, required = 1)
    public abstract static class KeyNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;

        public KeyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public KeyNode(KeyNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = "isNull(hash)")
        public boolean keyNull(RubyHash hash, Object key) {
            return false;
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public boolean keyPackedArray(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            final int size = hash.getSize();
            final Object[] store = (Object[]) hash.getStore();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size && eqlNode.callBoolean(frame, store[n * 2], "eql?", null, key)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isBuckets(hash)")
        public boolean keyBuckets(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                if (eqlNode.callBoolean(frame, keyValue.getKey(), "eql?", null, key)) {
                    return true;
                }
            }

            return false;
        }

    }

    @CoreMethod(names = "keys")
    public abstract static class KeysNode extends HashCoreMethodNode {

        public KeysNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KeysNode(KeysNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyArray keysNull(RubyHash hash) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyArray keysPackedArray(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            final Object[] keys = new Object[hash.getSize()];

            for (int n = 0; n < keys.length; n++) {
                keys[n] = store[n * 2];
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), keys, keys.length);
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyArray keysBuckets(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] keys = new Object[hash.getSize()];

            Entry entry = hash.getFirstInSequence();
            int n = 0;

            while (entry != null) {
                keys[n] = entry.getKey();
                n++;
                entry = entry.getNextInSequence();
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), keys, keys.length);
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapNode(MapNode prev) {
            super(prev);
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyArray mapPackedArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            final Object[] result = new Object[size];

            int count = 0;

            try {
                for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                    if (n < size) {
                        final Object key = store[n * 2];
                        final Object value = store[n * 2 + 1];
                        result[n] = yield(frame, block, key, value);

                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), result, size);
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyArray mapBuckets(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                array.slowPush(yield(frame, block, keyValue.getKey(), keyValue.getValue()));
            }

            return array;
        }

    }

    @CoreMethod(names = "merge", required = 1)
    public abstract static class MergeNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;

        private final BranchProfile nothingFromFirstProfile = BranchProfile.create();
        private final BranchProfile considerNothingFromSecondProfile = BranchProfile.create();
        private final BranchProfile nothingFromSecondProfile = BranchProfile.create();
        private final BranchProfile considerResultIsSmallProfile = BranchProfile.create();
        private final BranchProfile resultIsSmallProfile = BranchProfile.create();

        private final int smallHashSize = HashOperations.SMALL_HASH_SIZE;

        public MergeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public MergeNode(MergeNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "isNull(other)"})
        public RubyHash mergePackedArrayNull(RubyHash hash, RubyHash other) {
            final Object[] store = (Object[]) hash.getStore();
            final Object[] copy = Arrays.copyOf(store, HashOperations.SMALL_HASH_SIZE * 2);

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), hash.getDefaultBlock(), hash.getDefaultValue(), copy, hash.getSize(), null);
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "!isNull(other)", "!isBuckets(other)"})
        public RubyHash mergePackedArrayPackedArray(VirtualFrame frame, RubyHash hash, RubyHash other) {
            // TODO(CS): what happens with the default block here? Which side does it get merged from?

            final Object[] storeA = (Object[]) hash.getStore();
            final int storeASize = hash.getSize();

            final Object[] storeB = (Object[]) other.getStore();
            final int storeBSize = hash.getSize();

            final boolean[] mergeFromA = new boolean[storeASize];
            int mergeFromACount = 0;

            for (int a = 0; a < HashOperations.SMALL_HASH_SIZE; a++) {
                if (a < storeASize) {
                    boolean merge = true;

                    for (int b = 0; b < HashOperations.SMALL_HASH_SIZE; b++) {
                        if (b < storeBSize) {
                            if (eqlNode.callBoolean(frame, storeA[a * 2], "eql?", null, storeB[b * 2])) {
                                merge = false;
                                break;
                            }
                        }
                    }

                    if (merge) {
                        mergeFromACount++;
                    }

                    mergeFromA[a] = merge;
                }
            }

            if (mergeFromACount == 0) {
                nothingFromFirstProfile.enter();
                return new RubyHash(getContext().getCoreLibrary().getHashClass(), hash.getDefaultBlock(), hash.getDefaultValue(), Arrays.copyOf(storeB, HashOperations.SMALL_HASH_SIZE * 2), storeBSize, null);
            }

            considerNothingFromSecondProfile.enter();

            if (mergeFromACount == storeB.length) {
                nothingFromSecondProfile.enter();
                return new RubyHash(getContext().getCoreLibrary().getHashClass(), hash.getDefaultBlock(), hash.getDefaultValue(), Arrays.copyOf(storeB, HashOperations.SMALL_HASH_SIZE * 2), storeBSize, null);
            }

            considerResultIsSmallProfile.enter();

            final int mergedSize = storeBSize + mergeFromACount;

            if (storeBSize + mergeFromACount <= smallHashSize) {
                resultIsSmallProfile.enter();

                final Object[] merged = new Object[HashOperations.SMALL_HASH_SIZE * 2];

                int index = 0;

                for (int n = 0; n < storeASize; n++) {
                    if (mergeFromA[n]) {
                        merged[index] = storeA[n * 2];
                        merged[index + 1] = storeA[n * 2 + 1];
                        index += 2;
                    }
                }

                for (int n = 0; n < storeBSize; n++) {
                    merged[index] = storeB[n * 2];
                    merged[index + 1] = storeB[n * 2 + 1];
                    index += 2;
                }

                return new RubyHash(getContext().getCoreLibrary().getHashClass(), hash.getDefaultBlock(), hash.getDefaultValue(), merged, mergedSize, null);
            }

            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }


        @Specialization
        public RubyHash mergeBucketsBuckets(RubyHash hash, RubyHash other) {
            final RubyHash merged = new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, new Entry[HashOperations.capacityGreaterThan(hash.getSize() + other.getSize())], 0, null);

            int size = 0;

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue());
                size++;
            }

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(other)) {
                if (HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue())) {
                    size++;
                }
            }

            merged.setSize(size);

            return merged;
        }

    }

    @CoreMethod(names = "default", optional = 1)
    public abstract static class DefaultNode extends HashCoreMethodNode {

        public DefaultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultNode(DefaultNode prev) {
            super(prev);
        }

        @Specialization
        public Object defaultElement(VirtualFrame frame, RubyHash hash, UndefinedPlaceholder undefined) {
            Object ret = hash.getDefaultValue();

            // TODO (nirvdrum Dec. 1, 2014): This needs to evaluate the defaultProc if it exists before it tries defaultValue.
            if (ret != null) {
                return ret;
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }

        @Specialization(guards = "!isUndefinedPlaceholder(key)")
        public Object defaultElement(VirtualFrame frame, RubyHash hash, Object key) {
            Object ret = hash.getDefaultValue();

            // TODO (nirvdrum Dec. 1, 2014): This really needs to do something with the key.  Dummy stub for now.
            if (ret != null) {
                return ret;
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }
    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends HashCoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public int sizeNull(RubyHash hash) {
            return 0;
        }

        @Specialization(guards = "!isNull(hash)")
        public int sizePackedArray(RubyHash hash) {
            return hash.getSize();
        }

    }

    @CoreMethod(names = "values")
    public abstract static class ValuesNode extends HashCoreMethodNode {

        public ValuesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ValuesNode(ValuesNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyArray valuesNull(RubyHash hash) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyArray valuesPackedArray(RubyHash hash) {
            final Object[] store = (Object[]) hash.getStore();

            final Object[] values = new Object[hash.getSize()];

            for (int n = 0; n < values.length; n++) {
                values[n] = store[n * 2 + 1];
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), values, values.length);
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyArray valuesBuckets(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] values = new Object[hash.getSize()];

            Entry entry = hash.getFirstInSequence();
            int n = 0;

            while (entry != null) {
                values[n] = entry.getValue();
                n++;
                entry = entry.getNextInSequence();
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), values, values.length);
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToArrayNode extends HashCoreMethodNode {

        public ToArrayNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToArrayNode(ToArrayNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyArray toArrayNull(RubyHash hash) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyArray toArrayPackedArray(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();
            final Object[] pairs = new Object[size];

            for (int n = 0; n < size; n++) {
                pairs[n] = RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), store[n * 2], store[n * 2 + 1]);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), pairs, size);
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyArray toArrayBuckets(RubyHash hash) {
            notDesignedForCompilation();

            final int size = hash.getSize();
            final Object[] pairs = new Object[size];

            int n = 0;

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                pairs[n] = RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), keyValue.getValue(), keyValue.getValue());
                n++;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), pairs, size);
        }

    }

}
