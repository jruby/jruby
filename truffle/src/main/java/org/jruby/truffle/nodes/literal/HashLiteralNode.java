/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.hash.HashNode;
import org.jruby.truffle.nodes.objects.IsFrozenNode;
import org.jruby.truffle.nodes.objects.IsFrozenNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.hash.PackedArrayStrategy;

import java.util.ArrayList;
import java.util.List;

public abstract class HashLiteralNode extends RubyNode {

    @Children protected final RubyNode[] keyValues;
    @Child protected CallDispatchHeadNode dupNode;
    @Child protected CallDispatchHeadNode freezeNode;

    protected HashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        super(context, sourceSection);
        assert keyValues.length % 2 == 0;
        this.keyValues = keyValues;
        dupNode = DispatchHeadNodeFactory.createMethodCall(context);
        freezeNode = DispatchHeadNodeFactory.createMethodCall(context);
    }

    public int size() {
        return keyValues.length / 2;
    }

    public RubyNode getKey(int index) {
        return keyValues[2 * index];
    }

    public RubyNode getValue(int index) {
        return keyValues[2 * index + 1];
    }

    public static HashLiteralNode create(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        if (keyValues.length == 0) {
            return new EmptyHashLiteralNode(context, sourceSection);
        } else if (keyValues.length <= PackedArrayStrategy.MAX_ENTRIES * 2) {
            return new SmallHashLiteralNode(context, sourceSection, keyValues);
        } else {
            return new GenericHashLiteralNode(context, sourceSection, keyValues);
        }
    }

    @Override
    public abstract RubyHash executeRubyHash(VirtualFrame frame);

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyHash(frame);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        for (RubyNode child : keyValues) {
            child.executeVoid(frame);
        }
    }

    public static class EmptyHashLiteralNode extends HashLiteralNode {

        public EmptyHashLiteralNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection, new RubyNode[]{});
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, null, 0, null);
        }

    }

    public static class SmallHashLiteralNode extends HashLiteralNode {

        private final ConditionProfile stringKeyProfile = ConditionProfile.createBinaryProfile();

        @Child private HashNode hashNode;
        @Child private CallDispatchHeadNode equalNode;
        @Child private IsFrozenNode isFrozenNode;

        public SmallHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
            hashNode = new HashNode(context, sourceSection);
            equalNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            final Object[] store = PackedArrayStrategy.createStore();

            int size = 0;

            initializers: for (int n = 0; n < keyValues.length / 2; n++) {
                Object key = keyValues[n * 2].execute(frame);

                if (stringKeyProfile.profile(key instanceof RubyString)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
                    }

                    if (!isFrozenNode.executeIsFrozen(key)) {
                        key = freezeNode.call(frame, dupNode.call(frame, key, "dup", null), "freeze", null);
                    }
                }

                final int hashed = hashNode.hash(frame, key);

                final Object value = keyValues[n * 2 + 1].execute(frame);

                for (int i = 0; i < n; i++) {
                    if (i < size &&
                            hashed == PackedArrayStrategy.getHashed(store, i) &&
                            equalNode.callBoolean(frame, key, "eql?", null, PackedArrayStrategy.getKey(store, i))) {
                        PackedArrayStrategy.setKey(store, i, key);
                        PackedArrayStrategy.setValue(store, i, value);
                        continue initializers;
                    }
                }

                PackedArrayStrategy.setHashedKeyValue(store, size, hashed, key, value);
                size++;
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, store, size, null);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        public GenericHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();

            final List<KeyValue> entries = new ArrayList<>();

            for (int n = 0; n < keyValues.length; n += 2) {
                final Object key = keyValues[n].execute(frame);
                final Object value = keyValues[n + 1].execute(frame);
                entries.add(new KeyValue(key, value));
            }

            return HashOperations.verySlowFromEntries(getContext(), entries, false);
        }

    }

}
