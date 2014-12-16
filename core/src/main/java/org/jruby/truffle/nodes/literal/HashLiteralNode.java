/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.PredicateDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.hash.Entry;

import java.util.*;

public abstract class HashLiteralNode extends RubyNode {

    @Children protected final RubyNode[] keyValues;
    @Child protected DispatchHeadNode dupNode;
    @Child protected DispatchHeadNode freezeNode;

    protected HashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        super(context, sourceSection);
        assert keyValues.length % 2 == 0;
        this.keyValues = keyValues;
        dupNode = new DispatchHeadNode(context);
        freezeNode = new DispatchHeadNode(context);
    }

    public static HashLiteralNode create(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        if (keyValues.length == 0) {
            return new EmptyHashLiteralNode(context, sourceSection);
        } else if (keyValues.length <= RubyHash.HASHES_SMALL * 2) {
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

        @Child protected PredicateDispatchHeadNode equalNode;

        public SmallHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
            equalNode = new PredicateDispatchHeadNode(context);
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            final Object[] storage = new Object[RubyHash.HASHES_SMALL * 2];

            int position = 0;

            initializers: for (int n = 0; n < keyValues.length; n += 2) {
                Object key = keyValues[n].execute(frame);

                if (key instanceof RubyString && !((RubyString) key).isFrozen()) {
                    key = freezeNode.call(frame, dupNode.call(frame, key, "dup", null), "freeze", null);
                }

                final Object value = keyValues[n + 1].execute(frame);

                for (int i = 0; i < n; i += 2) {
                    if (equalNode.call(frame, key, "eql?", null, storage[i])) {
                        storage[i + 1] = value;
                        continue initializers;
                    }
                }

                storage[position] = key;
                storage[position + 1] = value;
                position += 2;
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null, storage, position / 2, null);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        public GenericHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            notDesignedForCompilation();

            final List<Entry> entries = new ArrayList<>();

            for (int n = 0; n < keyValues.length; n += 2) {
                final Object key = keyValues[n].execute(frame);
                final Object value = keyValues[n + 1].execute(frame);
                entries.add(new Entry(key, value));
            }

            return RubyHash.verySlowFromEntries(getContext(), entries);
        }

    }

}
