/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.*;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.Collection;

@CoreClass(name = "Hash")
public abstract class HashNodes {

    @CoreMethod(names = "[]", onSingleton = true, argumentsAsArray = true, reallyDoesNeedSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ConstructNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode;

        public ConstructNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
        }

        @ExplodeLoop
        @Specialization(guards = "isSmallArrayOfPairs(args)")
        public Object construct(VirtualFrame frame, RubyClass hashClass, Object[] args) {
            final RubyArray array = (RubyArray) args[0];

            final Object[] store = (Object[]) array.getStore();

            final int size = array.getSize();
            final Object[] newStore = PackedArrayStrategy.createStore();

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
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

                    final Object key = pairStore[0];
                    final Object value = pairStore[1];

                    final int hashed = hashNode.hash(frame, key);

                    PackedArrayStrategy.setHashedKeyValue(newStore, n, hashed, key, value);
                }
            }

            return new RubyHash(hashClass, null, null, newStore, size, null);
        }

        @Specialization
        public Object constructFallback(VirtualFrame frame, RubyClass hashClass, Object[] args) {
            return ruby(frame, "_constructor_fallback(*args)", "args", RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), args));
        }

        public static boolean isSmallArrayOfPairs(Object[] args) {
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

            if (store.length > PackedArrayStrategy.MAX_ENTRIES) {
                return false;
            }

            return true;
        }

    }

    @CoreMethod(names = "[]", required = 1)
    @ImportStatic(HashGuards.class)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode;
        @Child private CallDispatchHeadNode eqlNode;
        @Child private BasicObjectNodes.ReferenceEqualNode equalNode;
        @Child private CallDispatchHeadNode callDefaultNode;
        @Child private LookupEntryNode lookupEntryNode;

        private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile notInHashProfile = BranchProfile.create();
        private final BranchProfile useDefaultProfile = BranchProfile.create();
        
        @CompilerDirectives.CompilationFinal private Object undefinedValue;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
            callDefaultNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupEntryNode = new LookupEntryNode(context, sourceSection);
        }

        public abstract Object executeGet(VirtualFrame frame, RubyHash hash, Object key);

        @Specialization(guards = "isNullStorage(hash)")
        public Object getNull(VirtualFrame frame, RubyHash hash, Object key) {
            hashNode.hash(frame, key);

            if (undefinedValue != null) {
                return undefinedValue;
            } else {
                return callDefaultNode.call(frame, hash, "default", null, key);
            }
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedArrayStorage(hash)")
        public Object getPackedArray(VirtualFrame frame, RubyHash hash, Object key) {
            final int hashed = hashNode.hash(frame, key);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                        final boolean equal;

                        if (byIdentityProfile.profile(hash.isCompareByIdentity())) {
                            equal = equalNode.executeReferenceEqual(frame, key, PackedArrayStrategy.getKey(store, n));
                        } else {
                            equal = eqlNode.callBoolean(frame, key, "eql?", null, PackedArrayStrategy.getKey(store, n));
                        }

                        if (equal) {
                            return PackedArrayStrategy.getValue(store, n);
                        }
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

        @Specialization(guards = "isBucketsStorage(hash)")
        public Object getBuckets(VirtualFrame frame, RubyHash hash, Object key) {
            final HashLookupResult hashLookupResult = lookupEntryNode.lookup(frame, hash, key);

            if (hashLookupResult.getEntry() != null) {
                return hashLookupResult.getEntry().getValue();
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
    public abstract static class GetOrUndefinedNode extends CoreMethodArrayArgumentsNode {

        @Child private GetIndexNode getIndexNode;
        
        public GetOrUndefinedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getIndexNode = HashNodesFactory.GetIndexNodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            getIndexNode.setUndefinedValue(context.getCoreLibrary().getRubiniusUndefined());
        }

        @Specialization
        public Object getOrUndefined(VirtualFrame frame, RubyHash hash, Object key) {
            return getIndexNode.executeGet(frame, hash, key);
        }

    }

    @CoreMethod(names = "[]=", required = 2, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class SetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode;
        @Child private CallDispatchHeadNode eqlNode;
        @Child private BasicObjectNodes.ReferenceEqualNode equalNode;
        @Child private LookupEntryNode lookupEntryNode;

        private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

        private final BranchProfile extendProfile = BranchProfile.create();
        private final ConditionProfile strategyProfile = ConditionProfile.createBinaryProfile();

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
        }

        @Specialization(guards = { "isNullStorage(hash)", "!isRubyString(key)" })
        public Object setNull(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            hash.setStore(PackedArrayStrategy.createStore(hashNode.hash(frame, key), key, value), 1, null, null);
            assert HashOperations.verifyStore(hash);
            return value;
        }

        @Specialization(guards = "isNullStorage(hash)")
        public Object setNull(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            if (hash.isCompareByIdentity()) {
                return setNull(frame, hash, (Object) key, value);
            } else {
                return setNull(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

        @ExplodeLoop
        @Specialization(guards = {"isPackedArrayStorage(hash)", "!isRubyString(key)"})
        public Object setPackedArray(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            assert HashOperations.verifyStore(hash);

            final int hashed = hashNode.hash(frame, key);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                        final boolean equal;

                        if (byIdentityProfile.profile(hash.isCompareByIdentity())) {
                            equal = equalNode.executeReferenceEqual(frame, key, PackedArrayStrategy.getKey(store, n));
                        } else {
                            equal = eqlNode.callBoolean(frame, key, "eql?", null, PackedArrayStrategy.getKey(store, n));
                        }

                        if (equal) {
                            PackedArrayStrategy.setValue(store, n, value);
                            assert HashOperations.verifyStore(hash);
                            return value;
                        }
                    }
                }
            }

            extendProfile.enter();

            if (strategyProfile.profile(size + 1 <= PackedArrayStrategy.MAX_ENTRIES)) {
                PackedArrayStrategy.setHashedKeyValue(store, size, hashed, key, value);
                hash.setSize(size + 1);
                return value;
            } else {
                PackedArrayStrategy.promoteToBuckets(hash, store, size);
                BucketsStrategy.addNewEntry(hash, hashed, key, value);
            }

            assert HashOperations.verifyStore(hash);

            return value;
        }

        @Specialization(guards = "isPackedArrayStorage(hash)")
        public Object setPackedArray(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            if (hash.isCompareByIdentity()) {
                return setPackedArray(frame, hash, (Object) key, value);
            } else {
                return setPackedArray(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

        // Can't be @Cached yet as we call from the RubyString specialisation
        private final ConditionProfile foundProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile bucketCollisionProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile appendingProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile resizeProfile = ConditionProfile.createBinaryProfile();

        @Specialization(guards = {"isBucketsStorage(hash)", "!isRubyString(key)"})
        public Object setBuckets(VirtualFrame frame, RubyHash hash, Object key, Object value) {
            assert HashOperations.verifyStore(hash);

            if (lookupEntryNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookupEntryNode = insert(new LookupEntryNode(getContext(), getEncapsulatingSourceSection()));
            }

            final HashLookupResult result = lookupEntryNode.lookup(frame, hash, key);

            final Entry entry = result.getEntry();

            if (foundProfile.profile(entry == null)) {
                final Entry[] entries = (Entry[]) hash.getStore();

                final Entry newEntry = new Entry(result.getHashed(), key, value);

                if (bucketCollisionProfile.profile(result.getPreviousEntry() == null)) {
                    entries[result.getIndex()] = newEntry;
                } else {
                    result.getPreviousEntry().setNextInLookup(newEntry);
                }

                final Entry lastInSequence = hash.getLastInSequence();

                if (appendingProfile.profile(lastInSequence == null)) {
                    hash.setFirstInSequence(newEntry);
                } else {
                    lastInSequence.setNextInSequence(newEntry);
                    newEntry.setPreviousInSequence(lastInSequence);
                }

                hash.setLastInSequence(newEntry);

                final int newSize = hash.getSize() + 1;

                hash.setSize(newSize);

                // TODO CS 11-May-15 could store the next size for resize instead of doing a float operation each time

                if (resizeProfile.profile(newSize / (double) entries.length > BucketsStrategy.MAX_LOAD_BALANCE)) {
                    BucketsStrategy.resize(hash);
                }
            } else {
                entry.setKeyValue(result.getHashed(), key, value);
            }

            assert HashOperations.verifyStore(hash);

            return value;
        }

        @Specialization(guards = "isBucketsStorage(hash)")
        public Object setBuckets(VirtualFrame frame, RubyHash hash, RubyString key, Object value) {
            if (hash.isCompareByIdentity()) {
                return setBuckets(frame, hash, key, value);
            } else {
                return setBuckets(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value);
            }
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public RubyHash emptyNull(RubyHash hash) {
            return hash;
        }

        @Specialization(guards = "!isNullStorage(hash)")
        public RubyHash empty(RubyHash hash) {
            assert HashOperations.verifyStore(hash);
            hash.setStore(null, 0, null, null);
            assert HashOperations.verifyStore(hash);
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity", raiseIfFrozenSelf = true)
    public abstract static class CompareByIdentityNode extends CoreMethodArrayArgumentsNode {

        public CompareByIdentityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyHash compareByIdentity(RubyHash hash) {
            hash.setCompareByIdentity(true);
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity?")
    public abstract static class IsCompareByIdentityNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();
        
        public IsCompareByIdentityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean compareByIdentity(RubyHash hash) {
            return profile.profile(hash.isCompareByIdentity());
        }

    }

    @CoreMethod(names = "default_proc")
    public abstract static class DefaultProcNode extends CoreMethodArrayArgumentsNode {

        public DefaultProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object defaultProc(RubyHash hash) {
            if (hash.getDefaultBlock() == null) {
                return nil();
            } else {
                return hash.getDefaultBlock();
            }
        }

    }

    @CoreMethod(names = "delete", required = 1, needsBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class DeleteNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode;
        @Child private CallDispatchHeadNode eqlNode;
        @Child private LookupEntryNode lookupEntryNode;
        @Child private YieldDispatchHeadNode yieldNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            lookupEntryNode = new LookupEntryNode(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public Object deleteNull(VirtualFrame frame, RubyHash hash, Object key, Object block) {
            assert HashOperations.verifyStore(hash);

            if (block == UndefinedPlaceholder.INSTANCE) {
                return nil();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = {"isPackedArrayStorage(hash)", "!isCompareByIdentity(hash)"})
        public Object deletePackedArray(VirtualFrame frame, RubyHash hash, Object key, Object block) {
            assert HashOperations.verifyStore(hash);

            final int hashed = hashNode.hash(frame, key);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                        if (eqlNode.callBoolean(frame, PackedArrayStrategy.getKey(store, n), "eql?", null, key)) {
                            final Object value = PackedArrayStrategy.getValue(store, n);
                            PackedArrayStrategy.removeEntry(store, n);
                            hash.setSize(size - 1);
                            assert HashOperations.verifyStore(hash);
                            return value;
                        }
                    }
                }
            }

            assert HashOperations.verifyStore(hash);

            if (block == UndefinedPlaceholder.INSTANCE) {
                return nil();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = "isBucketsStorage(hash)")
        public Object delete(VirtualFrame frame, RubyHash hash, Object key, Object block) {
            assert HashOperations.verifyStore(hash);

            final HashLookupResult hashLookupResult = lookupEntryNode.lookup(frame, hash, key);

            if (hashLookupResult.getEntry() == null) {
                if (block == UndefinedPlaceholder.INSTANCE) {
                    return nil();
                } else {
                    return yieldNode.dispatch(frame, (RubyProc) block, key);
                }
            }

            final Entry entry = hashLookupResult.getEntry();

            // Remove from the sequence chain

            if (entry.getPreviousInSequence() == null) {
                assert hash.getFirstInSequence() == entry;
                hash.setFirstInSequence(entry.getNextInSequence());
            } else {
                assert hash.getFirstInSequence() != entry;
                entry.getPreviousInSequence().setNextInSequence(entry.getNextInSequence());
            }

            if (entry.getNextInSequence() == null) {
                hash.setLastInSequence(entry.getPreviousInSequence());
            } else {
                entry.getNextInSequence().setPreviousInSequence(entry.getPreviousInSequence());
            }

            // Remove from the lookup chain

            if (hashLookupResult.getPreviousEntry() == null) {
                ((Entry[]) hash.getStore())[hashLookupResult.getIndex()] = entry.getNextInLookup();
            } else {
                hashLookupResult.getPreviousEntry().setNextInLookup(entry.getNextInLookup());
            }

            hash.setSize(hash.getSize() - 1);

            assert HashOperations.verifyStore(hash);

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

        @Specialization(guards = "isNullStorage(hash)")
        public RubyHash eachNull(RubyHash hash, RubyProc block) {
            return hash;
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedArrayStorage(hash)")
        public RubyHash eachPackedArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            assert HashOperations.verifyStore(hash);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            int count = 0;

            try {
                for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    if (n < size) {
                        yield(frame, block, new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{PackedArrayStrategy.getKey(store, n), PackedArrayStrategy.getValue(store, n)}, 2));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return hash;
        }

        @Specialization(guards = "isBucketsStorage(hash)")
        public RubyHash eachBuckets(VirtualFrame frame, RubyHash hash, RubyProc block) {
            assert HashOperations.verifyStore(hash);

            for (KeyValue keyValue : verySlowToKeyValues(hash)) {
                yield(frame, block, new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{keyValue.getKey(), keyValue.getValue()}, 2));
            }

            return hash;
        }

        @CompilerDirectives.TruffleBoundary
        private Collection<KeyValue> verySlowToKeyValues(RubyHash hash) {
            return HashOperations.verySlowToKeyValues(hash);
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyHash hash, UndefinedPlaceholder block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            return toEnumNode.call(frame, hash, "to_enum", null, getContext().getSymbolTable().getSymbol(method.getName()));
        }

    }

    @CoreMethod(names = "empty?")
    @ImportStatic(HashGuards.class)
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public boolean emptyNull(RubyHash hash) {
            return true;
        }

        @Specialization(guards = "!isNullStorage(hash)")
        public boolean emptyPackedArray(RubyHash hash) {
            return hash.getSize() == 0;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 1, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

    @CoreMethod(names = {"initialize_copy", "replace"}, required = 1, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(from)")
        public RubyHash dupNull(RubyHash self, RubyHash from) {
            if (self == from) {
                return self;
            }

            self.setStore(null, 0, null, null);
            copyOther(self, from);

            return self;
        }

        @Specialization(guards = "isPackedArrayStorage(from)")
        public RubyHash dupPackedArray(RubyHash self, RubyHash from) {
            if (self == from) {
                return self;
            }

            final Object[] store = (Object[]) from.getStore();
            self.setStore(PackedArrayStrategy.copyStore(store), from.getSize(), null, null);

            copyOther(self, from);

            assert HashOperations.verifyStore(self);

            return self;
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isBucketsStorage(from)")
        public RubyHash dupBuckets(RubyHash self, RubyHash from) {
            if (self == from) {
                return self;
            }

            HashOperations.verySlowSetKeyValues(self, HashOperations.verySlowToKeyValues(from), from.isCompareByIdentity());

            copyOther(self, from);

            assert HashOperations.verifyStore(self);

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

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public RubyArray mapNull(VirtualFrame frame, RubyHash hash, RubyProc block) {
            assert HashOperations.verifyStore(hash);

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedArrayStorage(hash)")
        public RubyArray mapPackedArray(VirtualFrame frame, RubyHash hash, RubyProc block) {
            assert HashOperations.verifyStore(hash);

            final Object[] store = (Object[]) hash.getStore();
            final int size = hash.getSize();

            final Object[] result = new Object[size];

            int count = 0;

            try {
                for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                    if (n < size) {
                        final Object key = PackedArrayStrategy.getKey(store, n);
                        final Object value = PackedArrayStrategy.getValue(store, n);
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

        @Specialization(guards = "isBucketsStorage(hash)")
        public RubyArray mapBuckets(VirtualFrame frame, RubyHash hash, RubyProc block) {
            CompilerDirectives.transferToInterpreter();

            assert HashOperations.verifyStore(hash);

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

        public MergeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        @Specialization(guards = {"isPackedArrayStorage(hash)", "isNullStorage(other)", "!isCompareByIdentity(hash)"})
        public RubyHash mergePackedArrayNull(RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            final Object[] store = (Object[]) hash.getStore();
            final Object[] copy = PackedArrayStrategy.copyStore(store);
            return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), copy, hash.getSize(), null);
        }

        @ExplodeLoop
        @Specialization(guards = {"isPackedArrayStorage(hash)", "isPackedArrayStorage(other)", "!isCompareByIdentity(hash)"})
        public RubyHash mergePackedArrayPackedArray(VirtualFrame frame, RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            // TODO(CS): what happens with the default block here? Which side does it get merged from?
            assert HashOperations.verifyStore(hash);
            assert HashOperations.verifyStore(other);

            final Object[] storeA = (Object[]) hash.getStore();
            final int storeASize = hash.getSize();

            final Object[] storeB = (Object[]) other.getStore();
            final int storeBSize = other.getSize();

            final boolean[] mergeFromA = new boolean[storeASize];
            int mergeFromACount = 0;

            int conflictsCount = 0;

            for (int a = 0; a < PackedArrayStrategy.MAX_ENTRIES; a++) {
                if (a < storeASize) {
                    boolean merge = true;

                    for (int b = 0; b < PackedArrayStrategy.MAX_ENTRIES; b++) {
                        if (b < storeBSize) {
                            if (eqlNode.callBoolean(frame, PackedArrayStrategy.getKey(storeA, a), "eql?", null, PackedArrayStrategy.getKey(storeB, b))) {
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
                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), PackedArrayStrategy.copyStore(storeB), storeBSize, null);
            }

            considerNothingFromSecondProfile.enter();

            if (conflictsCount == storeBSize) {
                nothingFromSecondProfile.enter();
                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), PackedArrayStrategy.copyStore(storeA), storeASize, null);
            }

            considerResultIsSmallProfile.enter();

            final int mergedSize = storeBSize + mergeFromACount;

            if (storeBSize + mergeFromACount <= PackedArrayStrategy.MAX_ENTRIES) {
                resultIsSmallProfile.enter();

                final Object[] merged = PackedArrayStrategy.createStore();

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

                return new RubyHash(hash.getLogicalClass(), hash.getDefaultBlock(), hash.getDefaultValue(), merged, mergedSize, null);
            }

            CompilerDirectives.transferToInterpreter();

            return mergeBucketsBuckets(hash, other, block);
        }
        
        // TODO CS 3-Mar-15 need negative guards on this
        @Specialization(guards = "!isCompareByIdentity(hash)")
        public RubyHash mergeBucketsBuckets(RubyHash hash, RubyHash other, UndefinedPlaceholder block) {
            CompilerDirectives.transferToInterpreter();

            final RubyHash merged = new RubyHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(hash.getSize() + other.getSize())], 0, null);

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

            assert HashOperations.verifyStore(hash);

            return merged;
        }

        @Specialization(guards = "!isCompareByIdentity(hash)")
        public RubyHash merge(VirtualFrame frame, RubyHash hash, RubyHash other, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            
            final RubyHash merged = new RubyHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(hash.getSize() + other.getSize())], 0, null);

            int size = 0;

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(hash)) {
                HashOperations.verySlowSetInBuckets(merged, keyValue.getKey(), keyValue.getValue(), false);
                size++;
            }

            for (KeyValue keyValue : HashOperations.verySlowToKeyValues(other)) {
                final HashLookupResult searchResult = HashOperations.verySlowFindBucket(merged, keyValue.getKey(), false);
                
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

            assert HashOperations.verifyStore(hash);

            return merged;
        }

        @Specialization(guards = {"!isRubyHash(other)", "!isCompareByIdentity(hash)"})
        public Object merge(VirtualFrame frame, RubyHash hash, Object other, Object block) {
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
    public abstract static class SetDefaultNode extends CoreMethodArrayArgumentsNode {

        public SetDefaultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setDefault(VirtualFrame frame, RubyHash hash, Object defaultValue) {
            ruby(frame, "Rubinius.check_frozen");
            hash.setDefaultValue(defaultValue);
            hash.setDefaultBlock(null);
            return defaultValue;
        }
    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ShiftNode extends CoreMethodArrayArgumentsNode {

        public ShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isEmpty(hash)", "!hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public RubyBasicObject shiftEmpty(RubyHash hash) {
            return nil();
        }

        @Specialization(guards = {"isEmpty(hash)", "hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public Object shiftEmpyDefaultValue(RubyHash hash) {
            return hash.getDefaultValue();
        }

        @Specialization(guards = {"isEmpty(hash)", "!hasDefaultValue(hash)", "hasDefaultBlock(hash)"})
        public Object shiftEmptyDefaultProc(RubyHash hash) {
            return hash.getDefaultBlock().rootCall(hash, nil());
        }

        @Specialization(guards = {"!isEmpty(hash)", "isPackedArrayStorage(hash)"})
        public RubyArray shiftPackedArray(RubyHash hash) {
            assert HashOperations.verifyStore(hash);
            
            final Object[] store = (Object[]) hash.getStore();
            
            final Object key = PackedArrayStrategy.getKey(store, 0);
            final Object value = PackedArrayStrategy.getValue(store, 0);
            
            PackedArrayStrategy.removeEntry(store, 0);
            
            hash.setSize(hash.getSize() - 1);

            assert HashOperations.verifyStore(hash);
            
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

        @Specialization(guards = {"!isEmpty(hash)", "isBucketsStorage(hash)"})
        public RubyArray shiftBuckets(RubyHash hash) {
            assert HashOperations.verifyStore(hash);

            final Entry first = hash.getFirstInSequence();
            assert first.getPreviousInSequence() == null;

            final Object key = first.getKey();
            final Object value = first.getValue();
            
            hash.setFirstInSequence(first.getNextInSequence());

            if (first.getNextInSequence() != null) {
                first.getNextInSequence().setPreviousInSequence(null);
                hash.setFirstInSequence(first.getNextInSequence());
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

            assert HashOperations.verifyStore(hash);

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

    }
    
    @CoreMethod(names = {"size", "length"})
    @ImportStatic(HashGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public int sizeNull(RubyHash hash) {
            return 0;
        }

        @Specialization(guards = "!isNullStorage(hash)")
        public int sizePackedArray(RubyHash hash) {
            return hash.getSize();
        }

    }

    @CoreMethod(names = "rehash", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class RehashNode extends CoreMethodArrayArgumentsNode {

        public RehashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullStorage(hash)")
        public RubyHash rehashNull(RubyHash hash) {
            return hash;
        }

        @Specialization(guards = "isPackedArrayStorage(hash)")
        public RubyHash rehashPackedArray(RubyHash hash) {
            // Nothing to do as we weren't using the hash code anyway
            return hash;
        }

        @Specialization(guards = "isBucketsStorage(hash)")
        public RubyHash rehashBuckets(RubyHash hash) {
            CompilerDirectives.transferToInterpreter();

            assert HashOperations.verifyStore(hash);
            
            HashOperations.verySlowSetKeyValues(hash, HashOperations.verySlowToKeyValues(hash), hash.isCompareByIdentity());

            assert HashOperations.verifyStore(hash);
            
            return hash;
        }

    }

    @RubiniusOnly
    @NodeChild(type = RubyNode.class, value = "self")
    public abstract static class DefaultValueNode extends CoreMethodNode {

        public DefaultValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object defaultValue(RubyHash hash) {
            final Object value = hash.getDefaultValue();
            
            if (value == null) {
                return nil();
            } else {
                return value;
            }
        }
    }

    @RubiniusOnly
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "defaultValue")
    })
    public abstract static class SetDefaultValueNode extends CoreMethodNode {

        public SetDefaultValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setDefaultValue(RubyHash hash, Object defaultValue) {
            hash.setDefaultValue(defaultValue);
            return defaultValue;
        }
        
    }

    @RubiniusOnly
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "defaultProc")
    })
    public abstract static class SetDefaultProcNode extends CoreMethodNode {

        public SetDefaultProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyProc setDefaultProc(RubyHash hash, RubyProc defaultProc) {
            hash.setDefaultValue(null);
            hash.setDefaultBlock(defaultProc);
            return defaultProc;
        }

        @Specialization(guards = "isNil(nil)")
        public RubyBasicObject setDefaultProc(RubyHash hash, Object nil) {
            hash.setDefaultValue(null);
            hash.setDefaultBlock(null);
            return nil();
        }

    }

    public static class HashGuards {

        public static boolean isNullStorage(RubyHash hash) {
            return hash.getStore() == null;
        }

        public static boolean isPackedArrayStorage(RubyHash hash) {
            // Can't do instanceof Object[] due to covariance
            return !(isNullStorage(hash) || isBucketsStorage(hash));
        }

        public static boolean isBucketsStorage(RubyHash hash) {
            return hash.getStore() instanceof Entry[];
        }

        public static boolean isCompareByIdentity(RubyHash hash) {
            return hash.isCompareByIdentity();
        }

        public static boolean isEmpty(RubyHash hash) {
            return hash.getSize() == 0;
        }

        public static boolean hasDefaultValue(RubyHash hash) {
            return hash.getDefaultValue() != null;
        }

        public static boolean hasDefaultBlock(RubyHash hash) {
            return hash.getDefaultBlock() != null;
        }

    }

}
