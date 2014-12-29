/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "Proc")
public abstract class ProcNodes {

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ArityNode(ArityNode prev) {
            super(prev);
        }

        @Specialization
        public int arity(RubyProc proc) {
            return 1;
        }

    }

    @CoreMethod(names = "binding")
    public abstract static class BindingNode extends CoreMethodNode {

        public BindingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindingNode(BindingNode prev) {
            super(prev);
        }

        @Specialization
        public Object binding(RubyProc proc) {
            if (!RubyProc.PROC_BINDING) {
                getContext().getWarnings().warn("Proc#binding disabled, returning nil. Use -Xtruffle.proc.binding=true to enable it.");
                return getContext().getCoreLibrary().getNilObject();
            }

            final MaterializedFrame frame = proc.getDeclarationFrame();

            return new RubyBinding(getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(frame.getArguments()),
                    frame);
        }

    }

    @CoreMethod(names = {"call", "[]"}, argumentsAsArray = true, needsBlock = true)
    public abstract static class CallNode extends CoreMethodNode {

        @Child protected YieldDispatchHeadNode yieldNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        public CallNode(CallNode prev) {
            super(prev);
            yieldNode = prev.yieldNode;
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyProc proc, Object[] args, UndefinedPlaceholder block) {
            return yieldNode.dispatch(frame, proc, args);
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyProc proc, Object[] args, RubyProc block) {
            return yieldNode.dispatchWithModifiedBlock(frame, proc, block, args);
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize(RubyProc proc, RubyProc block) {
            proc.initialize(block.getSharedMethodInfo(), block.getCallTargetForMethods(), block.getCallTargetForMethods(),
                    block.getDeclarationFrame(), block.getDeclaringModule(), block.getMethod(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope());

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "lambda?")
    public abstract static class LambdaNode extends CoreMethodNode {

        public LambdaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LambdaNode(LambdaNode prev) {
            super(prev);
        }

        @Specialization
        public boolean lambda(RubyProc proc) {
            return proc.getType() == RubyProc.Type.LAMBDA;
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ParametersNode(ParametersNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray parameters(RubyProc proc) {
            final ArgsNode argsNode = proc.getSharedMethodInfo().getParseTree().findFirstChild(ArgsNode.class);

            final String[] parameters = Helpers.encodeParameterList((ArgsNode) argsNode).split(";");

            return (RubyArray) getContext().toTruffle(Helpers.parameterListToParameters(getContext().getRuntime(),
                    parameters, proc.getType() == RubyProc.Type.LAMBDA));
        }

    }

}
