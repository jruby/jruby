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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.methods.CallBoundMethodNode;
import org.jruby.truffle.language.methods.CallBoundMethodNodeGen;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.yield.CallBlockNode;
import org.jruby.truffle.language.yield.CallBlockNodeGen;

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

        public HelperNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeCall(VirtualFrame frame, Object receiver, Object[] arguments);

        @Specialization(guards = "isRubyProc(proc)")
        protected Object callProc(VirtualFrame frame, DynamicObject proc, Object[] arguments,
                @Cached("createCallBlockNode()") CallBlockNode callBlockNode) {
            Object self = Layouts.PROC.getSelf(proc);
            return callBlockNode.executeCallBlock(frame, proc, self, null, arguments);
        }

        protected CallBlockNode createCallBlockNode() {
            return CallBlockNodeGen.create(getContext(), getSourceSection(), DeclarationContext.BLOCK, null, null, null, null);
        }

        @Specialization(guards = "isRubyMethod(method)")
        protected Object callMethod(VirtualFrame frame, DynamicObject method, Object[] arguments,
                @Cached("createCallBoundMethodNode()") CallBoundMethodNode callBoundMethodNode) {
            return callBoundMethodNode.executeCallBoundMethod(frame, method, arguments, null);
        }

        protected CallBoundMethodNode createCallBoundMethodNode() {
            return CallBoundMethodNodeGen.create(getContext(), getSourceSection(), null, null, null);
        }

    }

}
