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


import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.interop.InteropNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.interop.InteropPredicate;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.exception.UnsupportedMessageException;
import com.oracle.truffle.api.interop.messages.Message;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.interop.messages.Execute;
import com.oracle.truffle.interop.messages.HasSize;
import com.oracle.truffle.interop.messages.IsBoxed;
import com.oracle.truffle.interop.messages.IsExecutable;
import com.oracle.truffle.interop.messages.IsNull;
import com.oracle.truffle.interop.messages.Receiver;

public class RubyMethodForeignAccessFactory implements ForeignAccessFactory {

    private final RubyContext context;

    public RubyMethodForeignAccessFactory(RubyContext context) {
        this.context = context;
    }

    @Override
    public InteropPredicate getLanguageCheck() {
        return new InteropPredicate() {
            @Override
            public boolean test(TruffleObject o) {
                return o instanceof  RubyBasicObject;
            }
        };
    }

    public CallTarget getAccess(Message tree) {
    	if (Execute.create(Receiver.create() ,0).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecute(context, new NullSourceSection("", ""))));
        } else if (IsExecutable.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsExecutable(context, new NullSourceSection("", ""))));
        } else if (IsBoxed.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsBoxedPrimitive(context, new NullSourceSection("", ""))));
        } else if (IsNull.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsNull(context, new NullSourceSection("", ""))));
        } else if (HasSize.create(Receiver.create()).matchStructure(tree)) {
            return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createHasSizePropertyFalse(context, new NullSourceSection("", ""))));
        } else {
            throw new UnsupportedMessageException("Message not supported: " + tree.toString());
        }
    }

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
