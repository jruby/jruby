/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;

import java.util.List;

@AcceptMessage(value = "EXECUTE", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignExecuteNode extends ForeignExecuteBaseNode {

    @Child private Node findContextNode;
    @Child private ExecuteMethodNode executeMethodNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, Object[] args) {
        return getInteropNode().executeWithTarget(frame, object);
    }

    private ExecuteMethodNode getInteropNode() {
        if (executeMethodNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            executeMethodNode = insert(ForeignExecuteNodeFactory.ExecuteMethodNodeGen.create(context, null, null));
        }

        return executeMethodNode;
    }

    @NodeChild(value="method", type = RubyNode.class)
    public static abstract class ExecuteMethodNode extends RubyNode {

        @Child private IndirectCallNode callNode;
        public ExecuteMethodNode(RubyContext context,
                                 SourceSection sourceSection) {
            super(context, sourceSection);
            callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        @Specialization(guards = {"isRubyProc(proc)", "proc == cachedProc"})
        protected Object doCallProc(VirtualFrame frame, DynamicObject proc,
                                    @Cached("proc") DynamicObject cachedProc,
                                    @Cached("create(getCallTarget(cachedProc))") DirectCallNode callNode) {
            final List<Object> faArgs = ForeignAccess.getArguments(frame);
            Object[] args = faArgs.toArray();
            return callNode.call(frame, RubyArguments.pack(Layouts.PROC.getDeclarationFrame(cachedProc), null, Layouts.PROC.getMethod(cachedProc), DeclarationContext.METHOD, null, Layouts.PROC.getSelf(cachedProc), null, args));
        }

        @Specialization(guards = "isRubyProc(proc)")
        protected Object doCallProc(VirtualFrame frame, DynamicObject proc) {
            final List<Object> faArgs = ForeignAccess.getArguments(frame);
            Object[] args = faArgs.toArray();
            return callNode.call(frame, Layouts.PROC.getCallTargetForType(proc), RubyArguments.pack(Layouts.PROC.getDeclarationFrame(proc), null, Layouts.PROC.getMethod(proc), DeclarationContext.METHOD, null, Layouts.PROC.getSelf(proc), null, args));
        }

        @Specialization(guards = {"isRubyMethod(method)", "method == cachedMethod"})
        protected Object doCall(VirtualFrame frame, DynamicObject method,
                                @Cached("method") DynamicObject cachedMethod,
                                @Cached("getMethod(cachedMethod)") InternalMethod internalMethod,
                                @Cached("create(getMethod(cachedMethod).getCallTarget())") DirectCallNode callNode) {
            final List<Object> faArgs = ForeignAccess.getArguments(frame);

            Object[] args = faArgs.subList(0, faArgs.size()).toArray();
            return callNode.call(frame, RubyArguments.pack(null, null, internalMethod, DeclarationContext.METHOD, null, Layouts.METHOD.getReceiver(cachedMethod), null, args));
        }

        @Specialization(guards = "isRubyMethod(method)")
        protected Object doCall(VirtualFrame frame, DynamicObject method) {
            final InternalMethod internalMethod = Layouts.METHOD.getMethod(method);
            final List<Object> faArgs = ForeignAccess.getArguments(frame);

            Object[] args = faArgs.subList(0, faArgs.size()).toArray();
            return callNode.call(frame, internalMethod.getCallTarget(), RubyArguments.pack(null, null, internalMethod, DeclarationContext.METHOD, null, Layouts.METHOD.getReceiver(method), null, args));
        }

        protected CallTarget getCallTarget(DynamicObject proc) {
            return Layouts.PROC.getCallTargetForType(proc);
        }

        protected InternalMethod getMethod(DynamicObject method) {
            return Layouts.METHOD.getMethod(method);
        }

        public abstract Object executeWithTarget(VirtualFrame frame, Object method);

    }

}
