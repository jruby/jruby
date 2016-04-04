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
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyObjectType;
import org.jruby.truffle.language.dispatch.DispatchAction;
import org.jruby.truffle.language.dispatch.DispatchHeadNode;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;

@AcceptMessage(value = "READ", receiverType = RubyObjectType.class, language = RubyLanguage.class)
public final class ForeignReadNode extends ForeignReadBaseNode {

    @Child private Node findContextNode;
    @Child private RubyNode interopNode;

    @Override
    public Object access(VirtualFrame frame, DynamicObject object, Object name) {
        return getInteropNode().execute(frame);
    }

    private RubyNode getInteropNode() {
        if (interopNode == null) {
            CompilerDirectives.transferToInterpreter();
            findContextNode = insert(RubyLanguage.INSTANCE.unprotectedCreateFindContextNode());
            final RubyContext context = RubyLanguage.INSTANCE.unprotectedFindContext(findContextNode);
            interopNode = insert(new UnresolvedInteropReadNode(context, null));
        }

        return interopNode;
    }

    public static class UnresolvedInteropReadNode extends RubyNode {

        private final int labelIndex;

        public UnresolvedInteropReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableReadNode(getContext(), getSourceSection(), name, labelIndex)).execute(frame);
                    }
                }
                DynamicObject receiver = (DynamicObject) ForeignAccess.getReceiver(frame);

                if (RubyGuards.isRubyString(receiver)) {
                    // TODO CS 22-Mar-16 monomorphic, what happens if it fails for other objects?
                    return this.replace(new UnresolvedInteropStringReadNode(getContext(), getSourceSection())).execute(frame);
                }

                InternalMethod labelMethod = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedReadNode(getContext(), getSourceSection(), labelIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropReadNode(getContext(), getSourceSection(), (String) label, labelIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex)).execute(frame);
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

    public static class ResolvedInteropReadNode extends RubyNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final int labelIndex;

        public ResolvedInteropReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    public static class ResolvedInteropIndexedReadNode extends RubyNode {

        private final String name;
        @Child private DispatchHeadNode head;
        @Child private ForeignToRubyNode toRubyIndex;
        private final int indexIndex;

        public ResolvedInteropIndexedReadNode(RubyContext context, SourceSection sourceSection, int indexIndex) {
            super(context, sourceSection);
            this.name = "[]";
            this.indexIndex = indexIndex;
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.toRubyIndex = ForeignToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeConvert(frame, ForeignAccess.getArguments(frame).get(indexIndex));
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[] {index});
        }
    }

    public static class ResolvedInteropReadFromSymbolNode extends RubyNode {

        @Child private DispatchHeadNode head;
        private final DynamicObject name;
        private final int labelIndex;

        public ResolvedInteropReadFromSymbolNode(RubyContext context, SourceSection sourceSection, DynamicObject name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    public static class InteropInstanceVariableReadNode extends RubyNode {

        @Child private ReadObjectFieldNode read;
        private final String name;
        private final int labelIndex;

        public InteropInstanceVariableReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.read = ReadObjectFieldNodeGen.create(context, name, nil());
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return read.execute((DynamicObject) ForeignAccess.getReceiver(frame));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

    public static class UnresolvedInteropStringReadNode extends RubyNode {

        private final int labelIndex;

        public UnresolvedInteropStringReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableReadNode(getContext(), getSourceSection(), name, labelIndex)).execute(frame);
                    }
                }
                if (label instanceof Integer || label instanceof  Long) {
                    return this.replace(new InteropReadStringByteNode(getContext(), getSourceSection(), labelIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropReadNode(getContext(), getSourceSection(), (String) label, labelIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex)).execute(frame);
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

    public static class InteropReadStringByteNode extends RubyNode {

        private final int labelIndex;

        public InteropReadStringByteNode(RubyContext context, SourceSection sourceSection, int labelIndex) {
            super(context, sourceSection);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (RubyGuards.isRubyString(ForeignAccess.getReceiver(frame))) {
                final DynamicObject string = (DynamicObject) ForeignAccess.getReceiver(frame);
                final int index = (int) ForeignAccess.getArguments(frame).get(labelIndex);
                if (index >= Layouts.STRING.getRope(string).byteLength()) {
                    return 0;
                } else {
                    return (byte) StringOperations.getByteListReadOnly(string).get(index);
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

}
