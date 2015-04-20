/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;


import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.interop.exception.*;
import com.oracle.truffle.interop.messages.*;
import com.oracle.truffle.api.interop.messages.Message;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.NullSourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyTypes;
import org.jruby.truffle.nodes.interop.InteropNode;
import org.jruby.truffle.runtime.RubyContext;

public class HashForeignAccessFactory implements ForeignAccessFactory {

    private final RubyContext context;

    public HashForeignAccessFactory(RubyContext context) {
        this.context = context;
    }

    @Override
    public InteropPredicate getLanguageCheck() {
        return new InteropPredicate() {
            @Override
            public boolean test(TruffleObject o) {
                return o instanceof  RubyHash;
            }
        };
    }

    public CallTarget getAccess(Message tree) {
    	if (Read.create(Receiver.create(), Argument.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createRead(context, new NullSourceSection("", ""), (Read) tree)));
    	} else if (Execute.create(Read.create(Receiver.create(), Argument.create()),0).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecuteAfterRead(context, new NullSourceSection("", ""), (Execute) tree)));
    	} else if (Write.create(Receiver.create(), Argument.create(), Argument.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createWrite(context, new NullSourceSection("", ""), (Write) tree)));
        } else if (IsExecutable.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsExecutable(context, new NullSourceSection("", ""))));
        } else if (IsBoxed.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsBoxedPrimitive(context, new NullSourceSection("", ""))));
        } else if (IsNull.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsNull(context, new NullSourceSection("", ""))));
        } else if (HasSize.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createHasSizePropertyTrue(context, new NullSourceSection("", ""))));
        } else if (GetSize.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createGetSize(context, new NullSourceSection("", ""))));
        } else {
            throw new UnsupportedMessageException("Message not supported: " + tree.toString());
        }
    }

    @TypeSystemReference(RubyTypes.class)
    protected static final class RubyInteropRootNode extends RootNode {

        @Child private RubyNode node;

        public RubyInteropRootNode(RubyNode node) {
            this.node = node;
        }

        @Override
        public Object execute(VirtualFrame virtualFrame) {
            return node.execute(virtualFrame);
        }

        @Override
        public String toString() {
            return "Root of: " + node.toString();
        }
    }
}
