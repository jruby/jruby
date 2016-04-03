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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
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

@AcceptMessage(value = "EXECUTE", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignExecuteNode extends ForeignExecuteBaseNode {

    @Child private Node findContextNode;
    @Child private HelperNode executeMethodNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, Object[] arguments) {
        return getHelperNode().executeCall(frame, object, arguments);
    }

    private HelperNode getHelperNode() {
        if (executeMethodNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            executeMethodNode = insert(ForeignExecuteNodeFactory.HelperNodeGen.create(context, null, null, null));
        }

        return executeMethodNode;
    }

    @NodeChildren({
            @NodeChild("receiver"),
            @NodeChild("arguments")
    })
    protected static abstract class HelperNode extends RubyNode {

        public HelperNode(RubyContext context,
                                 SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeCall(VirtualFrame frame, Object receiver, Object[] arguments);

        @Specialization(
                guards = {
                        "isRubyProc(proc)",
                        "proc == cachedProc"
                },
                limit = "getCacheLimit()"
        )
        protected Object callProcCached(VirtualFrame frame,
                                        DynamicObject proc,
                                        Object[] arguments,
                                        @Cached("proc") DynamicObject cachedProc,
                                        @Cached("create(getProcCallTarget(cachedProc))") DirectCallNode callNode) {
            return callNode.call(
                    frame,
                    RubyArguments.pack(
                            Layouts.PROC.getDeclarationFrame(cachedProc),
                            null,
                            Layouts.PROC.getMethod(cachedProc),
                            DeclarationContext.METHOD,
                            null,
                            Layouts.PROC.getSelf(cachedProc),
                            null,
                            arguments));
        }

        @Specialization(
                guards = "isRubyProc(proc)",
                contains = "callProcCached"
        )
        protected Object callProcUncached(VirtualFrame frame,
                                          DynamicObject proc,
                                          Object[] arguments,
                                          @Cached("create()") IndirectCallNode callNode) {
            return callNode.call(
                    frame,
                    getProcCallTarget(proc),
                    RubyArguments.pack(
                            Layouts.PROC.getDeclarationFrame(proc),
                            null,
                            Layouts.PROC.getMethod(proc),
                            DeclarationContext.METHOD,
                            null,
                            Layouts.PROC.getSelf(proc),
                            null,
                            arguments));
        }

        protected CallTarget getProcCallTarget(DynamicObject proc) {
            return Layouts.PROC.getCallTargetForType(proc);
        }

        @Specialization(
                guards = {
                        "isRubyMethod(method)",
                        "method == cachedMethod"
                },
                limit = "getCacheLimit()"
        )
        protected Object callMethodCached(VirtualFrame frame,
                                DynamicObject method,
                                Object[] arguments,
                                @Cached("method") DynamicObject cachedMethod,
                                @Cached("getMethod(cachedMethod)") InternalMethod cachedInternalMethod,
                                @Cached("create(cachedInternalMethod.getCallTarget())") DirectCallNode callNode) {
            return callNode.call(
                    frame,
                    RubyArguments.pack(
                            null,
                            null,
                            cachedInternalMethod,
                            DeclarationContext.METHOD,
                            null,
                            Layouts.METHOD.getReceiver(cachedMethod),
                            null,
                            arguments));
        }

        @Specialization(
                guards = "isRubyMethod(method)",
                contains = "callMethodCached"
        )
        protected Object callMethodUncached(VirtualFrame frame,
                                            DynamicObject method,
                                            Object[] arguments,
                                            @Cached("create()") IndirectCallNode callNode) {
            final InternalMethod internalMethod = getMethod(method);
            return callNode.call(
                    frame,
                    internalMethod.getCallTarget(),
                    RubyArguments.pack(
                            null,
                            null,
                            internalMethod,
                            DeclarationContext.METHOD,
                            null,
                            Layouts.METHOD.getReceiver(method),
                            null,
                            arguments));
        }

        protected InternalMethod getMethod(DynamicObject method) {
            return Layouts.METHOD.getMethod(method);
        }

        protected int getCacheLimit() {
            return getContext().getOptions().INTEROP_EXECUTE_CACHE;
        }

    }

}
