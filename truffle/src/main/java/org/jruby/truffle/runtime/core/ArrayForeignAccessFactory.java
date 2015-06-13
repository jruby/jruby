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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.NullSourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.interop.InteropNode;
import org.jruby.truffle.runtime.RubyContext;

public class ArrayForeignAccessFactory implements ForeignAccess.Factory10 {
    private final RubyContext context;

    private ArrayForeignAccessFactory(RubyContext context) {
        this.context = context;
    }

    public static ForeignAccess create(RubyContext context) {
        return ForeignAccess.create(RubyArray.class, new ArrayForeignAccessFactory(context));
    }

    @Override
    public CallTarget accessIsNull() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsNull(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessIsExecutable() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsExecutable(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsBoxedPrimitive(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createHasSizePropertyTrue(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessGetSize() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createGetSize(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessUnbox() {
        return null;
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createRead(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessWrite() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createWrite(context, new NullSourceSection("", ""))));
    }

    @Override
    public CallTarget accessExecute(int i) {
        return null;
    }

    @Override
    public CallTarget accessInvoke(int arity) {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecuteAfterRead(context, new NullSourceSection("", ""), arity)));
    }

    @Override
    public CallTarget accessMessage(Message msg) {
        return null;
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
