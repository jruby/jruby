/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.Log;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayBuilderNode;
import org.jruby.truffle.core.hash.HashNodesFactory.DefaultValueNodeFactory;
import org.jruby.truffle.core.hash.HashNodesFactory.GetIndexNodeFactory;
import org.jruby.truffle.core.hash.HashNodesFactory.InternalRehashNodeGen;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.yield.YieldNode;

import java.util.Arrays;

@CoreClass("Hash")
public abstract class HashNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocateHash(rubyClass, null, 0, null, null, null, null, false);
        }

    }

    @CoreMethod(names = "[]", constructor = true, rest = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ConstructNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode = new HashNode();
        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

        @ExplodeLoop
        @Specialization(guards = "isSmallArrayOfPairs(args)")
        public Object construct(
                        VirtualFrame frame,
                        DynamicObject hashClass,
                        Object[] args,
                        @Cached("new()") SnippetNode snippetNode) {
            final DynamicObject array = (DynamicObject) args[0];

            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            final int size = Layouts.ARRAY.getSize(array);
            final Object[] newStore = PackedArrayStrategy.createStore(getContext());

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < size) {
                    final Object pair = store[n];

                    if (!RubyGuards.isRubyArray(pair)) {
                        return snippetNode.execute(frame, "_constructor_fallback(*args)", "args", createArray(args, args.length));
                    }

                    final DynamicObject pairArray = (DynamicObject) pair;
                    final Object pairStore = Layouts.ARRAY.getStore(pairArray);

                    if (pairStore != null && pairStore.getClass() != Object[].class) {
                        return snippetNode.execute(frame, "_constructor_fallback(*args)", "args", createArray(args, args.length));
                    }

                    if (Layouts.ARRAY.getSize(pairArray) != 2) {
                        return snippetNode.execute(frame, "_constructor_fallback(*args)", "args", createArray(args, args.length));
                    }

                    final Object[] pairObjectStore = (Object[]) pairStore;

                    final Object key = pairObjectStore[0];
                    final Object value = pairObjectStore[1];

                    final int hashed = hashNode.hash(frame, key, false);

                    PackedArrayStrategy.setHashedKeyValue(newStore, n, hashed, key, value);
                }
            }

            return allocateObjectNode.allocateHash(hashClass, newStore, size, null, null, null, null, false);
        }

        @Specialization(guards = "!isSmallArrayOfPairs(args)")
        public Object constructFallback(
                        VirtualFrame frame,
                        DynamicObject hashClass,
                        Object[] args,
                        @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "_constructor_fallback(*args)", "args", createArray(args, args.length));
        }

        public boolean isSmallArrayOfPairs(Object[] args) {
            if (args.length != 1) {
                return false;
            }

            final Object arg = args[0];

            if (!RubyGuards.isRubyArray(arg)) {
                return false;
            }

            final DynamicObject array = (DynamicObject) arg;
            final Object store = Layouts.ARRAY.getStore(array);

            if (store == null || store.getClass() != Object[].class) {
                return false;
            }

            final Object[] objectStore = (Object[]) store;

            if (objectStore.length > getContext().getOptions().HASH_PACKED_ARRAY_MAX) {
                return false;
            }

            return true;
        }

    }

    @CoreMethod(names = "[]", required = 1)
    @ImportStatic(HashGuards.class)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode callDefaultNode = DispatchHeadNodeFactory.createMethodCall();
        @Child private LookupEntryNode lookupEntryNode = new LookupEntryNode();
        @Child private HashNode hashNode = new HashNode();
        @Child private CompareHashKeysNode compareHashKeysNode = new CompareHashKeysNode();

        @CompilationFinal private Object undefinedValue;

        public abstract Object executeGet(VirtualFrame frame, DynamicObject hash, Object key);

        @Specialization(guards = "isNullHash(hash)")
        public Object getNull(VirtualFrame frame, DynamicObject hash, Object key) {
            if (undefinedValue != null) {
                return undefinedValue;
            } else {
                return callDefaultNode.call(frame, hash, "default", key);
            }
        }

        @Specialization(guards = {
                        "isPackedHash(hash)",
                        "isCompareByIdentity(hash) == cachedByIdentity",
                        "cachedIndex >= 0",
                        "cachedIndex < getSize(hash)",
                        "compareKeysAtIndex(frame, hash, key, cachedIndex, cachedByIdentity)"
        }, limit = "1")
        public Object getConstantIndexPackedArray(VirtualFrame frame, DynamicObject hash, Object key,
                        @Cached("index(frame, hash, key)") int cachedIndex,
                        @Cached("isCompareByIdentity(hash)") boolean cachedByIdentity) {
            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            return PackedArrayStrategy.getValue(store, cachedIndex);
        }

        protected int index(VirtualFrame frame, DynamicObject hash, Object key) {
            if (!HashGuards.isPackedHash(hash)) {
                return -1;
            }

            boolean compareByIdentity = Layouts.HASH.getCompareByIdentity(hash);
            int hashed = hashNode.hash(frame, key, compareByIdentity);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            final int size = Layouts.HASH.getSize(hash);

            for (int n = 0; n < size; n++) {
                final int otherHashed = PackedArrayStrategy.getHashed(store, n);
                final Object otherKey = PackedArrayStrategy.getKey(store, n);
                if (equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed)) {
                    return n;
                }
            }

            return -1;
        }

        protected boolean compareKeysAtIndex(VirtualFrame frame, DynamicObject hash, Object key, int cachedIndex, boolean cachedByIdentity) {
            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            Object other = PackedArrayStrategy.getKey(store, cachedIndex);
            int otherHashed = PackedArrayStrategy.getHashed(store, cachedIndex);
            int hashed = hashNode.hash(frame, key, cachedByIdentity);
            return equalKeys(frame, cachedByIdentity, key, hashed, other, otherHashed);
        }

        protected int getSize(DynamicObject hash) {
            return Layouts.HASH.getSize(hash);
        }

        protected boolean equalKeys(VirtualFrame frame, boolean compareByIdentity, Object key, int hashed, Object otherKey, int otherHashed) {
            return compareHashKeysNode.equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed);
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)", contains = "getConstantIndexPackedArray")
        public Object getPackedArray(VirtualFrame frame, DynamicObject hash, Object key,
                        @Cached("create()") BranchProfile notInHashProfile,
                        @Cached("create()") BranchProfile useDefaultProfile,
                        @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile) {
            final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));
            final int hashed = hashNode.hash(frame, key, compareByIdentity);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            final int size = Layouts.HASH.getSize(hash);

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < size) {
                    final int otherHashed = PackedArrayStrategy.getHashed(store, n);
                    final Object otherKey = PackedArrayStrategy.getKey(store, n);
                    if (equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed)) {
                        return PackedArrayStrategy.getValue(store, n);
                    }
                }
            }

            notInHashProfile.enter();

            if (undefinedValue != null) {
                return undefinedValue;
            }

            useDefaultProfile.enter();
            return callDefaultNode.call(frame, hash, "default", key);

        }

        @Specialization(guards = "isBucketHash(hash)")
        public Object getBuckets(VirtualFrame frame, DynamicObject hash, Object key,
                        @Cached("create()") BranchProfile notInHashProfile,
                        @Cached("create()") BranchProfile useDefaultProfile) {
            final HashLookupResult hashLookupResult = lookupEntryNode.lookup(frame, hash, key);

            if (hashLookupResult.getEntry() != null) {
                return hashLookupResult.getEntry().getValue();
            }

            notInHashProfile.enter();

            if (undefinedValue != null) {
                return undefinedValue;
            }

            useDefaultProfile.enter();
            return callDefaultNode.call(frame, hash, "default", key);
        }

        public void setUndefinedValue(Object undefinedValue) {
            this.undefinedValue = undefinedValue;
        }

    }

    @CoreMethod(names = "_get_or_undefined", required = 1)
    public abstract static class GetOrUndefinedNode extends CoreMethodArrayArgumentsNode {

        @Child private GetIndexNode getIndexNode;

        public GetOrUndefinedNode() {
            getIndexNode = GetIndexNodeFactory.create(null);
            getIndexNode.setUndefinedValue(NotProvided.INSTANCE);
        }

        @Specialization
        public Object getOrUndefined(VirtualFrame frame, DynamicObject hash, Object key) {
            return getIndexNode.executeGet(frame, hash, key);
        }

    }

    @CoreMethod(names = "[]=", required = 2, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class SetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private SetNode setNode = SetNode.create();

        @Specialization
        public Object set(VirtualFrame frame, DynamicObject hash, Object key, Object value) {
            return setNode.executeSet(frame, hash, key, value, Layouts.HASH.getCompareByIdentity(hash));
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNullHash(hash)")
        public DynamicObject emptyNull(DynamicObject hash) {
            return hash;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public DynamicObject empty(DynamicObject hash) {
            assert HashOperations.verifyStore(getContext(), hash);
            assert HashOperations.verifyStore(getContext(), null, 0, null, null);
            Layouts.HASH.setStore(hash, null);
            Layouts.HASH.setSize(hash, 0);
            Layouts.HASH.setFirstInSequence(hash, null);
            Layouts.HASH.setLastInSequence(hash, null);

            assert HashOperations.verifyStore(getContext(), hash);
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class CompareByIdentityNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "!isCompareByIdentity(hash)")
        DynamicObject compareByIdentity(VirtualFrame frame, DynamicObject hash,
                @Cached("create()") InternalRehashNode internalRehashNode) {
            Layouts.HASH.setCompareByIdentity(hash, true);
            return internalRehashNode.executeRehash(frame, hash);
        }

        @Specialization(guards = "isCompareByIdentity(hash)")
        DynamicObject alreadyCompareByIdentity(DynamicObject hash) {
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity?")
    public abstract static class IsCompareByIdentityNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

        @Specialization
        public boolean compareByIdentity(DynamicObject hash) {
            return profile.profile(Layouts.HASH.getCompareByIdentity(hash));
        }

    }

    @CoreMethod(names = "default_proc")
    public abstract static class DefaultProcNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object defaultProc(DynamicObject hash,
                        @Cached("createBinaryProfile()") ConditionProfile defaultBlockNullProfile) {
            if (defaultBlockNullProfile.profile(Layouts.HASH.getDefaultBlock(hash) == null)) {
                return nil();
            } else {
                return Layouts.HASH.getDefaultBlock(hash);
            }
        }

    }

    @CoreMethod(names = "delete", required = 1, needsBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class DeleteNode extends CoreMethodArrayArgumentsNode {

        @Child private CompareHashKeysNode compareHashKeysNode = new CompareHashKeysNode();
        @Child private HashNode hashNode = new HashNode();
        @Child private LookupEntryNode lookupEntryNode = new LookupEntryNode();
        @Child private YieldNode yieldNode = new YieldNode();

        @Specialization(guards = "isNullHash(hash)")
        public Object deleteNull(VirtualFrame frame, DynamicObject hash, Object key, NotProvided block) {
            assert HashOperations.verifyStore(getContext(), hash);

            return nil();
        }

        @Specialization(guards = "isNullHash(hash)")
        public Object deleteNull(VirtualFrame frame, DynamicObject hash, Object key, DynamicObject block) {
            assert HashOperations.verifyStore(getContext(), hash);

            return yieldNode.dispatch(frame, block, key);
        }

        @Specialization(guards = "isPackedHash(hash)")
        public Object deletePackedArray(VirtualFrame frame, DynamicObject hash, Object key, Object maybeBlock,
                        @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile) {
            assert HashOperations.verifyStore(getContext(), hash);
            final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));
            final int hashed = hashNode.hash(frame, key, compareByIdentity);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            final int size = Layouts.HASH.getSize(hash);

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < size) {
                    final int otherHashed = PackedArrayStrategy.getHashed(store, n);
                    final Object otherKey = PackedArrayStrategy.getKey(store, n);

                    if (equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed)) {
                        final Object value = PackedArrayStrategy.getValue(store, n);
                        PackedArrayStrategy.removeEntry(getContext(), store, n);
                        Layouts.HASH.setSize(hash, size - 1);
                        assert HashOperations.verifyStore(getContext(), hash);
                        return value;
                    }
                }
            }

            assert HashOperations.verifyStore(getContext(), hash);

            if (maybeBlock == NotProvided.INSTANCE) {
                return nil();
            } else {
                return yieldNode.dispatch(frame, (DynamicObject) maybeBlock, key);
            }
        }

        @Specialization(guards = "isBucketHash(hash)")
        public Object delete(VirtualFrame frame, DynamicObject hash, Object key, Object maybeBlock) {
            assert HashOperations.verifyStore(getContext(), hash);

            final HashLookupResult hashLookupResult = lookupEntryNode.lookup(frame, hash, key);

            if (hashLookupResult.getEntry() == null) {
                if (maybeBlock == NotProvided.INSTANCE) {
                    return nil();
                } else {
                    return yieldNode.dispatch(frame, (DynamicObject) maybeBlock, key);
                }
            }

            final Entry entry = hashLookupResult.getEntry();

            // Remove from the sequence chain

            if (entry.getPreviousInSequence() == null) {
                assert Layouts.HASH.getFirstInSequence(hash) == entry;
                Layouts.HASH.setFirstInSequence(hash, entry.getNextInSequence());
            } else {
                assert Layouts.HASH.getFirstInSequence(hash) != entry;
                entry.getPreviousInSequence().setNextInSequence(entry.getNextInSequence());
            }

            if (entry.getNextInSequence() == null) {
                Layouts.HASH.setLastInSequence(hash, entry.getPreviousInSequence());
            } else {
                entry.getNextInSequence().setPreviousInSequence(entry.getPreviousInSequence());
            }

            // Remove from the lookup chain

            if (hashLookupResult.getPreviousEntry() == null) {
                ((Entry[]) Layouts.HASH.getStore(hash))[hashLookupResult.getIndex()] = entry.getNextInLookup();
            } else {
                hashLookupResult.getPreviousEntry().setNextInLookup(entry.getNextInLookup());
            }

            Layouts.HASH.setSize(hash, Layouts.HASH.getSize(hash) - 1);

            assert HashOperations.verifyStore(getContext(), hash);

            return entry.getValue();
        }

        protected boolean equalKeys(VirtualFrame frame, boolean compareByIdentity, Object key, int hashed, Object otherKey, int otherHashed) {
            return compareHashKeysNode.equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed);
        }

    }

    @CoreMethod(names = {"each", "each_pair"}, needsBlock = true, enumeratorSize = "size")
    @ImportStatic(HashGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        @Specialization(guards = "isNullHash(hash)")
        public DynamicObject eachNull(DynamicObject hash, DynamicObject block) {
            return hash;
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)")
        public DynamicObject eachPackedArray(VirtualFrame frame, DynamicObject hash, DynamicObject block) {
            assert HashOperations.verifyStore(getContext(), hash);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);

            int count = 0;

            try {
                for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    if (n < Layouts.HASH.getSize(hash)) {
                        yieldPair(frame, block, PackedArrayStrategy.getKey(store, n), PackedArrayStrategy.getValue(store, n));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return hash;
        }

        @Specialization(guards = "isBucketHash(hash)")
        public DynamicObject eachBuckets(VirtualFrame frame, DynamicObject hash, DynamicObject block) {
            assert HashOperations.verifyStore(getContext(), hash);

            for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(hash))) {
                yieldPair(frame, block, keyValue.getKey(), keyValue.getValue());
            }

            return hash;
        }

        @Specialization
        public Object each(VirtualFrame frame, DynamicObject hash, NotProvided block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf());
            }

            InternalMethod method = RubyArguments.getMethod(frame);
            return toEnumNode.call(frame, hash, "to_enum", getSymbol(method.getName()));
        }

        private Object yieldPair(VirtualFrame frame, DynamicObject block, Object key, Object value) {
            return yield(frame, block, createArray(new Object[]{key, value}, 2));
        }

    }

    @CoreMethod(names = "empty?")
    @ImportStatic(HashGuards.class)
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNullHash(hash)")
        public boolean emptyNull(DynamicObject hash) {
            return true;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public boolean emptyPackedArray(DynamicObject hash) {
            return Layouts.HASH.getSize(hash) == 0;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 1, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject initialize(DynamicObject hash, NotProvided defaultValue, NotProvided block) {
            assert HashOperations.verifyStore(getContext(), null, 0, null, null);
            Layouts.HASH.setStore(hash, null);
            Layouts.HASH.setSize(hash, 0);
            Layouts.HASH.setFirstInSequence(hash, null);
            Layouts.HASH.setLastInSequence(hash, null);

            Layouts.HASH.setDefaultValue(hash, null);
            Layouts.HASH.setDefaultBlock(hash, null);
            return hash;
        }

        @Specialization
        public DynamicObject initialize(DynamicObject hash, NotProvided defaultValue, DynamicObject block) {
            assert HashOperations.verifyStore(getContext(), null, 0, null, null);
            Layouts.HASH.setStore(hash, null);
            Layouts.HASH.setSize(hash, 0);
            Layouts.HASH.setFirstInSequence(hash, null);
            Layouts.HASH.setLastInSequence(hash, null);

            Layouts.HASH.setDefaultValue(hash, null);
            Layouts.HASH.setDefaultBlock(hash, block);
            return hash;
        }

        @Specialization(guards = "wasProvided(defaultValue)")
        public DynamicObject initialize(DynamicObject hash, Object defaultValue, NotProvided block) {
            assert HashOperations.verifyStore(getContext(), null, 0, null, null);
            Layouts.HASH.setStore(hash, null);
            Layouts.HASH.setSize(hash, 0);
            Layouts.HASH.setFirstInSequence(hash, null);
            Layouts.HASH.setLastInSequence(hash, null);

            Layouts.HASH.setDefaultValue(hash, defaultValue);
            Layouts.HASH.setDefaultBlock(hash, null);
            return hash;
        }

        @Specialization(guards = "wasProvided(defaultValue)")
        public Object initialize(DynamicObject hash, Object defaultValue, DynamicObject block) {
            throw new RaiseException(coreExceptions().argumentError("wrong number of arguments (1 for 0)", this));
        }

    }

    @CoreMethod(names = {"initialize_copy", "replace"}, required = 1, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = {"isRubyHash(from)", "isNullHash(from)"})
        public DynamicObject replaceNull(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }

            assert HashOperations.verifyStore(getContext(), null, 0, null, null);
            Layouts.HASH.setStore(self, null);
            Layouts.HASH.setSize(self, 0);
            Layouts.HASH.setFirstInSequence(self, null);
            Layouts.HASH.setLastInSequence(self, null);

            copyOtherFields(self, from);

            return self;
        }

        @Specialization(guards = {"isRubyHash(from)", "isPackedHash(from)"})
        public DynamicObject replacePackedArray(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }

            final Object[] store = (Object[]) Layouts.HASH.getStore(from);
            Object store1 = PackedArrayStrategy.copyStore(getContext(), store);
            int size = Layouts.HASH.getSize(from);
            assert HashOperations.verifyStore(getContext(), store1, size, null, null);
            Layouts.HASH.setStore(self, store1);
            Layouts.HASH.setSize(self, size);
            Layouts.HASH.setFirstInSequence(self, null);
            Layouts.HASH.setLastInSequence(self, null);

            copyOtherFields(self, from);

            assert HashOperations.verifyStore(getContext(), self);

            return self;
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyHash(from)", "isBucketHash(from)"})
        public DynamicObject replaceBuckets(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }

            BucketsStrategy.copyInto(getContext(), from, self);
            copyOtherFields(self, from);

            assert HashOperations.verifyStore(getContext(), self);

            return self;
        }

        @Specialization(guards = "!isRubyHash(other)")
        public Object replaceBuckets(
                        VirtualFrame frame,
                        DynamicObject self,
                        Object other,
                        @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "replace(Rubinius::Type.coerce_to other, Hash, :to_hash)", "other", other);
        }

        private void copyOtherFields(DynamicObject self, DynamicObject from) {
            Layouts.HASH.setDefaultBlock(self, Layouts.HASH.getDefaultBlock(from));
            Layouts.HASH.setDefaultValue(self, Layouts.HASH.getDefaultValue(from));
            Layouts.HASH.setCompareByIdentity(self, Layouts.HASH.getCompareByIdentity(from));
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullHash(hash)")
        public DynamicObject mapNull(VirtualFrame frame, DynamicObject hash, DynamicObject block) {
            assert HashOperations.verifyStore(getContext(), hash);

            return createArray(null, 0);
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)")
        public DynamicObject mapPackedArray(VirtualFrame frame, DynamicObject hash, DynamicObject block,
                        @Cached("create()") ArrayBuilderNode arrayBuilderNode) {
            assert HashOperations.verifyStore(getContext(), hash);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);

            final int length = Layouts.HASH.getSize(hash);
            Object resultStore = arrayBuilderNode.start(length);

            try {
                for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                    if (n < length) {
                        final Object key = PackedArrayStrategy.getKey(store, n);
                        final Object value = PackedArrayStrategy.getValue(store, n);
                        resultStore = arrayBuilderNode.appendValue(resultStore, n, yieldPair(frame, block, key, value));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, length);
                }
            }

            return createArray(arrayBuilderNode.finish(resultStore, length), length);
        }

        @Specialization(guards = "isBucketHash(hash)")
        public DynamicObject mapBuckets(VirtualFrame frame, DynamicObject hash, DynamicObject block,
                        @Cached("create()") ArrayBuilderNode arrayBuilderNode) {
            assert HashOperations.verifyStore(getContext(), hash);

            final int length = Layouts.HASH.getSize(hash);
            Object store = arrayBuilderNode.start(length);

            int index = 0;

            try {
                for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(hash))) {
                    arrayBuilderNode.appendValue(store, index, yieldPair(frame, block, keyValue.getKey(), keyValue.getValue()));
                    index++;
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, length);
                }
            }

            return createArray(arrayBuilderNode.finish(store, length), length);
        }

        private Object yieldPair(VirtualFrame frame, DynamicObject block, Object key, Object value) {
            return yield(frame, block, createArray(new Object[]{key, value}, 2));
        }

    }

    @ImportStatic(HashGuards.class)
    @CoreMethod(names = "merge", required = 1, needsBlock = true)
    public abstract static class MergeNode extends YieldingCoreMethodNode {

        @Child private LookupEntryNode lookupEntryNode;
        @Child private SetNode setNode = SetNode.create();
        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private CompareHashKeysNode compareHashKeysNode = new CompareHashKeysNode();

        // Merge with an empty hash, without a block

        @Specialization(guards = {"isNullHash(hash)", "isRubyHash(other)", "isNullHash(other)"})
        public DynamicObject mergeEmptyEmpty(DynamicObject hash, DynamicObject other, NotProvided block) {
            return newHash(hash, null, 0, Layouts.HASH.getCompareByIdentity(hash));
        }

        @Specialization(guards = {"isEmptyHash(hash)", "isRubyHash(other)", "isPackedHash(other)"})
        public DynamicObject mergeEmptyPacked(DynamicObject hash, DynamicObject other, NotProvided block) {
            final Object[] store = (Object[]) Layouts.HASH.getStore(other);
            final Object[] copy = PackedArrayStrategy.copyStore(getContext(), store);
            return newHash(hash, copy, Layouts.HASH.getSize(other), Layouts.HASH.getCompareByIdentity(hash));
        }

        @Specialization(guards = {"isPackedHash(hash)", "isRubyHash(other)", "isEmptyHash(other)"})
        public DynamicObject mergePackedEmpty(DynamicObject hash, DynamicObject other, NotProvided block) {
            return mergeEmptyPacked(other, hash, block);
        }

        @Specialization(guards = {"isEmptyHash(hash)", "isRubyHash(other)", "isBucketHash(other)"})
        public DynamicObject mergeEmptyBuckets(DynamicObject hash, DynamicObject other, NotProvided block) {
            final DynamicObject merged = newHash(hash, null, 0, Layouts.HASH.getCompareByIdentity(hash));
            BucketsStrategy.copyInto(getContext(), other, merged);
            return merged;
        }

        @Specialization(guards = {"isBucketHash(hash)", "isRubyHash(other)", "isEmptyHash(other)"})
        public DynamicObject mergeBucketsEmpty(DynamicObject hash, DynamicObject other, NotProvided block) {
            return mergeEmptyBuckets(other, hash, block);
        }

        // Merge non-empty packed with non-empty packed, without a block

        @ExplodeLoop
        @Specialization(guards = {"isPackedHash(hash)", "!isEmptyHash(hash)", "isRubyHash(other)", "isPackedHash(other)", "!isEmptyHash(other)"})
        public DynamicObject mergePackedPacked(VirtualFrame frame, DynamicObject hash, DynamicObject other, NotProvided block,
                        @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nothingFromFirstProfile,
                        @Cached("createBinaryProfile()") ConditionProfile resultIsSmallProfile) {
            assert HashOperations.verifyStore(getContext(), hash);
            assert HashOperations.verifyStore(getContext(), other);

            final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));

            final Object[] storeA = (Object[]) Layouts.HASH.getStore(hash);
            final int storeASize = Layouts.HASH.getSize(hash);

            final Object[] storeB = (Object[]) Layouts.HASH.getStore(other);
            final int storeBSize = Layouts.HASH.getSize(other);

            // Go through and figure out what gets merged from each hash

            final boolean[] mergeFromA = new boolean[storeASize];
            int mergeFromACount = 0;

            for (int a = 0; a < getContext().getOptions().HASH_PACKED_ARRAY_MAX; a++) {
                if (a < storeASize) {
                    boolean merge = true;

                    for (int b = 0; b < getContext().getOptions().HASH_PACKED_ARRAY_MAX; b++) {
                        if (b < storeBSize) {
                            if (equalKeys(frame, compareByIdentity,
                                            PackedArrayStrategy.getKey(storeA, a), PackedArrayStrategy.getHashed(storeA, a),
                                            PackedArrayStrategy.getKey(storeB, b), PackedArrayStrategy.getHashed(storeB, b))) {
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

            // If nothing comes from A, it's easy

            if (nothingFromFirstProfile.profile(mergeFromACount == 0)) {
                return newHash(hash, PackedArrayStrategy.copyStore(getContext(), storeB), storeBSize, compareByIdentity);
            }

            // More complicated case where some things from each hash, but it still fits in a packed
            // array

            final int mergedSize = storeBSize + mergeFromACount;

            if (resultIsSmallProfile.profile(storeBSize + mergeFromACount <= getContext().getOptions().HASH_PACKED_ARRAY_MAX)) {
                final Object[] merged = PackedArrayStrategy.createStore(getContext());

                int index = 0;

                for (int n = 0; n < storeASize; n++) {
                    if (mergeFromA[n]) {
                        PackedArrayStrategy.setHashedKeyValue(merged, index,
                                        PackedArrayStrategy.getHashed(storeA, n),
                                        PackedArrayStrategy.getKey(storeA, n),
                                        PackedArrayStrategy.getValue(storeA, n));
                        index++;
                    }
                }

                for (int n = 0; n < storeBSize; n++) {
                    PackedArrayStrategy.setHashedKeyValue(merged, index,
                                    PackedArrayStrategy.getHashed(storeB, n),
                                    PackedArrayStrategy.getKey(storeB, n),
                                    PackedArrayStrategy.getValue(storeB, n));
                    index++;
                }

                return newHash(hash, merged, mergedSize, compareByIdentity);
            }

            // Most complicated cases where things from both hashes, and it also needs to be
            // promoted to buckets

            final Entry[] newStore = new Entry[BucketsStrategy.capacityGreaterThan(mergedSize)];
            final DynamicObject merged = newHash(hash, newStore, 0, compareByIdentity);

            for (int n = 0; n < storeASize; n++) {
                if (mergeFromA[n]) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(storeA, n), PackedArrayStrategy.getValue(storeA, n), false);
                }
            }

            for (int n = 0; n < storeBSize; n++) {
                setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(storeB, n), PackedArrayStrategy.getValue(storeB, n), false);
            }

            assert HashOperations.verifyStore(getContext(), hash);
            return merged;
        }

        // Merge non-empty buckets with non-empty buckets, without a block

        @Specialization(guards = {"isBucketHash(hash)", "!isEmptyHash(hash)", "isRubyHash(other)", "isBucketHash(other)", "!isEmptyHash(other)"})
        public DynamicObject mergeBucketsBuckets(VirtualFrame frame, DynamicObject hash, DynamicObject other, NotProvided block) {
            final boolean compareByIdentity = Layouts.HASH.getCompareByIdentity(hash);
            final Entry[] newStore = new Entry[BucketsStrategy.capacityGreaterThan(Layouts.HASH.getSize(hash) + Layouts.HASH.getSize(other))];
            final DynamicObject merged = newHash(hash, newStore, 0, compareByIdentity);

            for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(hash))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), compareByIdentity);
            }

            for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(other))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), compareByIdentity);
            }

            assert HashOperations.verifyStore(getContext(), hash);
            return merged;
        }

        // Merge combinations of packed and buckets, without a block

        @Specialization(guards = {"isPackedHash(hash)", "!isEmptyHash(hash)", "isRubyHash(other)", "isBucketHash(other)", "!isEmptyHash(other)"})
        public DynamicObject mergePackedBuckets(VirtualFrame frame, DynamicObject hash, DynamicObject other, NotProvided block) {
            final boolean compareByIdentity = Layouts.HASH.getCompareByIdentity(hash);
            final Entry[] newStore = new Entry[BucketsStrategy.capacityGreaterThan(Layouts.HASH.getSize(hash) + Layouts.HASH.getSize(other))];
            final DynamicObject merged = newHash(hash, newStore, 0, compareByIdentity);

            final Object[] hashStore = (Object[]) Layouts.HASH.getStore(hash);
            final int hashSize = Layouts.HASH.getSize(hash);

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < hashSize) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(hashStore, n), PackedArrayStrategy.getValue(hashStore, n), compareByIdentity);
                }
            }

            for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(other))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), compareByIdentity);
            }

            assert HashOperations.verifyStore(getContext(), hash);
            return merged;
        }

        @Specialization(guards = {"isBucketHash(hash)", "!isEmptyHash(hash)", "isRubyHash(other)", "isPackedHash(other)", "!isEmptyHash(other)"})
        public DynamicObject mergeBucketsPacked(VirtualFrame frame, DynamicObject hash, DynamicObject other, NotProvided block) {
            final boolean compareByIdentity = Layouts.HASH.getCompareByIdentity(hash);
            final Entry[] newStore = new Entry[BucketsStrategy.capacityGreaterThan(Layouts.HASH.getSize(hash) + Layouts.HASH.getSize(other))];
            final DynamicObject merged = newHash(hash, newStore, 0, compareByIdentity);

            for (KeyValue keyValue : BucketsStrategy.iterableKeyValues(Layouts.HASH.getFirstInSequence(hash))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), compareByIdentity);
            }

            final Object[] otherStore = (Object[]) Layouts.HASH.getStore(other);
            final int otherSize = Layouts.HASH.getSize(other);

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < otherSize) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(otherStore, n), PackedArrayStrategy.getValue(otherStore, n), compareByIdentity);
                }
            }

            assert HashOperations.verifyStore(getContext(), hash);
            return merged;
        }

        // Merge using a block

        @Specialization(guards = "isRubyHash(other)")
        public DynamicObject merge(VirtualFrame frame, DynamicObject hash, DynamicObject other, DynamicObject block) {
            Log.notOptimizedOnce("Hash#merge with a block is not yet optimized");
            final boolean compareByIdentity = Layouts.HASH.getCompareByIdentity(hash);

            final int capacity = BucketsStrategy.capacityGreaterThan(Layouts.HASH.getSize(hash) + Layouts.HASH.getSize(other));
            final DynamicObject merged = newHash(hash, new Entry[capacity], 0, compareByIdentity);

            int size = 0;

            for (KeyValue keyValue : HashOperations.iterableKeyValues(hash)) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), false);
                size++;
            }

            if (lookupEntryNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookupEntryNode = insert(new LookupEntryNode());
            }

            for (KeyValue keyValue : HashOperations.iterableKeyValues(other)) {
                final HashLookupResult searchResult = lookupEntryNode.lookup(frame, merged, keyValue.getKey());

                if (searchResult.getEntry() == null) {
                    setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), false);
                    size++;
                } else {
                    final Object oldValue = searchResult.getEntry().getValue();
                    final Object newValue = keyValue.getValue();
                    final Object mergedValue = yield(frame, block, keyValue.getKey(), oldValue, newValue);

                    setNode.executeSet(frame, merged, keyValue.getKey(), mergedValue, false);
                }
            }

            Layouts.HASH.setSize(merged, size);

            assert HashOperations.verifyStore(getContext(), hash);
            return merged;
        }

        // Merge with something that wasn't a hash

        @Specialization(guards = "!isRubyHash(other)")
        public Object merge(VirtualFrame frame, DynamicObject hash, Object other, Object maybeBlock,
                        @Cached("createMethodCall()") CallDispatchHeadNode fallbackCallNode) {
            final DynamicObject block;
            if (maybeBlock == NotProvided.INSTANCE) {
                block = null;
            } else {
                block = (DynamicObject) maybeBlock;
            }

            return fallbackCallNode.callWithBlock(frame, hash, "merge_fallback", block, other);
        }

        private DynamicObject newHash(DynamicObject hash, Object[] store, int size, boolean compareByIdentity) {
            return allocateObjectNode.allocateHash(
                            Layouts.BASIC_OBJECT.getLogicalClass(hash),
                            store, size,
                            null, null,
                            Layouts.HASH.getDefaultBlock(hash),
                            Layouts.HASH.getDefaultValue(hash),
                            compareByIdentity);
        }

        protected boolean equalKeys(VirtualFrame frame, boolean compareByIdentity, Object key, int hashed, Object otherKey, int otherHashed) {
            return compareHashKeysNode.equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed);
        }

    }

    @CoreMethod(names = "default=", required = 1, raiseIfFrozenSelf = true)
    public abstract static class SetDefaultNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object setDefault(DynamicObject hash, Object defaultValue) {
            Layouts.HASH.setDefaultValue(hash, defaultValue);
            Layouts.HASH.setDefaultBlock(hash, null);
            return defaultValue;
        }
    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ShiftNode extends CoreMethodArrayArgumentsNode {

        @Child private YieldNode yieldNode;

        @Specialization(guards = {"isEmptyHash(hash)", "!hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public DynamicObject shiftEmpty(DynamicObject hash) {
            return nil();
        }

        @Specialization(guards = {"isEmptyHash(hash)", "hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public Object shiftEmpyDefaultValue(DynamicObject hash) {
            return Layouts.HASH.getDefaultValue(hash);
        }

        @Specialization(guards = {"isEmptyHash(hash)", "!hasDefaultValue(hash)", "hasDefaultBlock(hash)"})
        public Object shiftEmptyDefaultProc(VirtualFrame frame, DynamicObject hash) {
            if (yieldNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                yieldNode = insert(new YieldNode());
            }

            return yieldNode.dispatch(frame, Layouts.HASH.getDefaultBlock(hash), hash, nil());
        }

        @Specialization(guards = {"!isEmptyHash(hash)", "isPackedHash(hash)"})
        public DynamicObject shiftPackedArray(DynamicObject hash) {
            assert HashOperations.verifyStore(getContext(), hash);

            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);

            final Object key = PackedArrayStrategy.getKey(store, 0);
            final Object value = PackedArrayStrategy.getValue(store, 0);

            PackedArrayStrategy.removeEntry(getContext(), store, 0);

            Layouts.HASH.setSize(hash, Layouts.HASH.getSize(hash) - 1);

            assert HashOperations.verifyStore(getContext(), hash);

            Object[] objects = new Object[]{key, value};
            return createArray(objects, objects.length);
        }

        @Specialization(guards = {"!isEmptyHash(hash)", "isBucketHash(hash)"})
        public DynamicObject shiftBuckets(DynamicObject hash) {
            assert HashOperations.verifyStore(getContext(), hash);

            final Entry first = Layouts.HASH.getFirstInSequence(hash);
            assert first.getPreviousInSequence() == null;

            final Object key = first.getKey();
            final Object value = first.getValue();

            Layouts.HASH.setFirstInSequence(hash, first.getNextInSequence());

            if (first.getNextInSequence() != null) {
                first.getNextInSequence().setPreviousInSequence(null);
                Layouts.HASH.setFirstInSequence(hash, first.getNextInSequence());
            }

            if (Layouts.HASH.getLastInSequence(hash) == first) {
                Layouts.HASH.setLastInSequence(hash, null);
            }

            /*
             * TODO CS 7-Mar-15 this isn't great - we need to remove from the lookup sequence for
             * which we need the previous entry in the bucket. However we normally get that from the
             * search result, and we haven't done a search here - we've just taken the first result.
             * For the moment we'll just do a manual search.
             */

            final Entry[] store = (Entry[]) Layouts.HASH.getStore(hash);

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

            Layouts.HASH.setSize(hash, Layouts.HASH.getSize(hash) - 1);

            assert HashOperations.verifyStore(getContext(), hash);

            Object[] objects = new Object[]{key, value};
            return createArray(objects, objects.length);
        }

    }

    @CoreMethod(names = {"size", "length"})
    @ImportStatic(HashGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNullHash(hash)")
        public int sizeNull(DynamicObject hash) {
            return 0;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public int sizePackedArray(DynamicObject hash) {
            return Layouts.HASH.getSize(hash);
        }

    }

    @ImportStatic(HashGuards.class)
    @NodeChild("hash")
    public abstract static class InternalRehashNode extends RubyNode {

        @Child private HashNode hashNode = new HashNode();

        public static InternalRehashNode create() {
            return InternalRehashNodeGen.create(null);
        }

        public abstract DynamicObject executeRehash(VirtualFrame frame, DynamicObject hash);

        @Specialization(guards = "isNullHash(hash)")
        DynamicObject rehashNull(DynamicObject hash) {
            return hash;
        }

        @Specialization(guards = "isPackedHash(hash)")
        DynamicObject rehashPackedArray(VirtualFrame frame, DynamicObject hash,
                @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile) {
            assert HashOperations.verifyStore(getContext(), hash);

            final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));
            final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
            final int size = Layouts.HASH.getSize(hash);

            for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
                if (n < size) {
                    PackedArrayStrategy.setHashed(store, n, hashNode.hash(frame, PackedArrayStrategy.getKey(store, n), compareByIdentity));
                }
            }

            assert HashOperations.verifyStore(getContext(), hash);

            return hash;
        }

        @Specialization(guards = "isBucketHash(hash)")
        DynamicObject rehashBuckets(VirtualFrame frame, DynamicObject hash,
                @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile) {
            assert HashOperations.verifyStore(getContext(), hash);

            final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));
            final Entry[] entries = (Entry[]) Layouts.HASH.getStore(hash);
            Arrays.fill(entries, null);

            Entry entry = Layouts.HASH.getFirstInSequence(hash);

            while (entry != null) {
                final int newHash = hashNode.hash(frame, entry.getKey(), compareByIdentity);
                entry.setHashed(newHash);
                entry.setNextInLookup(null);
                final int index = BucketsStrategy.getBucketIndex(entry.getHashed(), entries.length);
                Entry bucketEntry = entries[index];

                if (bucketEntry == null) {
                    entries[index] = entry;
                } else {
                    while (bucketEntry.getNextInLookup() != null) {
                        bucketEntry = bucketEntry.getNextInLookup();
                    }

                    bucketEntry.setNextInLookup(entry);
                }

                entry = entry.getNextInSequence();
            }

            assert HashOperations.verifyStore(getContext(), hash);
            return hash;
        }

    }

    @CoreMethod(names = "rehash", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class RehashNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isCompareByIdentity(hash)")
        public DynamicObject rehashIdentity(DynamicObject hash) {
            // the identity hash of objects never change.
            return hash;
        }

        @Specialization(guards = "!isCompareByIdentity(hash)")
        public DynamicObject rehashNotIdentity(VirtualFrame frame, DynamicObject hash,
                @Cached("create()") InternalRehashNode internalRehashNode) {
            return internalRehashNode.executeRehash(frame, hash);
        }

    }

    @NonStandard
    @CoreMethod(names = "internal_default_value")
    public abstract static class InternalDefaultValueNode extends CoreMethodArrayArgumentsNode {

        @Child private DefaultValueNode defaultValueNode = DefaultValueNodeFactory.create(null);

        @Specialization
        public Object internalDefaultValue(DynamicObject hash) {
            return defaultValueNode.executeDefaultValue(hash);
        }

    }

    @NonStandard
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class DefaultValueNode extends CoreMethodNode {

        public abstract Object executeDefaultValue(DynamicObject hash);

        @Specialization
        public Object defaultValue(DynamicObject hash,
                        @Cached("createBinaryProfile()") ConditionProfile nullValueProfile) {
            final Object value = Layouts.HASH.getDefaultValue(hash);

            if (nullValueProfile.profile(value == null)) {
                return nil();
            } else {
                return value;
            }
        }
    }

    @NonStandard
    @NodeChildren({
                    @NodeChild(type = RubyNode.class, value = "self"),
                    @NodeChild(type = RubyNode.class, value = "defaultValue")
    })
    public abstract static class SetDefaultValueNode extends CoreMethodNode {

        @Specialization
        public Object setDefaultValue(DynamicObject hash, Object defaultValue) {
            Layouts.HASH.setDefaultValue(hash, defaultValue);
            return defaultValue;
        }

    }

    @NonStandard
    @NodeChildren({
                    @NodeChild(type = RubyNode.class, value = "self"),
                    @NodeChild(type = RubyNode.class, value = "defaultProc")
    })
    public abstract static class SetDefaultProcNode extends CoreMethodNode {

        @Specialization(guards = "isRubyProc(defaultProc)")
        public DynamicObject setDefaultProc(DynamicObject hash, DynamicObject defaultProc) {
            Layouts.HASH.setDefaultValue(hash, null);
            Layouts.HASH.setDefaultBlock(hash, defaultProc);
            return defaultProc;
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject setDefaultProc(DynamicObject hash, Object nil) {
            Layouts.HASH.setDefaultValue(hash, null);
            Layouts.HASH.setDefaultBlock(hash, null);
            return nil();
        }

    }

}
