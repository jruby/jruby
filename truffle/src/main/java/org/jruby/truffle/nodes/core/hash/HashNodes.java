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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayBuilderNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.BucketsStrategy;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashLookupResult;
import org.jruby.truffle.runtime.hash.PackedArrayStrategy;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

@CoreClass(name = "Hash")
public abstract class HashNodes {

    public static int slowHashKey(RubyContext context, Object key) {
        final Object hashValue = DebugOperations.send(context, key, "hash", null);

        if (hashValue instanceof Integer) {
            return (int) hashValue;
        } else if (hashValue instanceof Long) {
            return (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static boolean slowAreKeysEqual(RubyContext context, Object a, Object b, boolean byIdentity) {
        final String method;

        if (byIdentity) {
            method = "equal?";
        } else {
            method = "eql?";
        }

        final Object equalityResult = DebugOperations.send(context, a, method, null, b);

        if (equalityResult instanceof Boolean) {
            return (boolean) equalityResult;
        }

        throw new UnsupportedOperationException();
    }

    public static boolean verifyStore(RubyBasicObject hash) {
        return verifyStore(getStore(hash), getSize(hash), getFirstInSequence(hash), getLastInSequence(hash));
    }

    public static boolean verifyStore(Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        assert store == null || store instanceof Object[] || store instanceof Entry[];

        if (store == null) {
            assert size == 0;
            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        if (store instanceof Entry[]) {
            assert lastInSequence == null || lastInSequence.getNextInSequence() == null;

            final Entry[] entryStore = (Entry[]) store;

            Entry foundFirst = null;
            Entry foundLast = null;
            int foundSizeBuckets = 0;

            for (int n = 0; n < entryStore.length; n++) {
                Entry entry = entryStore[n];

                while (entry != null) {
                    foundSizeBuckets++;

                    if (entry == firstInSequence) {
                        assert foundFirst == null;
                        foundFirst = entry;
                    }

                    if (entry == lastInSequence) {
                        assert foundLast == null;
                        foundLast = entry;
                    }

                    entry = entry.getNextInLookup();
                }
            }

            assert foundSizeBuckets == size;
            assert firstInSequence == foundFirst;
            assert lastInSequence == foundLast;

            int foundSizeSequence = 0;
            Entry entry = firstInSequence;

            while (entry != null) {
                foundSizeSequence++;

                if (entry.getNextInSequence() == null) {
                    assert entry == lastInSequence;
                } else {
                    assert entry.getNextInSequence().getPreviousInSequence() == entry;
                }

                entry = entry.getNextInSequence();

                assert entry != firstInSequence;
            }

            assert foundSizeSequence == size : String.format("%d %d", foundSizeSequence, size);
        } else if (store instanceof Object[]) {
            assert ((Object[]) store).length == PackedArrayStrategy.MAX_ENTRIES * PackedArrayStrategy.ELEMENTS_PER_ENTRY : ((Object[]) store).length;

            final Object[] packedStore = (Object[]) store;

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    assert packedStore[n * 2] != null;
                    assert packedStore[n * 2 + 1] != null;
                }
            }

            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        return true;
    }

    public static class HashType extends BasicObjectType {

    }

    public static final HashType HASH_TYPE = new HashType();

    private static final DynamicObjectFactory HASH_FACTORY;

    static {
        final Shape shape = RubyBasicObject.LAYOUT.createShape(HASH_TYPE);
        HASH_FACTORY = shape.createFactory();
    }

    public static RubyProc getDefaultBlock(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).defaultBlock;
    }

    public static void setDefaultBlock(RubyBasicObject hash, RubyProc defaultBlock) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).defaultBlock = defaultBlock;
    }

