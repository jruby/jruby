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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;

public abstract class HashLiteralNode extends RubyNode {

    @Children protected final RubyNode[] keyValues;
    @Child protected CallDispatchHeadNode dupNode = DispatchHeadNodeFactory.createMethodCall();
    @Child protected CallDispatchHeadNode freezeNode = DispatchHeadNodeFactory.createMethodCall();

    protected HashLiteralNode(RubyNode[] keyValues) {
        assert keyValues.length % 2 == 0;
        this.keyValues = keyValues;
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

    public static HashLiteralNode create(RubyContext context, RubyNode[] keyValues) {
        if (keyValues.length == 0) {
            return new EmptyHashLiteralNode();
        } else if (keyValues.length <= context.getOptions().HASH_PACKED_ARRAY_MAX * 2) {
            return new SmallHashLiteralNode(keyValues);
        } else {
            return new GenericHashLiteralNode(keyValues);
        }
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (RubyNode child : keyValues) {
            child.executeVoid(frame);
        }
    }

    public static class EmptyHashLiteralNode extends HashLiteralNode {

        public EmptyHashLiteralNode() {
            super(new RubyNode[]{});
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            return Layouts.HASH.createHash(coreLibrary().getHashFactory(), null, 0, null, null, null, null, false);
        }

    }

    public static class SmallHashLiteralNode extends HashLiteralNode {

        private final ConditionProfile stringKeyProfile = ConditionProfile.createBinaryProfile();

        @Child private HashNode hashNode = new HashNode();
        @Child private CallDispatchHeadNode equalNode = DispatchHeadNodeFactory.createMethodCall();
        @Child private IsFrozenNode isFrozenNode;

        public SmallHashLiteralNode(RubyNode[] keyValues) {
            super(keyValues);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final Object[] store = PackedArrayStrategy.createStore(getContext());

            int size = 0;

            initializers: for (int n = 0; n < keyValues.length / 2; n++) {
                Object key = keyValues[n * 2].execute(frame);

                if (stringKeyProfile.profile(RubyGuards.isRubyString(key))) {
                    if (!isFrozen(key)) {
                        key = freezeNode.call(frame, dupNode.call(frame, key, "dup"), "freeze");
                    }
                }

                final int hashed = hashNode.hash(frame, key, false);

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

            return Layouts.HASH.createHash(coreLibrary().getHashFactory(), store, size, null, null, null, null, false);
        }

        protected boolean isFrozen(Object object) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(null));
            }
            return isFrozenNode.executeIsFrozen(object);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        @Child SetNode setNode;

        public GenericHashLiteralNode(RubyNode[] keyValues) {
            super(keyValues);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            if (setNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setNode = insert(SetNode.create());
            }

            final int bucketsCount = BucketsStrategy.capacityGreaterThan(keyValues.length / 2) * BucketsStrategy.OVERALLOCATE_FACTOR;
            final Entry[] newEntries = new Entry[bucketsCount];

            final DynamicObject hash = Layouts.HASH.createHash(coreLibrary().getHashFactory(), newEntries, 0, null, null, null, null, false);

            for (int n = 0; n < keyValues.length; n += 2) {
                final Object key = keyValues[n].execute(frame);
                final Object value = keyValues[n + 1].execute(frame);
                setNode.executeSet(frame, hash, key, value, false);
            }

            return hash;
        }

    }

}
