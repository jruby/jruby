/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.InternalRootNode;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.interop.InteropNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;

public class RubyMethodForeignAccessFactory implements ForeignAccess.Factory10 {
    private final RubyContext context;

    private RubyMethodForeignAccessFactory(RubyContext context) {
        this.context = context;
    }

    public static ForeignAccess create(RubyContext context) {
        return ForeignAccess.create(DynamicObject.class, new RubyMethodForeignAccessFactory(context));
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
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createHasSizePropertyFalse(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessGetSize() {
        return null;
    }

    @Override
    public CallTarget accessUnbox() {
        return null;
    }

    @Override
    public CallTarget accessRead() {
        return null;
    }

    @Override
    public CallTarget accessWrite() {
        return null;
    }

    @Override
    public CallTarget accessExecute(int i) {
        return Truffle.getRuntime().createCallTarget(new RubyInteropRootNode(InteropNode.createExecute(context, SourceSection.createUnavailable("", ""))));
    }

    @Override
    public CallTarget accessInvoke(int i) {
        return null;
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessMessage(com.oracle.truffle.api.interop.Message msg) {
        return null;
    }

    protected static final class RubyInteropRootNode extends RootNode implements InternalRootNode {

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
