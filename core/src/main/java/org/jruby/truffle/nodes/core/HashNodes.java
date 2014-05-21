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
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;

@CoreClass(name = "Hash")
public abstract class HashNodes {

    @CoreMethod(names = "==", minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(RubyHash a, RubyHash b) {
            notDesignedForCompilation();

            return a.storage.equals(b.storage);
        }

        @Specialization
        public boolean equal(RubyHash a, Object b) {
            notDesignedForCompilation();

            if (b instanceof RubyHash) {
                return equal(a, (RubyHash) b);
            } else {
                return false;
            }
        }

    }

    @CoreMethod(names = "[]", isModuleMethod = true, needsSelf = false, isSplatted = true)
    public abstract static class ConstructNode extends CoreMethodNode {

        public ConstructNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConstructNode(ConstructNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash construct(Object[] args) {
            notDesignedForCompilation();

            final RubyHash hash = new RubyHash(getContext().getCoreLibrary().getHashClass());

            if (args.length == 1) {
                final Object[] arrayValues = ((RubyArray) args[0]).slowToArray();

                for (int n = 0; n < arrayValues.length; n++) {
                    final Object[] keyValue = ((RubyArray) arrayValues[n]).slowToArray();
                    hash.put(keyValue[0], keyValue[1]);
                }
            } else {
                if (args.length % 2 != 0) {
                    // TODO(CS): figure out what error to throw here
                    throw new UnsupportedOperationException();
                }

                for (int n = 0; n < args.length; n += 2) {
                    hash.put(args[n], args[n + 1]);
                }
            }

            return hash;
        }

    }

    @CoreMethod(names = "[]", minArgs = 1, maxArgs = 1)
    public abstract static class GetIndexNode extends CoreMethodNode {

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        @Specialization
        public Object construct(RubyHash hash, Object index) {
            notDesignedForCompilation();

            final Object value = hash.get(index);

            if (value == null) {
                if (hash.defaultBlock == null) {
                    return NilPlaceholder.INSTANCE;
                } else {
                    return hash.defaultBlock.call(hash, index);
                }
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "[]=", minArgs = 2, maxArgs = 2)
    public abstract static class SetIndexNode extends CoreMethodNode {

        public SetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetIndexNode(SetIndexNode prev) {
            super(prev);
        }

        @Specialization
        public Object construct(RubyHash hash, Object index, Object value) {
            notDesignedForCompilation();

            hash.put(index, value);
            return value;
        }

    }

    @CoreMethod(names = "delete", minArgs = 1, maxArgs = 1)
    public abstract static class DeleteNode extends CoreMethodNode {

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
        }

        @Specialization
        public Object delete(RubyHash hash, Object index) {
            notDesignedForCompilation();

            hash.checkFrozen();

            final Object value = hash.getMap().remove(index);

            if (value == null) {
                return NilPlaceholder.INSTANCE;
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "dup", maxArgs = 0)
    public abstract static class DupNode extends CoreMethodNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DupNode(DupNode prev) {
            super(prev);
        }

        @Specialization
        public Object dup(RubyHash hash) {
            notDesignedForCompilation();

            final RubyHash newHash = new RubyHash(getContext().getCoreLibrary().getHashClass());
            newHash.setInstanceVariables(hash.getFields());
            newHash.storage.putAll(hash.storage);
            return newHash;
        }

    }

    @CoreMethod(names = "each", needsBlock = true, maxArgs = 0)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash each(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            int count = 0;

            try {
                for (Map.Entry<Object, Object> entry : hash.storage.entrySet()) {
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
    public abstract static class EmptyNode extends CoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean empty(RubyHash hash) {
            notDesignedForCompilation();

            return hash.storage.isEmpty();
        }

    }

    @CoreMethod(names = "to_a", maxArgs = 0)
    public abstract static class ToArrayNode extends CoreMethodNode {

        public ToArrayNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToArrayNode(ToArrayNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray toArray(RubyHash hash) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object key : hash.storage.keySet()) {
                RubyArray subArray = new RubyArray(getContext().getCoreLibrary().getArrayClass());

                subArray.slowPush(key);
                subArray.slowPush(hash.storage.get(key));
                array.slowPush(subArray);
            }
            return array;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, maxArgs = 0)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder initialize(RubyHash hash, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            hash.initialize(null);
            return NilPlaceholder.INSTANCE;
        }

        @Specialization
        public NilPlaceholder initialize(RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            hash.initialize(block);
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true, maxArgs = 0)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapNode(MapNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray map(VirtualFrame frame, RubyHash hash, RubyProc block) {
            notDesignedForCompilation();

            final RubyArray result = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            int count = 0;

            try {
                for (Map.Entry<Object, Object> entry : hash.storage.entrySet()) {
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
    public abstract static class MergeNode extends CoreMethodNode {

        public MergeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MergeNode(MergeNode prev) {
            super(prev);
        }

        @CompilerDirectives.SlowPath
        @Specialization
        public RubyHash merge(RubyHash hash, RubyHash other) {
            notDesignedForCompilation();

            final RubyHash merged = new RubyHash(getContext().getCoreLibrary().getHashClass());
            merged.getMap().putAll(hash.getMap());
            merged.getMap().putAll(other.getMap());
            return merged;
        }

    }

    @CoreMethod(names = "key?", minArgs = 1, maxArgs = 1)
    public abstract static class KeyNode extends CoreMethodNode {

        public KeyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KeyNode(KeyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean key(RubyHash hash, Object key) {
            return hash.storage.containsKey(key);
        }

    }

    @CoreMethod(names = "keys", maxArgs = 0)
    public abstract static class KeysNode extends CoreMethodNode {

        public KeysNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KeysNode(KeysNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray keys(RubyHash hash) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object key : hash.storage.keySet()) {
                array.slowPush(key);
            }

            return array;
        }

    }

    @CoreMethod(names = "size", maxArgs = 0)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubyHash hash) {
            return hash.storage.size();
        }

    }

    @CoreMethod(names = "values", maxArgs = 0)
    public abstract static class ValuesNode extends CoreMethodNode {

        public ValuesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ValuesNode(ValuesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray values(RubyHash hash) {
            notDesignedForCompilation();

            final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass());

            for (Object value : hash.storage.values()) {
                array.slowPush(value);
            }

            return array;
        }

    }

}
