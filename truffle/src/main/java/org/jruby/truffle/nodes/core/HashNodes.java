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
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.hash.FindEntryNode;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
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

        @Child private CallDispatchHeadNode eqlNode;
        @Child private BasicObjectNodes.ReferenceEqualNode equalNode;
        
        private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
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
                        
                        final boolean equal;
                        
                        if (byIdentityProfile.profile(a.isCompareByIdentity())) {
                            equal = (boolean) DebugOperations.send(getContext(), aKeyValue.getKey(), "equal?", null, bEntries.get(n).getKey());
                        } else {
                            equal = (boolean) DebugOperations.send(getContext(), aKeyValue.getKey(), "eql?", null, bEntries.get(n).getKey());
                        }

                        if (equal) {
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

    @CoreMethod(names = "[]", onSingleton = true, argumentsAsArray = true, reallyDoesNeedSelf = true)
    public abstract static class ConstructNode extends HashCoreMethodNode {

        public ConstructNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstructNode(ConstructNode prev) {
            super(prev);
        }

        @ExplodeLoop
        @Specialization(guards = "isSmallArrayOfPairs(hashClass, args)")
        public Object construct(VirtualFrame frame, RubyClass hashClass, Object[] args) {
            final RubyArray array = (RubyArray) args[0];

            final Object[] store = (Object[]) array.getStore();

            final int size = array.getSize();
            final Object[] newStore = new Object[HashOperations.SMALL_HASH_SIZE * 2];

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size) {
                    final Object pair = store[n];

                    if (!(pair instanceof RubyArray)) {
                        CompilerDirectives.transferToInterpreter();
                        return constructFallback(frame, hashClass, args);
                    }

                    final RubyArray pairArray = (RubyArray) pair;

                    if (!(pairArray.getStore() instanceof Object[])) {
                        CompilerDirectives.transferToInterpreter();
                        return constructFallback(frame, hashClass, args);
                    }

                    final Object[] pairStore = (Object[]) pairArray.getStore();

                    newStore[n * 2] = pairStore[0];
                    newStore[n * 2 + 1] = pairStore[1];
                }
            }

            return new RubyHash(hashClass, null, null, newStore, size, null);
        }

        @Specialization
        public Object constructFallback(VirtualFrame frame, RubyClass hashClass, Object[] args) {
            return ruby(frame, "_constructor_fallback(*args)", "args", RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), args));
        }

        public static boolean isSmallArrayOfPairs(RubyClass hashClass, Object[] args) {
            if (args.length != 1) {
                return false;
            }

            final Object arg = args[0];

            if (!(arg instanceof RubyArray)) {
                return false;
            }

            final RubyArray array = (RubyArray) arg;

            if (!(array.getStore() instanceof Object[])) {
                return false;
            }

            final Object[] store = (Object[]) array.getStore();

            if (store.length > HashOperations.SMALL_HASH_SIZE) {
                return false;
            }

            return true;
        }

    }

    @CoreMethod(names = "[]", required = 1)
    public abstract static class GetIndexNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode hashNode;
        @Child private CallDispatchHeadNode eqlNode;
        @Child private BasicObjectNodes.ReferenceEqualNode equalNode;
        @Child private CallDispatchHeadNode callDefaultNode;
        @Child private FindEntryNode findEntryNode;

        private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile notInHashProfile = BranchProfile.create();
        private final BranchProfile useDefaultProfile = BranchProfile.create();
        
        @CompilerDirectives.CompilationFinal private Object undefinedValue;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = DispatchHeadNodeFactory.createMethodCall(context, true);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
            callDefaultNode = DispatchHeadNodeFactory.createMethodCall(context);
            findEntryNode = new FindEntryNode(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
            hashNode = prev.hashNode;
            eqlNode = prev.eqlNode;
            equalNode = prev.equalNode;
            callDefaultNode = prev.callDefaultNode;
            findEntryNode = prev.findEntryNode;
            undefinedValue = prev.undefinedValue;
        }
        
        public abstract Object executeGet(VirtualFrame frame, RubyHash hash, Object key);

        @Specialization(guards = "isNull(hash)")
        public Object getNull(VirtualFrame frame, RubyHash hash, Object key) {
            notDesignedForCompilation();

            hashNode.call(frame, key, "hash", null);

            if (undefinedValue != null) {
                return undefinedValue;
            } else {
                return callDefaultNode.call(frame, hash, "default", null, key);
            }
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public Object getPackedArray(VirtualFrame frame, RubyHash hash, Object key) {
            hashNode.call(frame, key, "hash", null);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size) {
                    final boolean equal;

                    if (byIdentityProfile.profile(hash.isCompareByIdentity())) {
                        equal = equalNode.executeReferenceEqual(frame, key, store[n * 2]);
                    } else {
                        equal = eqlNode.callBoolean(frame, key, "eql?", null, store[n * 2]);
                    }
                    
                    if (equal) {
                        return store[n * 2 + 1];
                    }
                }
            }

            notInHashProfile.enter();
            
            if (undefinedValue != null) {
                return undefinedValue;
            }

            useDefaultProfile.enter();
            return callDefaultNode.call(frame, hash, "default", null, key);

        }

        @Specialization(guards = "isBuckets(hash)")
        public Object getBuckets(VirtualFrame frame, RubyHash hash, Object key) {
            final HashSearchResult hashSearchResult = findEntryNode.search(frame, hash, key);

            if (hashSearchResult.getEntry() != null) {
                return hashSearchResult.getEntry().getValue();
            }

            notInHashProfile.enter();

            if (undefinedValue != null) {
                return undefinedValue;
            }

            useDefaultProfile.enter();
            return callDefaultNode.call(frame, hash, "default", null, key);
        }
        
        public void setUndefinedValue(Object undefinedValue) {
            this.undefinedValue = undefinedValue;
        }

    }
    
    @CoreMethod(names = "_get_or_undefined", required = 1)
    public abstract static class GetOrUndefinedNode extends HashCoreMethodNode {

        @Child private GetIndexNode getIndexNode;
        
        public GetOrUndefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getIndexNode = HashNodesFactory.GetIndexNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            getIndexNode.setUndefinedValue(context.getCoreLibrary().getRubiniusUndefined());
        }

        public GetOrUndefinedNode(GetOrUndefinedNode prev) {
            super(prev);
            getIndexNode = prev.getIndexNode;
        }

        @Specialization
        public Object getOrUndefined(VirtualFrame frame, RubyHash hash, Object key) {
            return getIndexNode.executeGet(frame, hash, key);
        }

    }

    @CoreMethod(names = "[]=", required = 2, raiseIfFrozenSelf = true)
    public abstract static class SetIndexNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode hashNode;
        @Child private CallDispatchHeadNode eqlNode;
        @Child private BasicObjectNodes.ReferenceEqualNode equalNode;

        private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile considerExtendProfile = BranchProfile.create();
        private final BranchProfile extendProfile = BranchProfile.create();

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = DispatchHeadNodeFactory.createMethodCall(context, true);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
        }

        public SetIndexNode(SetIndexNode prev) {
            super(prev);
            hashNode = prev.hashNode;
            eqlNode = prev.eqlNode;
            equalNode = prev.equalNode;
        }

        @Specialization(guards = { "isNull(hash)", "!isRubyString(key)" })
        public Object setNull(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            final Object[] store = new Object[HashOperations.SMALL_HASH_SIZE * 2];
            hashNode.call(frame, key, "hash", null);
            store[0] = key;
            store[1] = value;
            hash.setStore(store, 1, null, null);
            return value;
        }

        @Specialization(guards = "isNull(hash)")
        public Object setNull(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            notDesignedForCompilation();
            if (hash.isCompareByIdentity()) {
                return setNull(frame, hash, (Object) key, value);
            } else {
                return setNull(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "!isRubyString(key)"})
        public Object setPackedArray(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            hashNode.call(frame, key, "hash", null);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                if (n < size) {
                    final boolean equal;
                    
                    if (byIdentityProfile.profile(hash.isCompareByIdentity())) {
                        equal = equalNode.executeReferenceEqual(frame, key, store[n * 2]);
                    } else {
                        equal = eqlNode.callBoolean(frame, key, "eql?", null, store[n * 2]);
                    }
                    
                    if (equal) {
                        store[n * 2 + 1] = value;
                        return value;
                    }
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
                HashOperations.verySlowSetInBuckets(hash, keyValue.getKey(), keyValue.getValue(), hash.isCompareByIdentity());
            }

            HashOperations.verySlowSetInBuckets(hash, key, value, false);

            return value;
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public Object setPackedArray(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            notDesignedForCompilation();
            if (hash.isCompareByIdentity()) {
                return setPackedArray(frame, hash, (Object) key, value);
            } else {
                return setPackedArray(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

        @Specialization(guards = {"isBuckets(hash)", "!isRubyString(key)"})
        public Object setBuckets(RubyHash hash, Object key, Object value) {
            notDesignedForCompilation();

            if (HashOperations.verySlowSetInBuckets(hash, key, value, hash.isCompareByIdentity())) {
                hash.setSize(hash.getSize() + 1);
            }

            return value;
        }

        @Specialization(guards = "isBuckets(hash)")
        public Object setBuckets(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            notDesignedForCompilation();
            if (hash.isCompareByIdentity()) {
                return setBuckets(hash, key, value);
            } else {
                return setBuckets(hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
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

    @CoreMethod(names = "compare_by_identity", raiseIfFrozenSelf = true)
    public abstract static class CompareByIdentityNode extends HashCoreMethodNode {

        public CompareByIdentityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareByIdentityNode(CompareByIdentityNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash compareByIdentity(RubyHash hash) {
            hash.setCompareByIdentity(true);
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity?")
    public abstract static class IsCompareByIdentityNode extends HashCoreMethodNode {

        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();
        
        public IsCompareByIdentityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IsCompareByIdentityNode(IsCompareByIdentityNode prev) {
            super(prev);
        }
        
        @Specialization
        public boolean compareByIdentity(RubyHash hash) {
            return profile.profile(hash.isCompareByIdentity());
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

    @CoreMethod(names = "delete", required = 1, needsBlock = true, raiseIfFrozenSelf = true)
    public abstract static class DeleteNode extends HashCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;
        @Child private FindEntryNode findEntryNode;
        @Child private YieldDispatchHeadNode yieldNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            findEntryNode = new FindEntryNode(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
            eqlNode = prev.eqlNode;
            findEntryNode = prev.findEntryNode;
            yieldNode = prev.yieldNode;
        }

        @Specialization(guards = "isNull(hash)")
        public Object deleteNull(VirtualFrame frame, RubyHash hash, Object key, Object block) {
            if (block == UndefinedPlaceholder.INSTANCE) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "!isCompareByIdentity(hash)"})
        public Object deletePackedArray(VirtualFrame frame, RubyHash hash, Object key, Object block) {
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

            if (block == UndefinedPlaceholder.INSTANCE) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = "isBuckets(hash)")
        public Object delete(VirtualFrame frame, RubyHash hash, Object key, Object block) {
            notDesignedForCompilation();

            final HashSearchResult hashSearchResult = findEntryNode.search(frame, hash, key);

            if (hashSearchResult.getEntry() == null) {
                if (block == UndefinedPlaceholder.INSTANCE) {
                    return getContext().getCoreLibrary().getNilObject();
                } else {
                    return yieldNode.dispatch(frame, (RubyProc) block, key);
                }
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

        @Child private CallDispatchHeadNode toEnumNode;
        
        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
            toEnumNode = prev.toEnumNode;
        }

        @Specialization(guards = "isNull(hash)")
        public RubyHash eachNull(RubyHash hash, RubyProc block) {
            return hash;
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyHash eachPackedArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            int count = 0;

            try {
                for (int n = 0; n < HashOperations.SMALL_HASH_SIZE; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    if (n < size) {
                        yield(frame, block, new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{store[n * 2], store[n * 2 + 1]}, 2));
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

        @Specialization
        public Object each(VirtualFrame frame, RubyHash hash, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return toEnumNode.call(frame, hash, "to_enum", null, getContext().getSymbolTable().getSymbol(getName()));
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

    @CoreMethod(names = "initialize", needsBlock = true, optional = 1, raiseIfFrozenSelf = true)
    public abstract static class InitializeNode extends HashCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash initialize(RubyHash hash, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            hash.setStore(null, 0, null, null);
            hash.setDefaultValue(null);
            hash.setDefaultBlock(null);
            return hash;
        }

        @Specialization
        public RubyHash initialize(RubyHash hash, UndefinedPlaceholder defaultValue, RubyProc block) {
            hash.setStore(null, 0, null, null);
            hash.setDefaultValue(null);
            hash.setDefaultBlock(block);
            return hash;
        }

        @Specialization(guards = "!isUndefinedPlaceholder(defaultValue)")
        public RubyHash initialize(RubyHash hash, Object defaultValue, UndefinedPlaceholder block) {
            hash.setStore(null, 0, null, null);
            hash.setDefaultValue(defaultValue);
            hash.setDefaultBlock(null);
            return hash;
        }

        @Specialization(guards = "!isUndefinedPlaceholder(defaultValue)")
        public Object initialize(RubyHash hash, Object defaultValue, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("wrong number of arguments (1 for 0)", this));
        }

    }

    // TODO CS 8-Mar-15 visibility = Visibility.PRIVATE
    @CoreMethod(names = {"initialize_copy", "replace"}, required = 1, raiseIfFrozenSelf = true)
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

            self.setStore(null, 0, null, null);
            copyOther(self, from);

            return self;
        }

        @Specialization(guards = {"!isNull(from)", "!isBuckets(from)"})
        public RubyHash dupPackedArray(RubyHash self, RubyHash from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            final Object[] store = (Object[]) from.getStore();
            self.setStore(Arrays.copyOf(store, HashOperations.SMALL_HASH_SIZE * 2), from.getSize(), null, null);

            copyOther(self, from);

            return self;
        }

        @Specialization(guards = "isBuckets(from)")
        public RubyHash dupBuckets(RubyHash self, RubyHash from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            HashOperations.verySlowSetKeyValues(self, HashOperations.verySlowToKeyValues(from), from.isCompareByIdentity());

            copyOther(self, from);

            return self;
        }

        @Specialization(guards = "!isRubyHash(other)")
        public Object dupBuckets(VirtualFrame frame, RubyHash self, Object other) {
            return ruby(frame, "replace(Rubinius::Type.coerce_to other, Hash, :to_hash)", "other", other);
        }
        
        private void copyOther(RubyHash self, RubyHash from) {
            self.setDefaultBlock(from.getDefaultBlock());
            self.setDefaultValue(from.getDefaultValue());
            self.setCompareByIdentity(from.isCompareByIdentity());
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

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "!isCompareByIdentity(hash)"})
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

        @Specialization(guards = {"isBuckets(hash)", "!isCompareByIdentity(hash)"})
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

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapNode(MapNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyArray mapNull(VirtualFrame frame, RubyHash hash, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
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

    @ImportStatic(HashGuards.class)
    @CoreMethod(names = "merge", required = 1, needsBlock = true)
    public abstract static class MergeNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;
        @Child private CallDispatchHeadNode fallbackCallNode;

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
            fallbackCallNode = prev.fallbackCallNode;
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "isNull(other)", "!isCompareByIdentity(hash)"})
        public RubyHash mergePackedArrayNull(RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            final Object[] store = (Object[]) hash.getStore();
            final Object[] copy = Arrays.copyOf(store, HashOperations.SMALL_HASH_SIZE * 2);

            return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), copy, hash.getSize(), null);
        }

        @ExplodeLoop
        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)", "!isNull(other)", "!isBuckets(other)", "!isCompareByIdentity(hash)"})
        public RubyHash mergePackedArrayPackedArray(VirtualFrame frame, RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            // TODO(CS): what happens with the default block here? Which side does it get merged from?

            final Object[] storeA = (Object[]) hash.getStore();
            final int storeASize = hash.getSize();

            final Object[] storeB = (Object[]) other.getStore();
            final int storeBSize = other.getSize();

            final boolean[] mergeFromA = new boolean[storeASize];
            int mergeFromACount = 0;

            int conflictsCount = 0;

            for (int a = 0; a < HashOperations.SMALL_HASH_SIZE; a++) {
                if (a < storeASize) {
                    boolean merge = true;

                    for (int b = 0; b < HashOperations.SMALL_HASH_SIZE; b++) {
                        if (b < storeBSize) {
                            if (eqlNode.callBoolean(frame, storeA[a * 2], "eql?", null, storeB[b * 2])) {
                                conflictsCount++;
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
                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), Arrays.copyOf(storeB, HashOperations.SMALL_HASH_SIZE * 2), storeBSize, null);
            }

            considerNothingFromSecondProfile.enter();

            if (conflictsCount == storeBSize) {
                nothingFromSecondProfile.enter();
                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), Arrays.copyOf(storeA, HashOperations.SMALL_HASH_SIZE * 2), storeASize, null);
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

                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), merged, mergedSize, null);
            }

            CompilerDirectives.transferToInterpreter();

            return mergeBucketsBuckets(hash, other, block);
        }
        
        // TODO CS 3-Mar-15 need negative guards on this
        @Specialization(guards = "!isCompareByIdentity(hash)")
        public RubyHash mergeBucketsBuckets(RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final RubyHash merged = new RubyHash(hash.getLogicalClass(), null, null, new Entry[HashOperations.capacityGreaterThan(hash.getSize() + other.getSize())], 0, null);

            int size = 0;

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue(), false);
                size++;
            }

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(other)) {
                if (HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue(), false)) {
                    size++;
                }
            }

            merged.setSize(size);

            return merged;
        }

        @Specialization(guards = "!isCompareByIdentity(hash)")
        public RubyHash merge(VirtualFrame frame, RubyHash hash, RubyHash other, RubyProc block) {
            notDesignedForCompilation();
            
            final RubyHash merged = new RubyHash(hash.getLogicalClass(), null, null, new Entry[HashOperations.capacityGreaterThan(hash.getSize() + other.getSize())], 0, null);

            int size = 0;

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue(), false);
                size++;
            }

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(other)) {
                final HashSearchResult searchResult = HashOperations.verySlowFindBucket(merged, keyValue.getKey(), false);
                
                if (searchResult.getEntry() == null) {
                    HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue(), false);
                    size++;
                } else {
                    final Object oldValue = searchResult.getEntry().getValue();
                    final Object newValue = keyValue.getValue();
                    final Object mergedValue = yield(frame, block, keyValue.getKey(), oldValue, newValue);
                    
                    HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), mergedValue, false);
                }
            }

            merged.setSize(size);

            return merged;
        }

        @Specialization(guards = {"!isRubyHash(other)", "!isCompareByIdentity(hash)"})
        public Object merge(VirtualFrame frame, RubyHash hash, Object other, Object block) {
            notDesignedForCompilation();

            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }
            
            final RubyProc blockProc;
            
            if (block == UndefinedPlaceholder.INSTANCE) {
                blockProc = null;
            } else {
                blockProc = (RubyProc) block;
            }

            return fallbackCallNode.call(frame, hash, "merge_fallback", blockProc, other);
        }

    }

    @CoreMethod(names = "default=", required = 1)
    public abstract static class SetDefaultNode extends HashCoreMethodNode {

        public SetDefaultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultNode(SetDefaultNode prev) {
            super(prev);
        }

        @Specialization
        public Object setDefault(VirtualFrame frame, RubyHash hash, Object defaultValue) {
            notDesignedForCompilation();

            ruby(frame, "Rubinius.check_frozen");
            
            hash.setDefaultValue(defaultValue);
            hash.setDefaultBlock(null);
            return defaultValue;
        }
    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true)
    public abstract static class ShiftNode extends HashCoreMethodNode {

        public ShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ShiftNode(ShiftNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isEmpty(hash)", "!hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public RubyNilClass shiftEmpty(RubyHash hash) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = {"isEmpty(hash)", "hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public Object shiftEmpyDefaultValue(RubyHash hash) {
            return hash.getDefaultValue();
        }

        @Specialization(guards = {"isEmpty(hash)", "!hasDefaultValue(hash)", "hasDefaultBlock(hash)"})
        public Object shiftEmptyDefaultProc(RubyHash hash) {
            notDesignedForCompilation();
            
            return hash.getDefaultBlock().rootCall(hash, getContext().getCoreLibrary().getNilObject());
        }

        @Specialization(guards = {"!isEmpty(hash)", "!isNull(hash)", "!isBuckets(hash)"})
        public RubyArray shiftPackedArray(RubyHash hash) {
            notDesignedForCompilation();
            
            final Object[] store = (Object[]) hash.getStore();
            
            final Object key = store[0];
            final Object value = store[1];
            
            System.arraycopy(store, 2, store, 0, HashOperations.SMALL_HASH_SIZE * 2 - 2);
            
            hash.setSize(hash.getSize() - 1);
            
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

        @Specialization(guards = {"!isEmpty(hash)", "isBuckets(hash)"})
        public RubyArray shiftBuckets(RubyHash hash) {
            notDesignedForCompilation();
            
            final Entry first = hash.getFirstInSequence();

            final Object key = first.getKey();
            final Object value = first.getValue();
            
            hash.setFirstInSequence(first.getNextInSequence());
            
            if (first.getPreviousInSequence() != null) {
                first.getNextInSequence().setPreviousInSequence(null);
            }

            if (hash.getLastInSequence() == first) {
                hash.setLastInSequence(null);
            }
            
            /*
             * TODO CS 7-Mar-15 this isn't great - we need to remove from the
             * lookup sequence for which we need the previous entry in the
             * bucket. However we normally get that from the search result, and
             * we haven't done a search here - we've just taken the first
             * result. For the moment we'll just do a manual search.
             */
            
            final Entry[] store = (Entry[]) hash.getStore();
            
            bucketLoop: for (int n = 0; n < store.length; n++) {
                Entry previous = null;
                Entry entry = store[n];
                
                while (entry != null) {
                    if (entry == first) {
                        if (previous == null) {
                            store[n] = first.getNextInLookup();
                        } else {
                            previous.setNextInLookup(first.getNextInLookup());
                        }
                        
                        break bucketLoop;
                    }
                    
                    previous = entry;
                    entry = entry.getNextInLookup();
                }
            }
            
            hash.setSize(hash.getSize() - 1);

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

    }
    
    @CoreMethod(names = {"size", "length"})
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

    @CoreMethod(names = "rehash", raiseIfFrozenSelf = true)
    public abstract static class RehashNode extends HashCoreMethodNode {

        public RehashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RehashNode(RehashNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(hash)")
        public RubyHash rehashNull(RubyHash hash) {
            return hash;
        }

        @Specialization(guards = {"!isNull(hash)", "!isBuckets(hash)"})
        public RubyHash rehashPackedArray(RubyHash hash) {
            // Nothing to do as we weren't using the hash code anyway
            return hash;
        }

        @Specialization(guards = "isBuckets(hash)")
        public RubyHash rehashBuckets(RubyHash hash) {
            notDesignedForCompilation();
            
            HashOperations.verySlowSetKeyValues(hash, HashOperations.verySlowToKeyValues(hash), hash.isCompareByIdentity());
            
            return hash;
        }

    }

    // Not a core method, used to simulate Rubinius @default.
    @NodeChild(value = "self")
    public abstract static class DefaultValueNode extends RubyNode {

        public DefaultValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefaultValueNode(DefaultValueNode prev) {
            super(prev);
        }

        @Specialization
        public Object defaultValue(RubyHash hash) {
            final Object value = hash.getDefaultValue();
            
            if (value == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return value;
            }
        }
    }

    // Not a core method, used to simulate Rubinius @default.
    @NodeChildren({ @NodeChild("self"), @NodeChild("defaultValue") })
    public abstract static class SetDefaultValueNode extends RubyNode {

        public SetDefaultValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultValueNode(SetDefaultValueNode prev) {
            super(prev);
        }

        @Specialization
        public Object setDefaultValue(RubyHash hash, Object defaultValue) {
            hash.setDefaultValue(defaultValue);
            return defaultValue;
        }
        
    }

    // Not a core method, used to simulate Rubinius @default_proc.
    @NodeChildren({ @NodeChild("self"), @NodeChild("defaultProc") })
    public abstract static class SetDefaultProcNode extends RubyNode {

        public SetDefaultProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetDefaultProcNode(SetDefaultProcNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc setDefaultProc(RubyHash hash, RubyProc defaultProc) {
            hash.setDefaultValue(null);
            hash.setDefaultBlock(defaultProc);
            return defaultProc;
        }

        @Specialization
        public RubyNilClass setDefaultProc(RubyHash hash, RubyNilClass nil) {
            hash.setDefaultValue(null);
            hash.setDefaultBlock(null);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
