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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.IsFrozenNode;
import org.jruby.truffle.nodes.objects.IsFrozenNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.hash.BucketsStrategy;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.PackedArrayStrategy;
import org.jruby.truffle.runtime.layouts.Layouts;

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
        public Object execute(VirtualFrame frame) {
            return Layouts.HASH.createHash(getContext().getCoreLibrary().getHashFactory(), null, null, null, 0, null, null, false);
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
            equalNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final Object[] store = PackedArrayStrategy.createStore();

            int size = 0;

            initializers: for (int n = 0; n < keyValues.length / 2; n++) {
                Object key = keyValues[n * 2].execute(frame);

                if (stringKeyProfile.profile(RubyGuards.isRubyString(key))) {
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

            return Layouts.HASH.createHash(getContext().getCoreLibrary().getHashFactory(), null, null, store, size, null, null, false);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        @Child SetNode setNode;

        public GenericHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (setNode == null) {
                CompilerDirectives.transferToInterpreter();
                setNode = insert(SetNodeGen.create(getContext(), getEncapsulatingSourceSection(), null, null, null, null));
            }

            final int bucketsCount = BucketsStrategy.capacityGreaterThan(keyValues.length / 2) * BucketsStrategy.OVERALLOCATE_FACTOR;
            final Entry[] newEntries = new Entry[bucketsCount];

            final DynamicObject hash = Layouts.HASH.createHash(getContext().getCoreLibrary().getHashFactory(), null, null, newEntries, 0, null, null, false);

            for (int n = 0; n < keyValues.length; n += 2) {
                final Object key = keyValues[n].execute(frame);
                final Object value = keyValues[n + 1].execute(frame);
                setNode.executeSet(frame, hash, key, value, false);
            }

            return hash;
        }

    }

}
