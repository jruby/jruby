/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyHash;

import java.util.LinkedHashMap;

public abstract class HashLiteralNode extends RubyNode {

    @Children protected final RubyNode[] keyValues;

    protected HashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        super(context, sourceSection);
        assert keyValues.length % 2 == 0;
        this.keyValues = keyValues;
    }

    public static HashLiteralNode create(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
        if (keyValues.length == 0) {
            return new EmptyHashLiteralNode(context, sourceSection);
        } else if (keyValues.length <= RubyContext.HASHES_SMALL * 2) {
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

    @Override
    public Object isDefined(@SuppressWarnings("unused") VirtualFrame frame) {
        return getContext().makeString("expression");
    }

    public static class EmptyHashLiteralNode extends HashLiteralNode {

        public EmptyHashLiteralNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection, new RubyNode[]{});
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, null);
        }

    }

    public static class SmallHashLiteralNode extends HashLiteralNode {

        public SmallHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            final Object[] storage = new Object[keyValues.length];

            for (int n = 0; n < storage.length; n++) {
                storage[n] = keyValues[n].execute(frame);
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        }

    }

    public static class GenericHashLiteralNode extends HashLiteralNode {

        public GenericHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] keyValues) {
            super(context, sourceSection, keyValues);
        }

        @ExplodeLoop
        @Override
        public RubyHash executeRubyHash(VirtualFrame frame) {
            notDesignedForCompilation();

            final LinkedHashMap<Object, Object> storage = new LinkedHashMap<>();

            for (int n = 0; n < keyValues.length; n += 2) {
                storage.put(keyValues[n].execute(frame), keyValues[n + 1].execute(frame));
            }

            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, storage);
        }

    }

}
