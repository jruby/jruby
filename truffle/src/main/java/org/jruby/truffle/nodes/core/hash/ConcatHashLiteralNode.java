/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;

import java.util.ArrayList;
import java.util.List;

public class ConcatHashLiteralNode extends RubyNode {

    @Children private final RubyNode[] children;

    public ConcatHashLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] children) {
        super(context, sourceSection);
        this.children = children;
    }

    @Override
    public RubyHash executeRubyHash(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final List<KeyValue> keyValues = new ArrayList<>();

        for (RubyNode child : children) {
            try {
                keyValues.addAll(HashOperations.verySlowToKeyValues(child.executeRubyHash(frame)));
            } catch (UnexpectedResultException e) {
                throw new UnsupportedOperationException();
            }
        }

        return HashOperations.verySlowFromEntries(getContext(), keyValues, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRubyHash(frame);
    }

}