    public static Object getDefaultValue(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).defaultValue;
    }

    public static void setDefaultValue(RubyBasicObject hash, Object defaultValue) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).defaultValue = defaultValue;
    }

    public static boolean isCompareByIdentity(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).compareByIdentity;
    }

    public static void setCompareByIdentity(RubyBasicObject hash, boolean compareByIdentity) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).compareByIdentity = compareByIdentity;
    }

    public static Object getStore(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).store;
    }

    public static void setStore(RubyBasicObject hash, Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        assert RubyGuards.isRubyHash(hash);
        assert verifyStore(store, size, firstInSequence, lastInSequence);
        ((RubyHash) hash).store = store;
        ((RubyHash) hash).size = size;
        ((RubyHash) hash).firstInSequence = firstInSequence;
        ((RubyHash) hash).lastInSequence = lastInSequence;
    }

    public static int getSize(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).size;
    }

    public static void setSize(RubyBasicObject hash, int storeSize) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).size = storeSize;
    }

    public static Entry getFirstInSequence(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).firstInSequence;
    }

    public static void setFirstInSequence(RubyBasicObject hash, Entry firstInSequence) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).firstInSequence = firstInSequence;
    }

    public static Entry getLastInSequence(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);
        return ((RubyHash) hash).lastInSequence;
    }

    public static void setLastInSequence(RubyBasicObject hash, Entry lastInSequence) {
        assert RubyGuards.isRubyHash(hash);
        ((RubyHash) hash).lastInSequence = lastInSequence;
    }

    public static RubyBasicObject createEmptyHash(RubyClass hashClass) {
        return createHash(hashClass, null, null, null, 0, null, null);
    }

    public static RubyBasicObject createHash(RubyClass hashClass, Object[] store, int size) {
        return createHash(hashClass, null, null, store, size, null, null);
    }

    public static RubyBasicObject createHash(RubyClass hashClass, RubyProc defaultBlock, Object defaultValue, Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        return new RubyHash(hashClass, defaultBlock, defaultValue, store, size, firstInSequence, lastInSequence, HASH_FACTORY.newInstance());
    }

    @TruffleBoundary
    public static Iterator<Map.Entry<Object, Object>> iterateKeyValues(RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);

        if (HashGuards.isNullHash(hash)) {
            return Collections.emptyIterator();
        } if (HashGuards.isPackedHash(hash)) {
            return PackedArrayStrategy.iterateKeyValues((Object[]) getStore(hash), getSize(hash));
        } else if (HashGuards.isBucketHash(hash)) {
            return BucketsStrategy.iterateKeyValues(getFirstInSequence(hash));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @TruffleBoundary
    public static Iterable<Map.Entry<Object, Object>> iterableKeyValues(final RubyBasicObject hash) {
        assert RubyGuards.isRubyHash(hash);

        return new Iterable<Map.Entry<Object, Object>>() {

            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                return iterateKeyValues(hash);
            }

        };
    }

    @CoreMethod(names = "[]", constructor = true, argumentsAsArray = true)
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

            final Object[] store = (Object[]) ArrayNodes.getStore(array);

            final int size = ArrayNodes.getSize(array);
            final Object[] newStore = PackedArrayStrategy.createStore();

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    final Object pair = store[n];

                    if (!RubyGuards.isRubyArray(pair)) {
                        return constructFallback(frame, hashClass, args);
                    }

                    final RubyBasicObject pairArray = (RubyBasicObject) pair;

                    if (!(ArrayNodes.getStore(pairArray) instanceof Object[])) {
                        return constructFallback(frame, hashClass, args);
                    }

                    final Object[] pairStore = (Object[]) ArrayNodes.getStore(pairArray);

                    final Object key = pairStore[0];
                    final Object value = pairStore[1];

                    final int hashed = hashNode.hash(frame, key);

                    PackedArrayStrategy.setHashedKeyValue(newStore, n, hashed, key, value);
                }
            }

            return createHash(hashClass, newStore, size);
        }

        @Specialization
        public Object constructFallback(VirtualFrame frame, RubyClass hashClass, Object[] args) {
            return ruby(frame, "_constructor_fallback(*args)", "args", ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), args));
        }

        public static boolean isSmallArrayOfPairs(Object[] args) {
            if (args.length != 1) {
                return false;
            }

            final Object arg = args[0];

            if (!RubyGuards.isRubyArray(arg)) {
                return false;
            }

            final RubyBasicObject array = (RubyBasicObject) arg;

            if (!(ArrayNodes.getStore(array) instanceof Object[])) {
                return false;
            }

            final Object[] store = (Object[]) ArrayNodes.getStore(array);

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
        
        @CompilationFinal private Object undefinedValue;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context);
            equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
            callDefaultNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupEntryNode = new LookupEntryNode(context, sourceSection);
        }

        public abstract Object executeGet(VirtualFrame frame, RubyBasicObject hash, Object key);

        @Specialization(guards = "isNullHash(hash)")
        public Object getNull(VirtualFrame frame, RubyBasicObject hash, Object key) {
            hashNode.hash(frame, key);

            if (undefinedValue != null) {
                return undefinedValue;
            } else {
                return callDefaultNode.call(frame, hash, "default", null, key);
            }
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)")
        public Object getPackedArray(VirtualFrame frame, RubyBasicObject hash, Object key) {
            final int hashed = hashNode.hash(frame, key);

            final Object[] store = (Object[]) getStore(hash);
            final int size = getSize(hash);

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                        final boolean equal;

                        if (byIdentityProfile.profile(isCompareByIdentity(hash))) {
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

        @Specialization(guards = "isBucketHash(hash)")
        public Object getBuckets(VirtualFrame frame, RubyBasicObject hash, Object key) {
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
        public Object getOrUndefined(VirtualFrame frame, RubyBasicObject hash, Object key) {
            return getIndexNode.executeGet(frame, hash, key);
        }

    }

    @CoreMethod(names = "[]=", required = 2, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class SetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private SetNode setNode;

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setNull(VirtualFrame frame, RubyBasicObject hash, Object key, Object value) {
            if (setNode == null) {
                CompilerDirectives.transferToInterpreter();
                setNode = insert(SetNodeGen.create(getContext(), getEncapsulatingSourceSection(), null, null, null, null));
            }

            return setNode.executeSet(frame, hash, key, value, isCompareByIdentity(hash));
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullHash(hash)")
        public RubyBasicObject emptyNull(RubyBasicObject hash) {
            return hash;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public RubyBasicObject empty(RubyBasicObject hash) {
            assert verifyStore(hash);
            setStore(hash, null, 0, null, null);
            assert verifyStore(hash);
            return hash;
        }

    }

    @CoreMethod(names = "compare_by_identity", raiseIfFrozenSelf = true)
    public abstract static class CompareByIdentityNode extends CoreMethodArrayArgumentsNode {

        public CompareByIdentityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject compareByIdentity(RubyBasicObject hash) {
            setCompareByIdentity(hash, true);
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
        public boolean compareByIdentity(RubyBasicObject hash) {
            return profile.profile(isCompareByIdentity(hash));
        }

    }

    @CoreMethod(names = "default_proc")
    public abstract static class DefaultProcNode extends CoreMethodArrayArgumentsNode {

        public DefaultProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object defaultProc(RubyBasicObject hash) {
            if (getDefaultBlock(hash) == null) {
                return nil();
            } else {
                return getDefaultBlock(hash);
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
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupEntryNode = new LookupEntryNode(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization(guards = "isNullHash(hash)")
        public Object deleteNull(VirtualFrame frame, RubyBasicObject hash, Object key, Object block) {
            assert verifyStore(hash);

            if (block == NotProvided.INSTANCE) {
                return nil();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = {"isPackedHash(hash)", "!isCompareByIdentity(hash)"})
        public Object deletePackedArray(VirtualFrame frame, RubyBasicObject hash, Object key, Object block) {
            assert verifyStore(hash);

            final int hashed = hashNode.hash(frame, key);

            final Object[] store = (Object[]) getStore(hash);
            final int size = getSize(hash);

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                        if (eqlNode.callBoolean(frame, PackedArrayStrategy.getKey(store, n), "eql?", null, key)) {
                            final Object value = PackedArrayStrategy.getValue(store, n);
                            PackedArrayStrategy.removeEntry(store, n);
                            setSize(hash, size - 1);
                            assert verifyStore(hash);
                            return value;
                        }
                    }
                }
            }

            assert verifyStore(hash);

            if (block == NotProvided.INSTANCE) {
                return nil();
            } else {
                return yieldNode.dispatch(frame, (RubyProc) block, key);
            }
        }

        @Specialization(guards = "isBucketHash(hash)")
        public Object delete(VirtualFrame frame, RubyBasicObject hash, Object key, Object block) {
            assert verifyStore(hash);

            final HashLookupResult hashLookupResult = lookupEntryNode.lookup(frame, hash, key);

            if (hashLookupResult.getEntry() == null) {
                if (block == NotProvided.INSTANCE) {
                    return nil();
                } else {
                    return yieldNode.dispatch(frame, (RubyProc) block, key);
                }
            }

            final Entry entry = hashLookupResult.getEntry();

            // Remove from the sequence chain

            if (entry.getPreviousInSequence() == null) {
                assert getFirstInSequence(hash) == entry;
                setFirstInSequence(hash, entry.getNextInSequence());
            } else {
                assert getFirstInSequence(hash) != entry;
                entry.getPreviousInSequence().setNextInSequence(entry.getNextInSequence());
            }

            if (entry.getNextInSequence() == null) {
                setLastInSequence(hash, entry.getPreviousInSequence());
            } else {
                entry.getNextInSequence().setPreviousInSequence(entry.getPreviousInSequence());
            }

            // Remove from the lookup chain

            if (hashLookupResult.getPreviousEntry() == null) {
                ((Entry[]) getStore(hash))[hashLookupResult.getIndex()] = entry.getNextInLookup();
            } else {
                hashLookupResult.getPreviousEntry().setNextInLookup(entry.getNextInLookup());
            }

            setSize(hash, getSize(hash) - 1);

            assert verifyStore(hash);

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

        @Specialization(guards = "isNullHash(hash)")
        public RubyBasicObject eachNull(RubyBasicObject hash, RubyProc block) {
            return hash;
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)")
        public RubyBasicObject eachPackedArray(VirtualFrame frame, RubyBasicObject hash, RubyProc block) {
            assert verifyStore(hash);

            final Object[] store = (Object[]) getStore(hash);
            final int size = getSize(hash);

            int count = 0;

            try {
                for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    if (n < size) {
                        yield(frame, block, createArray(new Object[]{PackedArrayStrategy.getKey(store, n), PackedArrayStrategy.getValue(store, n)}, 2));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return hash;
        }

        @Specialization(guards = "isBucketHash(hash)")
        public RubyBasicObject eachBuckets(VirtualFrame frame, RubyBasicObject hash, RubyProc block) {
            assert verifyStore(hash);

            for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(getFirstInSequence(hash))) {
                yield(frame, block, createArray(new Object[]{keyValue.getKey(), keyValue.getValue()}, 2));
            }

            return hash;
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyBasicObject hash, NotProvided block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }

            InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            return toEnumNode.call(frame, hash, "to_enum", null, getSymbol(method.getName()));
        }

    }

    @CoreMethod(names = "empty?")
    @ImportStatic(HashGuards.class)
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullHash(hash)")
        public boolean emptyNull(RubyBasicObject hash) {
            return true;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public boolean emptyPackedArray(RubyBasicObject hash) {
            return getSize(hash) == 0;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 1, raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyBasicObject hash, NotProvided defaultValue, NotProvided block) {
            setStore(hash, null, 0, null, null);
            setDefaultValue(hash, null);
            setDefaultBlock(hash, null);
            return hash;
        }

        @Specialization
        public RubyBasicObject initialize(RubyBasicObject hash, NotProvided defaultValue, RubyProc block) {
            setStore(hash, null, 0, null, null);
            setDefaultValue(hash, null);
            setDefaultBlock(hash, block);
            return hash;
        }

        @Specialization(guards = "wasProvided(defaultValue)")
        public RubyBasicObject initialize(RubyBasicObject hash, Object defaultValue, NotProvided block) {
            setStore(hash, null, 0, null, null);
            setDefaultValue(hash, defaultValue);
            setDefaultBlock(hash, null);
            return hash;
        }

        @Specialization(guards = "wasProvided(defaultValue)")
        public Object initialize(RubyBasicObject hash, Object defaultValue, RubyProc block) {
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

        @Specialization(guards = {"isRubyHash(from)", "isNullHash(from)"})
        public RubyBasicObject replaceNull(RubyBasicObject self, RubyBasicObject from) {
            if (self == from) {
                return self;
            }

            setStore(self, null, 0, null, null);
            copyOtherFields(self, from);

            return self;
        }

        @Specialization(guards = {"isRubyHash(from)", "isPackedHash(from)"})
        public RubyBasicObject replacePackedArray(RubyBasicObject self, RubyBasicObject from) {
            if (self == from) {
                return self;
            }

            final Object[] store = (Object[]) getStore(from);
            setStore(self, PackedArrayStrategy.copyStore(store), getSize(from), null, null);

            copyOtherFields(self, from);

            assert verifyStore(self);

            return self;
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyHash(from)", "isBucketHash(from)"})
        public RubyBasicObject replaceBuckets(RubyBasicObject self, RubyBasicObject from) {
            if (self == from) {
                return self;
            }

            BucketsStrategy.copyInto(from, self);
            copyOtherFields(self, from);

            assert verifyStore(self);

            return self;
        }

        @Specialization(guards = "!isRubyHash(other)")
        public Object replaceBuckets(VirtualFrame frame, RubyBasicObject self, Object other) {
            return ruby(frame, "replace(Rubinius::Type.coerce_to other, Hash, :to_hash)", "other", other);
        }
        
        private void copyOtherFields(RubyBasicObject self, RubyBasicObject from) {
            setDefaultBlock(self, getDefaultBlock(from));
            setDefaultValue(self, getDefaultValue(from));
            setCompareByIdentity(self, isCompareByIdentity(from));
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(HashGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Child ArrayBuilderNode arrayBuilderNode;

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullHash(hash)")
        public RubyBasicObject mapNull(VirtualFrame frame, RubyBasicObject hash, RubyProc block) {
            assert verifyStore(hash);

            return createEmptyArray();
        }

        @ExplodeLoop
        @Specialization(guards = "isPackedHash(hash)")
        public RubyBasicObject mapPackedArray(VirtualFrame frame, RubyBasicObject hash, RubyProc block) {
            assert verifyStore(hash);

            if (arrayBuilderNode == null) {
                CompilerDirectives.transferToInterpreter();
                arrayBuilderNode = insert(new ArrayBuilderNode.UninitializedArrayBuilderNode(getContext()));
            }

            final Object[] store = (Object[]) getStore(hash);

            final int length = getSize(hash);
            Object resultStore = arrayBuilderNode.start(length);

            try {
                for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                    if (n < length) {
                        final Object key = PackedArrayStrategy.getKey(store, n);
                        final Object value = PackedArrayStrategy.getValue(store, n);
                        resultStore = arrayBuilderNode.append(resultStore, n, yield(frame, block, key, value));
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(length);
                }
            }

            return arrayBuilderNode.finishAndCreate(getContext().getCoreLibrary().getArrayClass(), resultStore, length);
        }

        @Specialization(guards = "isBucketHash(hash)")
        public RubyBasicObject mapBuckets(VirtualFrame frame, RubyBasicObject hash, RubyProc block) {
            assert verifyStore(hash);

            if (arrayBuilderNode == null) {
                CompilerDirectives.transferToInterpreter();
                arrayBuilderNode = insert(new ArrayBuilderNode.UninitializedArrayBuilderNode(getContext()));
            }

            final int length = getSize(hash);
            Object store = arrayBuilderNode.start(length);

            int index = 0;

            try {
                for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(getFirstInSequence(hash))) {
                    arrayBuilderNode.append(store, index, yield(frame, block, keyValue.getKey(), keyValue.getValue()));
                    index++;
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(length);
                }
            }

            return arrayBuilderNode.finishAndCreate(getContext().getCoreLibrary().getArrayClass(), store, length);
        }

    }

    @ImportStatic(HashGuards.class)
    @CoreMethod(names = "merge", required = 1, needsBlock = true)
    public abstract static class MergeNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode eqlNode;
        @Child private CallDispatchHeadNode fallbackCallNode;
        @Child private LookupEntryNode lookupEntryNode;
        @Child private SetNode setNode;

        private final BranchProfile nothingFromFirstProfile = BranchProfile.create();
        private final BranchProfile considerNothingFromSecondProfile = BranchProfile.create();
        private final BranchProfile nothingFromSecondProfile = BranchProfile.create();
        private final BranchProfile considerResultIsSmallProfile = BranchProfile.create();
        private final BranchProfile resultIsSmallProfile = BranchProfile.create();
        private final BranchProfile promoteProfile = BranchProfile.create();

        public MergeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eqlNode = DispatchHeadNodeFactory.createMethodCall(context);
            setNode = SetNodeGen.create(context, sourceSection, null, null, null, null);
        }

        // Merge with an empty hash, without a block

        @Specialization(guards = {
                "isNullHash(hash)",
                "isRubyHash(other)",
                "isNullHash(other)"
        })
        public RubyBasicObject mergeEmptyEmpty(RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), null, 0, null, null);
        }

        @Specialization(guards = {
                "isEmptyHash(hash)",
                "isRubyHash(other)",
                "isPackedHash(other)"
        })
        public RubyBasicObject mergeEmptyPacked(RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final Object[] store = (Object[]) getStore(other);
            final Object[] copy = PackedArrayStrategy.copyStore(store);
            return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), copy, getSize(hash), null, null);
        }

        @Specialization(guards = {
                "isPackedHash(hash)",
                "isRubyHash(other)",
                "isEmptyHash(other)"
        })
        public RubyBasicObject mergePackedEmpty(RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final Object[] store = (Object[]) getStore(hash);
            final Object[] copy = PackedArrayStrategy.copyStore(store);
            return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), copy, getSize(hash), null, null);
        }

        @Specialization(guards = {
                "isEmptyHash(hash)",
                "isRubyHash(other)",
                "isBucketHash(other)"
        })
        public RubyBasicObject mergeEmptyBuckets(RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final RubyBasicObject merged = createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), null, 0, null, null);
            BucketsStrategy.copyInto(other, merged);
            return merged;
        }

        @Specialization(guards = {
                "isBucketHash(hash)",
                "isRubyHash(other)",
                "isEmptyHash(other)"
        })
        public RubyBasicObject mergeBucketsEmpty(RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final RubyBasicObject merged = createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), null, 0, null, null);
            BucketsStrategy.copyInto(hash, merged);
            return merged;
        }

        // Merge non-empty packed with non-empty packed, without a block

        @ExplodeLoop
        @Specialization(guards = {
                "isPackedHash(hash)",
                "!isEmptyHash(hash)",
                "isRubyHash(other)",
                "isPackedHash(other)",
                "!isEmptyHash(other)",
                "!isCompareByIdentity(hash)"})
        public RubyBasicObject mergePackedPacked(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            assert verifyStore(hash);
            assert verifyStore(other);

            final Object[] storeA = (Object[]) getStore(hash);
            final int storeASize = getSize(hash);

            final Object[] storeB = (Object[]) getStore(other);
            final int storeBSize = getSize(other);

            // Go through and figure out what gets merged from each hash

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

            // If nothing comes from A, it's easy

            if (mergeFromACount == 0) {
                nothingFromFirstProfile.enter();
                return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), PackedArrayStrategy.copyStore(storeB), storeBSize, null, null);
            }

            // Cut off here

            considerNothingFromSecondProfile.enter();

            // If everything in B conflicted with something in A, it's easy

            if (conflictsCount == storeBSize) {
                nothingFromSecondProfile.enter();
                return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), PackedArrayStrategy.copyStore(storeA), storeASize, null, null);
            }

            // Cut off here

            considerResultIsSmallProfile.enter();

            // More complicated case where some things from each hash, but it still fits in a packed array

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

                return createHash(hash.getLogicalClass(), getDefaultBlock(hash), getDefaultValue(hash), merged, mergedSize, null, null);
            }

            // Most complicated cases where things from both hashes, and it also needs to be promoted to buckets

            promoteProfile.enter();

            final RubyBasicObject merged = createHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(mergedSize)], 0, null, null);

            for (int n = 0; n < storeASize; n++) {
                if (mergeFromA[n]) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(storeA, n),  PackedArrayStrategy.getValue(storeA, n), false);
                }
            }

            for (int n = 0; n < storeBSize; n++) {
                setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(storeB, n),  PackedArrayStrategy.getValue(storeB, n), false);
            }

            assert verifyStore(hash);

            return merged;
        }

        // Merge non-empty buckets with non-empty buckets, without a block

        @Specialization(guards = {
                "isBucketHash(hash)",
                "!isEmptyHash(hash)",
                "isRubyHash(other)",
                "isBucketHash(other)",
                "!isEmptyHash(other)"
        })
        public RubyBasicObject mergeBucketsBuckets(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final boolean isCompareByIdentity = isCompareByIdentity(hash);

            final RubyBasicObject merged = createHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(getSize(hash) + getSize(other))], 0, null, null);

            for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(HashNodes.getFirstInSequence(hash))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), isCompareByIdentity);
            }

            for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(HashNodes.getFirstInSequence(other))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), isCompareByIdentity);
            }

            assert verifyStore(hash);

            return merged;
        }

        // Merge combinations of packed and buckets, without a block

        @Specialization(guards = {
                "isPackedHash(hash)",
                "!isEmptyHash(hash)",
                "isRubyHash(other)",
                "isBucketHash(other)",
                "!isEmptyHash(other)"
        })
        public RubyBasicObject mergePackedBuckets(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final boolean isCompareByIdentity = isCompareByIdentity(hash);

            final RubyBasicObject merged = createHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(getSize(hash) + getSize(other))], 0, null, null);

            final Object[] hashStore = (Object[]) getStore(hash);
            final int hashSize = getSize(hash);

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < hashSize) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(hashStore, n), PackedArrayStrategy.getValue(hashStore, n), isCompareByIdentity);
                }
            }

            for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(HashNodes.getFirstInSequence(other))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), isCompareByIdentity);
            }

            assert verifyStore(hash);

            return merged;
        }

        @Specialization(guards = {
                "isBucketHash(hash)",
                "!isEmptyHash(hash)",
                "isRubyHash(other)",
                "isPackedHash(other)",
                "!isEmptyHash(other)"
        })
        public RubyBasicObject mergeBucketsPacked(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject other, NotProvided block) {
            final boolean isCompareByIdentity = isCompareByIdentity(hash);

            final RubyBasicObject merged = createHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(getSize(hash) + getSize(other))], 0, null, null);

            for (Map.Entry<Object, Object> keyValue : BucketsStrategy.iterableKeyValues(HashNodes.getFirstInSequence(hash))) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), isCompareByIdentity);
            }

            final Object[] otherStore = (Object[]) getStore(other);
            final int otherSize = getSize(other);

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < otherSize) {
                    setNode.executeSet(frame, merged, PackedArrayStrategy.getKey(otherStore, n), PackedArrayStrategy.getValue(otherStore, n), isCompareByIdentity);
                }
            }

            assert verifyStore(hash);

            return merged;
        }

        // Merge using a block

        @Specialization(guards = {"isRubyHash(other)", "!isCompareByIdentity(hash)"})
        public RubyBasicObject merge(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject other, RubyProc block) {
            CompilerDirectives.bailout("Hash#merge with a block cannot be compiled at the moment");

            final RubyBasicObject merged = createHash(hash.getLogicalClass(), null, null, new Entry[BucketsStrategy.capacityGreaterThan(getSize(hash) + getSize(other))], 0, null, null);

            int size = 0;

            for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(hash)) {
                setNode.executeSet(frame, merged, keyValue.getKey(), keyValue.getValue(), false);
                size++;
            }

            if (lookupEntryNode == null) {
                CompilerDirectives.transferToInterpreter();
                lookupEntryNode = insert(new LookupEntryNode(getContext(), getSourceSection()));
            }

            for (Map.Entry<Object, Object> keyValue : HashNodes.iterableKeyValues(other)) {
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

            setSize(merged, size);

            assert verifyStore(hash);

            return merged;
        }

        // Merge with something that wasn't a hash

        @Specialization(guards = "!isRubyHash(other)")
        public Object merge(VirtualFrame frame, RubyBasicObject hash, Object other, Object block) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }
            
            final RubyProc blockProc;
            
            if (block == NotProvided.INSTANCE) {
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
        public Object setDefault(VirtualFrame frame, RubyBasicObject hash, Object defaultValue) {
            ruby(frame, "Rubinius.check_frozen");
            setDefaultValue(hash, defaultValue);
            setDefaultBlock(hash, null);
            return defaultValue;
        }
    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class ShiftNode extends CoreMethodArrayArgumentsNode {

        public ShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isEmptyHash(hash)", "!hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public RubyBasicObject shiftEmpty(RubyBasicObject hash) {
            return nil();
        }

        @Specialization(guards = {"isEmptyHash(hash)", "hasDefaultValue(hash)", "!hasDefaultBlock(hash)"})
        public Object shiftEmpyDefaultValue(RubyBasicObject hash) {
            return getDefaultValue(hash);
        }

        @Specialization(guards = {"isEmptyHash(hash)", "!hasDefaultValue(hash)", "hasDefaultBlock(hash)"})
        public Object shiftEmptyDefaultProc(RubyBasicObject hash) {
            return getDefaultBlock(hash).rootCall(hash, nil());
        }

        @Specialization(guards = {"!isEmptyHash(hash)", "isPackedHash(hash)"})
        public RubyBasicObject shiftPackedArray(RubyBasicObject hash) {
            assert verifyStore(hash);
            
            final Object[] store = (Object[]) getStore(hash);
            
            final Object key = PackedArrayStrategy.getKey(store, 0);
            final Object value = PackedArrayStrategy.getValue(store, 0);
            
            PackedArrayStrategy.removeEntry(store, 0);
            
            setSize(hash, getSize(hash) - 1);

            assert verifyStore(hash);
            
            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

        @Specialization(guards = {"!isEmptyHash(hash)", "isBucketHash(hash)"})
        public RubyBasicObject shiftBuckets(RubyBasicObject hash) {
            assert verifyStore(hash);

            final Entry first = getFirstInSequence(hash);
            assert first.getPreviousInSequence() == null;

            final Object key = first.getKey();
            final Object value = first.getValue();
            
            setFirstInSequence(hash, first.getNextInSequence());

            if (first.getNextInSequence() != null) {
                first.getNextInSequence().setPreviousInSequence(null);
                setFirstInSequence(hash, first.getNextInSequence());
            }

            if (getLastInSequence(hash) == first) {
                setLastInSequence(hash, null);
            }
            
            /*
             * TODO CS 7-Mar-15 this isn't great - we need to remove from the
             * lookup sequence for which we need the previous entry in the
             * bucket. However we normally get that from the search result, and
             * we haven't done a search here - we've just taken the first
             * result. For the moment we'll just do a manual search.
             */
            
            final Entry[] store = (Entry[]) getStore(hash);
            
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


            setSize(hash, getSize(hash) - 1);

            assert verifyStore(hash);

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), key, value);
        }

    }
    
    @CoreMethod(names = {"size", "length"})
    @ImportStatic(HashGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullHash(hash)")
        public int sizeNull(RubyBasicObject hash) {
            return 0;
        }

        @Specialization(guards = "!isNullHash(hash)")
        public int sizePackedArray(RubyBasicObject hash) {
            return getSize(hash);
        }

    }

    @CoreMethod(names = "rehash", raiseIfFrozenSelf = true)
    @ImportStatic(HashGuards.class)
    public abstract static class RehashNode extends CoreMethodArrayArgumentsNode {

        @Child private HashNode hashNode;

        public RehashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            hashNode = new HashNode(context, sourceSection);
        }

        @Specialization(guards = "isNullHash(hash)")
        public RubyBasicObject rehashNull(RubyBasicObject hash) {
            return hash;
        }

        @Specialization(guards = "isPackedHash(hash)")
        public RubyBasicObject rehashPackedArray(VirtualFrame frame, RubyBasicObject hash) {
            assert verifyStore(hash);

            final Object[] store = (Object[]) getStore(hash);
            final int size = getSize(hash);

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    PackedArrayStrategy.setHashed(store, n, hashNode.hash(frame, PackedArrayStrategy.getKey(store, n)));
                }
            }

            assert verifyStore(hash);

            return hash;
        }

        @TruffleBoundary
        @Specialization(guards = "isBucketHash(hash)")
        public RubyBasicObject rehashBuckets(RubyBasicObject hash) {
            assert verifyStore(hash);

            final Entry[] entries = (Entry[]) getStore(hash);
            Arrays.fill(entries, null);

            Entry entry = getFirstInSequence(hash);

            while (entry != null) {
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

            assert verifyStore(hash);
            
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
        public Object defaultValue(RubyBasicObject hash) {
            final Object value = getDefaultValue(hash);
            
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
        public Object setDefaultValue(RubyBasicObject hash, Object defaultValue) {
            HashNodes.setDefaultValue(hash, defaultValue);
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
        public RubyProc setDefaultProc(RubyBasicObject hash, RubyProc defaultProc) {
            setDefaultValue(hash, null);
            setDefaultBlock(hash, defaultProc);
            return defaultProc;
        }

        @Specialization(guards = "isNil(nil)")
        public RubyBasicObject setDefaultProc(RubyBasicObject hash, Object nil) {
            setDefaultValue(hash, null);
            setDefaultBlock(hash, null);
            return nil();
        }

    }

    public static class HashAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createEmptyHash(rubyClass);
        }

    }
}
