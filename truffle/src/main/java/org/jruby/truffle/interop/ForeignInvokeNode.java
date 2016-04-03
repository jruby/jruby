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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.AcceptMessage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;

import java.util.List;

@AcceptMessage(value = "INVOKE", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignInvokeNode extends ForeignInvokeBaseNode {

    @Child private Node findContextNode;
    @Child private RubyNode interopNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, String name, Object[] args) {
        return getInteropNode().execute(frame);
    }

    private RubyNode getInteropNode() {
        if (interopNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            interopNode = insert(new UnresolvedInteropExecuteAfterReadNode(context, null, 0));
        }

        return interopNode;
    }

    public static class UnresolvedInteropExecuteAfterReadNode extends RubyNode {

        private final int arity;
        private final int labelIndex;

        public UnresolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, int arity){
            super(context, sourceSection);
            this.arity = arity;
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (ForeignAccess.getArguments(frame).get(labelIndex) instanceof  String) {
                return this.replace(new ResolvedInteropExecuteAfterReadNode(getContext(), getSourceSection(), (String) ForeignAccess.getArguments(frame).get(labelIndex), arity)).execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(ForeignAccess.getArguments(frame).get(0) + " not allowed as name");
            }
        }
    }

    public static class ResolvedInteropExecuteAfterReadNode extends RubyNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final int labelIndex;
        private final int receiverIndex;

        public ResolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, String name, int arity) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = 1;
            this.receiverIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(frame.getArguments()[labelIndex])) {
                final List<Object> arguments = ForeignAccess.getArguments(frame);
                return head.dispatch(frame, frame.getArguments()[receiverIndex], frame.getArguments()[labelIndex], null, arguments.subList(1, arguments.size()).toArray());
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

}
