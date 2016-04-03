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
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.WriteInstanceVariableNode;

@AcceptMessage(value = "WRITE", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignWriteNode extends ForeignWriteBaseNode {

    @Child private Node findContextNode;
    @Child private RubyNode interopNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, Object name, Object value) {
        return getInteropNode().execute(frame);
    }

    private RubyNode getInteropNode() {
        if (interopNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            interopNode = insert(new UnresolvedInteropWriteNode(context, null));
        }

        return interopNode;
    }

    public static class UnresolvedInteropWriteNode extends RubyNode {

        private final int labelIndex;
        private final int valueIndex;

        public UnresolvedInteropWriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
            this.valueIndex = 1;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableWriteNode(getContext(), getSourceSection(), name, labelIndex, valueIndex)).execute(frame);
                    }
                }
                DynamicObject receiver = (DynamicObject) ForeignAccess.getReceiver(frame);
                InternalMethod labelMethod = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedWriteNode(getContext(), getSourceSection(), labelIndex, valueIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropWriteNode(getContext(), getSourceSection(), (String) label, labelIndex, valueIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropWriteToSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex, valueIndex)).execute(frame);
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(label + " not allowed as name");
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(label + " not allowed as name");
            }
        }
    }

    public static class ResolvedInteropWriteNode extends RubyNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final String accessName;
        private final int labelIndex;
        private final int valueIndex;

        public ResolvedInteropWriteNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.accessName = name + "=";
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                Object value = ForeignAccess.getArguments(frame).get(valueIndex);
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    public static class ResolvedInteropWriteToSymbolNode extends RubyNode {

        @Child private DispatchHeadNode head;
        private final DynamicObject name;
        private final DynamicObject  accessName;
        private final int labelIndex;
        private final int valueIndex;

        public ResolvedInteropWriteToSymbolNode(RubyContext context, SourceSection sourceSection, DynamicObject name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.accessName = context.getSymbolTable().getSymbol(Layouts.SYMBOL.getString(name) + "=");
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                Object value = ForeignAccess.getArguments(frame).get(valueIndex);
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    public static class InteropInstanceVariableWriteNode extends RubyNode {

        @Child private WriteInstanceVariableNode write;
        private final String name;
        private final int labelIndex;

        public InteropInstanceVariableWriteNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.labelIndex = labelIndex;
            this.write = new WriteInstanceVariableNode(context, sourceSection, name, new RubyInteropReceiverNode(context, sourceSection), new RubyInteropArgumentNode(context, sourceSection, valueIndex));
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return write.execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

    public static class ResolvedInteropIndexedWriteNode extends RubyNode {

        private final String name;
        @Child private DispatchHeadNode head;
        @Child private IndexLabelToRubyNode toRubyIndex;
        private final int indexIndex;
        private final int valueIndex;

        public ResolvedInteropIndexedWriteNode(RubyContext context, SourceSection sourceSection, int indexIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = "[]=";
            this.indexIndex = indexIndex;
            this.valueIndex = valueIndex;
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeWithTarget(frame, ForeignAccess.getArguments(frame).get(indexIndex));
            Object value = ForeignAccess.getArguments(frame).get(valueIndex);
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[] {index, value});
        }
    }

}
