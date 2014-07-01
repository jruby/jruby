/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.Ruby;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.util.cli.Options;

@CoreClass(name = "Hash")
public abstract class HashNodes {

    @CoreMethod(names = "==", minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends HashCoreMethodNode {

        @Child protected DispatchHeadNode equalNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = new DispatchHeadNode(context, "==", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = {"isNull", "isOtherNull"}, order = 1)
        public boolean equalNull(RubyHash a, RubyHash b) {
            return true;
        }

        @Specialization(guards = {"isObjectArray", "isOtherObjectArray"}, order = 2)
        public boolean equalObjectArray(VirtualFrame frame, RubyHash a, RubyHash b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            final Object[] aStore = (Object[]) a.getStore();
            final Object[] bStore = (Object[]) b.getStore();

            if (aStore.length != bStore.length) {
                return false;
            }

            for (int n = 0; n < aStore.length; n++) {
                // TODO(CS): cast
                if (!(boolean) equalNode.dispatch(frame, aStore[n], null, bStore[n])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = {"isObjectLinkedHashMap", "isOtherObjectLinkedHashMap"}, order = 3)
        public boolean equalObjectLinkedHashMap(RubyHash a, RubyHash b) {
            notDesignedForCompilation();
            throw new UnsupportedOperationException();
        }

        @Specialization(order = 4)
        public boolean equal(RubyHash a, RubySymbol b) {
            notDesignedForCompilation();
            return false;
        }

    }

    @CoreMethod(names = "[]", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class ConstructNode extends HashCoreMethodNode {

        private final BranchProfile singleObject = new BranchProfile();
        private final BranchProfile singleArray = new BranchProfile();
        private final BranchProfile objectArray = new BranchProfile();
        private final BranchProfile smallObjectArray = new BranchProfile();
        private final BranchProfile largeObjectArray = new BranchProfile();
        private final BranchProfile otherArray = new BranchProfile();
        private final BranchProfile singleOther = new BranchProfile();
        private final BranchProfile keyValues = new BranchProfile();

        public ConstructNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstructNode(ConstructNode prev) {
            super(prev);
        }

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

                        if (store.length <= Options.TRUFFLE_HASHES_SMALL.load()) {
                            smallObjectArray.enter();

                            final Object[] newStore = new Object[store.length * 2];

                            for (int n = 0; n < store.length; n++) {
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

                            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, newStore);
                        } else {
                            largeObjectArray.enter();
                            throw new UnsupportedOperationException();
                        }
                    } else {
                        otherArray.enter();
                        throw new UnsupportedOperationException();
                    }
                } else {
                    singleOther.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                keyValues.enter();
                // Slow because we don't want the PE to see the hash map at all
                return constructObjectLinkedMapMap(args);
            }
        }

        @CompilerDirectives.SlowPath
        public RubyHash constructObjectLinkedMapMap(Object[] args) {
            final LinkedHashMap<Object, Object> store = new LinkedHashMap<>();

            for (int n = 0; n < args.length; n += 2) {
                store.put(args[n], args[n + 1]);
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, store);
        }

    }

    @CoreMethod(names = "[]", minArgs = 1, maxArgs = 1)
    public abstract static class GetIndexNode extends HashCoreMethodNode {

        @Child protected DispatchHeadNode eqlNode;

        private final BranchProfile notInHashProfile = new BranchProfile();
        private final BranchProfile useDefaultProfile = new BranchProfile();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = new DispatchHeadNode(context, "eql?", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = "isNull", order = 1)
        public Object getNull(RubyHash hash, Object key) {
            notDesignedForCompilation();

            if (hash.getDefaultBlock() == null) {
                return NilPlaceholder.INSTANCE;
            } else {
                // TODO(CS): need a call node here
                return hash.getDefaultBlock().call(hash, key);
            }
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public Object getObjectArray(VirtualFrame frame, RubyHash hash, Object key) {
            final Object[] store = (Object[]) hash.getStore();

            for (int n = 0; n < store.length; n += 2) {
                // TODO(CS): cast
                if ((boolean) eqlNode.dispatch(frame, store[n], null, key)) {
                    return store[n + 1];
                }
            }

            notInHashProfile.enter();

            if (hash.getDefaultBlock() == null) {
                return NilPlaceholder.INSTANCE;
            }

            useDefaultProfile.enter();

            // TODO(CS): need a call node here
            return hash.getDefaultBlock().call(hash, key);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public Object getObjectLinkedHashMap(RubyHash hash, Object key) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            // TODO(CS): not correct - using Java's Object#equals

            final Object value = store.get(key);

            if (value == null) {
                if (hash.getDefaultBlock() == null) {
                    return NilPlaceholder.INSTANCE;
                } else {
                    // TODO(CS): need a call node here
                    return hash.getDefaultBlock().call(hash, key);
                }
            }

            return value;
        }

    }

    @CoreMethod(names = "[]=", minArgs = 2, maxArgs = 2)
    public abstract static class SetIndexNode extends HashCoreMethodNode {

        private final int smallHashArrayLength = Options.TRUFFLE_HASHES_SMALL.load() * 2;

        private final BranchProfile transitionToLinkedHashMap = new BranchProfile();

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetIndexNode(SetIndexNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public Object setNull(RubyHash hash, Object key, Object value) {
            hash.checkFrozen();
            hash.setStore(new Object[]{key, value});
            return value;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public Object setObjectArray(RubyHash hash, Object key, Object value) {
            hash.checkFrozen();

            final Object[] store = (Object[]) hash.getStore();
            final int length = store.length;

            if (length + 2 < smallHashArrayLength) {
                final Object[] newStore = Arrays.copyOf(store, length + 2);
                newStore[length] = key;
                newStore[length + 1] = value;
                hash.setStore(newStore);
            } else {
                transitionToLinkedHashMap.enter();

                transitionToLinkedHashMap(hash, store, key, value);
            }

            return value;
        }

        @CompilerDirectives.SlowPath
        private void transitionToLinkedHashMap(RubyHash hash, Object[] oldStore, Object key, Object value) {
            final LinkedHashMap<Object, Object> newStore = new LinkedHashMap<>();

            for (int n = 0; n < oldStore.length; n += 2) {
                newStore.put(oldStore[n], oldStore[n + 1]);
            }

            newStore.put(key, value);
            hash.setStore(newStore);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public Object setObjectLinkedHashMap(RubyHash hash, Object key, Object value) {
            notDesignedForCompilation();

            hash.checkFrozen();

            // TODO(CS): not correct - using Java's Object#equals

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();
            store.put(key, value);
            return value;
        }

    }

    @CoreMethod(names = "delete", minArgs = 1, maxArgs = 1)
    public abstract static class DeleteNode extends HashCoreMethodNode {

        @Child protected DispatchHeadNode eqlNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = new DispatchHeadNode(context, "eql?", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = "isNull", order = 1)
        public NilPlaceholder deleteNull(RubyHash hash, Object key) {
            hash.checkFrozen();
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public Object deleteObjectArray(RubyHash hash, Object key) {
            notDesignedForCompilation();

            // TODO(CS): seriously not correct

            hash.checkFrozen();

            final Object[] oldStore = (Object[]) hash.getStore();

            final LinkedHashMap<Object, Object> newStore = new LinkedHashMap<>();
            hash.setStore(newStore);

            for (int n = 0; n < oldStore.length; n += 2) {
                newStore.put(oldStore[n], oldStore[n + 1]);
            }

            // TODO(CS): seriously not correct - using Java's Object#equals

            final Object removed = newStore.remove(key);

            if (removed == null) {
                return NilPlaceholder.INSTANCE;
            } else {
                return removed;
            }
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public Object delete(RubyHash hash, Object key) {
            notDesignedForCompilation();

            hash.checkFrozen();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            // TODO(CS): seriously not correct - using Java's Object#equals

            final Object removed = store.remove(key);

            if (removed == null) {
                return NilPlaceholder.INSTANCE;
            } else {
                return removed;
            }
        }

    }

    @CoreMethod(names = "dup", maxArgs = 0)
    public abstract static class DupNode extends HashCoreMethodNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DupNode(DupNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyHash dupNull(RubyHash hash) {
            notDesignedForCompilation();

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null);
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public RubyHash dupObjectArray(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();
            final Object[] copy = Arrays.copyOf(store, store.length);

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, copy);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public RubyHash dupObjectLinkedHashMap(RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();
            final LinkedHashMap<Object, Object> copy = new LinkedHashMap<>(store);

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, copy);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, maxArgs = 0)
    public abstract static class EachNode extends YieldingHashCoreMethodNode {

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyHash eachNull(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            return hash;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public RubyHash eachObjectArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            int count = 0;

            try {
                for (int n = 0; n < store.length; n += 2) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), store[n], store[n + 1]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return hash;
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public RubyHash eachObjectLinkedHashMap(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            int count = 0;

            try {
                for (Map.Entry<Object, Object> entry : store.entrySet()) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), entry.getKey(), entry.getValue()));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return hash;
        }

    }

    @CoreMethod(names = "empty?", maxArgs = 0)
    public abstract static class EmptyNode extends HashCoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public boolean emptyNull(RubyHash hash) {
            return true;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public boolean emptyObjectArray(RubyHash hash) {
            // TODO(CS): is this invariant ok? Arrays for hashes should never be zero length?
            return false;
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public boolean emptyObjectLinkedHashMap(RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();
            return store.isEmpty();
        }

    }

    @CoreMethod(names = "to_a", maxArgs = 0)
    public abstract static class ToArrayNode extends HashCoreMethodNode {

        public ToArrayNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToArrayNode(ToArrayNode prev) {
            super(prev);
        }

        @Specialization(guards = "isObjectLinkedHashMap")
        public RubyArray toArray(RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            final Object[] array = new Object[store.size() * 2];

            int n = 0;

            for (Map.Entry<Object, Object> keyValues : store.entrySet()) {
                array[n] = keyValues.getKey();
                array[n + 1] = keyValues.getValue();
                n += 2;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array, array.length);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, maxArgs = 0)
    public abstract static class InitializeNode extends HashCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder initialize(RubyHash hash, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();
            hash.setStore(null);
            hash.setDefaultBlock(null);
            return NilPlaceholder.INSTANCE;
        }

        @Specialization
        public NilPlaceholder initialize(RubyHash hash, RubyProc block) {
            notDesignedForCompilation();
            hash.setStore(null);
            hash.setDefaultBlock(block);
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = {"inspect", "to_s"}, maxArgs = 0)
    public abstract static class InspectNode extends HashCoreMethodNode {

        @Child protected DispatchHeadNode inspect;

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            inspect = new DispatchHeadNode(context, "inspect", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
            inspect = prev.inspect;
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyString inspectNull(VirtualFrame frame, RubyHash hash) {
            notDesignedForCompilation();

            return getContext().makeString("{}");
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public RubyString inspectObjectArray(VirtualFrame frame, RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            final StringBuilder builder = new StringBuilder();

            builder.append("{");

            for (int n = 0; n < store.length; n += 2) {
                if (n > 0) {
                    builder.append(", ");
                }

                // TODO(CS): to string

                builder.append(inspect.dispatch(frame, store[n], null));
                builder.append("=>");
                builder.append(inspect.dispatch(frame, store[n + 1], null));
            }

            builder.append("}");

            return getContext().makeString(builder.toString());
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public RubyString inspectObjectLinkedHashMap(VirtualFrame frame, RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            final StringBuilder builder = new StringBuilder();

            builder.append("{");

            boolean first = true;

            for (Map.Entry<Object, Object> entry : store.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    builder.append(", ");
                }

                builder.append(inspect.dispatch(frame, entry.getKey(), null));
                builder.append("=>");
                builder.append(inspect.dispatch(frame, entry.getValue(), null));
            }

            builder.append("}");

            return getContext().makeString(builder.toString());
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true, maxArgs = 0)
    public abstract static class MapNode extends YieldingHashCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapNode(MapNode prev) {
            super(prev);
        }

        @Specialization(guards = "isObjectArray", order = 1)
        public RubyArray mapObjectArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            final Object[] store = (Object[]) hash.getStore();

            final Object[] result = new Object[store.length / 2];

            int count = 0;

            try {
                for (int n = 0; n < result.length; n++) {
                    final Object key = store[n * 2];
                    final Object value = store[n * 2 + 1];
                    result[n] = yield(frame, block, key, value);

                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), result, result.length);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 2)
        public RubyArray mapObjectLinkedHashMap(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            final RubyArray result = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            int count = 0;

            try {
                for (Map.Entry<Object, Object> entry : store.entrySet()) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    result.slowPush(yield(frame, block, entry.getKey(), entry.getValue()));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return result;
        }

    }

    @CoreMethod(names = "merge", minArgs = 1, maxArgs = 1)
    public abstract static class MergeNode extends HashCoreMethodNode {

        public MergeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MergeNode(MergeNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isObjectArray", "isOtherNull"})
        public RubyHash merge(RubyHash hash, RubyHash other) {
            final Object[] store = (Object[]) hash.getStore();
            final Object[] copy = Arrays.copyOf(store, store.length);

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), hash.getDefaultBlock(), copy);
        }

    }

    @CoreMethod(names = "key?", minArgs = 1, maxArgs = 1)
    public abstract static class KeyNode extends HashCoreMethodNode {

        @Child protected DispatchHeadNode eqlNode;

        public KeyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = new DispatchHeadNode(context, "eql?", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public KeyNode(KeyNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
        }

        @Specialization(guards = "isNull", order = 1)
        public boolean keyNull(RubyHash hash, Object key) {
            return false;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public boolean keyObjectArray(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            for (int n = 0; n < store.length; n += 2) {
                // TODO(CS): cast
                if ((boolean) eqlNode.dispatch(frame, store[n], null, key)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public boolean keyObjectLinkedHashMap(RubyHash hash, Object key) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            // TODO(CS): seriously not correct - using Java's Object#equals

            return store.containsKey(key);
        }

    }

    @CoreMethod(names = "keys", maxArgs = 0)
    public abstract static class KeysNode extends HashCoreMethodNode {

        public KeysNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KeysNode(KeysNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyArray keysNull(RubyHash hash) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public RubyArray keysObjectArray(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            final Object[] keys = new Object[store.length / 2];

            for (int n = 0; n < keys.length; n++) {
                keys[n] = store[n * 2];
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), keys, keys.length);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public RubyArray keysObjectLinkedHashMap(RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            final Object[] keys = new Object[store.size()];

            int n = 0;

            for (Object key : store.keySet()) {
                keys[n] = key;
                n++;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), keys, keys.length);
        }

    }

    @CoreMethod(names = "size", maxArgs = 0)
    public abstract static class SizeNode extends HashCoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public int sizeNull(RubyHash hash) {
            return 0;
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public int sizeObjectArray(RubyHash hash) {
            return ((Object[]) hash.getStore()).length / 2;
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public int sizeObjectLinkedHashMap(RubyHash hash) {
            notDesignedForCompilation();
            return ((LinkedHashMap<Object, Object>) hash.getStore()).size();
        }

    }

    @CoreMethod(names = "values", maxArgs = 0)
    public abstract static class ValuesNode extends HashCoreMethodNode {

        public ValuesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ValuesNode(ValuesNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyArray valuesNull(RubyHash hash) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = "isObjectArray", order = 2)
        public RubyArray valuesObjectArray(RubyHash hash) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) hash.getStore();

            final Object[] values = new Object[store.length / 2];

            for (int n = 0; n < values.length; n++) {
                values[n] = store[n * 2 + 1];
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), values, values.length);
        }

        @Specialization(guards = "isObjectLinkedHashMap", order = 3)
        public RubyArray valuesObjectLinkedHashMap(RubyHash hash) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> store = (LinkedHashMap<Object, Object>) hash.getStore();

            final Object[] values = new Object[store.size()];

            int n = 0;

            for (Object value : store.values()) {
                values[n] = value;
                n++;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), values, values.length);
        }

    }

}
