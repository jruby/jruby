/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.interop.InteropNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;

public class HashForeignAccessFactory implements ForeignAccess.Factory10, ForeignAccess.Factory {

    private final RubyContext context;

    private HashForeignAccessFactory(RubyContext context) {
        this.context = context;
    }

    public static ForeignAccess create(RubyContext context) {
        final HashForeignAccessFactory hashFactory = new HashForeignAccessFactory(context);
        return ForeignAccess.create(null, hashFactory);
    }


    @Override
    public boolean canHandle(TruffleObject to) {
        return RubyGuards.isRubyHash(to);
    }

    @Override
    public CallTarget accessIsNull() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsNull(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessIsExecutable() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsExecutable(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsBoxedPrimitive(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createHasSizePropertyTrue(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessGetSize() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createGetSize(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessUnbox() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createIsBoxedPrimitive(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createRead(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessWrite() {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createWrite(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessExecute(int i) {
        return null;
    }

    @Override
    public CallTarget accessInvoke(int arity) {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecuteAfterRead(context, SourceSection.createUnavailable("", ""), arity)));
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessMessage(Message msg) {
        return null;
    }

    protected static final class RubyInteropRootNode extends RootNode {

        @Child private RubyNode node;

        public RubyInteropRootNode(RubyNode node) {
            super(RubyLanguage.class, node.getSourceSection(), null);
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
