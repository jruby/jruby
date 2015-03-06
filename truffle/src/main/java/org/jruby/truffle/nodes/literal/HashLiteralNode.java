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
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.objects.IsFrozenNode;
import org.jruby.truffle.nodes.objects.IsFrozenNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;

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

    public static HashLiteralNode create(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        if (keyValues.length == 0) {
            return new EmptyHashLiteralNode(context, sourceSection);
        } else if (keyValues.length <= HashOperations.SMALL_HASH_SIZE * 2) {
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

        @Child private CallDispatchHeadNode equalNode;
        @Child private IsFrozenNode isFrozenNode;

        public SmallHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
            equalNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            final Object[] storage = new Object[HashOperations.SMALL_HASH_SIZE * 2];

            int end = 0;

            initializers: for (int n = 0; n < keyValues.length; n += 2) {
                Object key = keyValues[n].execute(frame);

                if (stringKeyProfile.profile(key instanceof RubyString)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeFactory.create(getContext(), getSourceSection(), null));
                    }

                    if (! isFrozenNode.executeIsFrozen(key)) {
                        key = freezeNode.call(frame, dupNode.call(frame, key, "dup", null), "freeze", null);
                    }
                }

                final Object value = keyValues[n + 1].execute(frame);

                for (int i = 0; i < n; i += 2) {
                    if (i < end && equalNode.callBoolean(frame, key, "eql?", null, storage[i])) {
                        storage[i + 1] = value;
                        continue initializers;
                    }
                }

                storage[end] = key;
                storage[end + 1] = value;
                end += 2;
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, storage, end / 2, null);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        public GenericHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            notDesignedForCompilation("9ef85ada171949aea09b621f42186e66");

            final List<KeyValue> entries = new ArrayList<>();

            for (int n = 0; n < keyValues.length; n += 2) {
                final Object key = keyValues[n].execute(frame);
                final Object value = keyValues[n + 1].execute(frame);
                entries.add(new KeyValue(key, value));
            }

            return HashOperations.verySlowFromEntries(getContext(), entries);
        }

    }

}
