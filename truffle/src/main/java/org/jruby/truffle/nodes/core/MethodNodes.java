/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.core.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyMethod;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.core.RubyUnboundMethod;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "Method")
public abstract class MethodNodes {

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        @Child protected ReferenceEqualNode referenceEqualNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        protected boolean areSame(VirtualFrame frame, Object left, Object right) {
            if (referenceEqualNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                referenceEqualNode = insert(BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
            return referenceEqualNode.executeReferenceEqual(frame, left, right);
        }

        @Specialization
        public boolean equal(VirtualFrame frame, RubyMethod a, RubyMethod b) {
            return areSame(frame, a.getReceiver(), b.getReceiver()) && a.getMethod() == b.getMethod();
        }

        @Specialization(guards = "!isRubyMethod(arguments[1])")
        public boolean equal(RubyMethod a, Object b) {
            return false;
        }

    }

    @CoreMethod(names = "call", argumentsAsArray = true)
    public abstract static class CallNode extends CoreMethodNode {

        @Child private IndirectCallNode callNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        public CallNode(CallNode prev) {
            super(prev);
            callNode = prev.callNode;
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyMethod method, Object... arguments) {
            // TODO(CS 11-Jan-15) should use a cache and DirectCallNode here so that we can inline - but it's
            // incompatible with our current dispatch chain.

            final InternalMethod internalMethod = method.getMethod();

            return callNode.call(frame, method.getMethod().getCallTarget(), RubyArguments.pack(
                    internalMethod,
                    internalMethod.getDeclarationFrame(),
                    method.getReceiver(),
                    null,
                    arguments));
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NameNode(NameNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol name(RubyMethod method) {
            notDesignedForCompilation();

            return getContext().newSymbol(method.getMethod().getName());
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OwnerNode(OwnerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule owner(RubyMethod method) {
            return method.getMethod().getDeclaringModule();
        }

    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends CoreMethodNode {

        public ReceiverNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReceiverNode(ReceiverNode prev) {
            super(prev);
        }

        @Specialization
        public Object receiver(RubyMethod method) {
            return method.getReceiver();
        }

    }

    @CoreMethod(names = "unbind")
    public abstract static class UnbindNode extends CoreMethodNode {

        public UnbindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnbindNode(UnbindNode prev) {
            super(prev);
        }

        @Specialization
        public RubyUnboundMethod unbind(RubyMethod method) {
            notDesignedForCompilation();

            return new RubyUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), method.getMethod());
        }

    }

}
