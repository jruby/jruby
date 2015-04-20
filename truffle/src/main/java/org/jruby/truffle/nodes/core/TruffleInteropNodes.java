/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.interop.messages.*;
import com.oracle.truffle.interop.node.ForeignObjectAccessNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

@CoreClass(name = "Truffle::Interop")
public abstract class TruffleInteropNodes {

    @CoreMethod(names = "interop_to_ruby_primitive", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class InteropToRubyPrimitive extends CoreMethodNode {

        public InteropToRubyPrimitive(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InteropToRubyPrimitive(InteropToRubyPrimitive prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public int convert(byte value) {
            return value;
        }

        @Specialization
        public int convert(short value) {
            return value;
        }

        @Specialization
        public int convert(char value) {
            return value;
        }

        @Specialization
        public int convert(int value) {
            return value;
        }

        @Specialization
        public long convert(long value) {
            return value;
        }

        @Specialization
        public double convert(float value) {
            return value;
        }

        @Specialization
        public double convert(double value) {
            return value;
        }

        @Specialization
        public int convert(String value) {
            return (int) value.charAt(0);
        }

    }

    @CoreMethod(names = "executable?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsExecutableNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public IsExecutableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsExecutable.create(Receiver.create()));
        }

        public IsExecutableNode(IsExecutableNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public boolean isExecutable(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "boxed_primitive?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsBoxedPrimitiveNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public IsBoxedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsBoxed.create(Receiver.create()));
        }

        public IsBoxedPrimitiveNode(IsBoxedPrimitiveNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "null?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsNullNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public IsNullNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsNull.create(Receiver.create()));
        }

        public IsNullNode(IsNullNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public boolean isNull(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "has_size_property?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class HasSizePropertyNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public HasSizePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(HasSize.create(Receiver.create()));
        }

        public HasSizePropertyNode(HasSizePropertyNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public boolean hasSizeProperty(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "read_property", isModuleFunction = true, needsSelf = false, required = 2)
    public abstract static class ReadPropertyNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public ReadPropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Read.create(Receiver.create(), Argument.create()));
        }

        public ReadPropertyNode(ReadPropertyNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, int identifier) {
            return node.executeForeign(frame, receiver, identifier);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, long identifier) {
            return node.executeForeign(frame, receiver, identifier);
        }

        @CompilerDirectives.CompilationFinal private String identifier;

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, RubySymbol identifier) {
            if (this.identifier == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.identifier = identifier.toString().intern();
            }
            return node.executeForeign(frame, receiver, this.identifier);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, RubyString identifier) {
            return node.executeForeign(frame, receiver, slowPathToString(identifier));
        }

        @CompilerDirectives.TruffleBoundary
        private static String slowPathToString(RubyString identifier) {
            return identifier.toString();
        }

    }

    @CoreMethod(names = "write_property", isModuleFunction = true, needsSelf = false, required = 3)
    public abstract static class WritePropertyNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public WritePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Write.create(Receiver.create(), Argument.create(), Argument.create()));
        }

        public WritePropertyNode(WritePropertyNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, int identifier,  Object value) {
            return node.executeForeign(frame, receiver, identifier, value);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, long identifier,  Object value) {
            return node.executeForeign(frame, receiver, identifier, value);
        }

        @CompilerDirectives.CompilationFinal private String identifier;

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, RubySymbol identifier,  Object value) {
            if (this.identifier == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.identifier = identifier.toString().intern();
            }
            return node.executeForeign(frame, receiver, this.identifier, value);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, RubyString identifier, Object value) {
            return node.executeForeign(frame, receiver, slowPathToString(identifier), value);
        }

        @CompilerDirectives.TruffleBoundary
        private static String slowPathToString(RubyString identifier) {
            return identifier.toString();
        }

    }

    @CoreMethod(names = "unbox_value", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class UnboxValueNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public UnboxValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Unbox.create(Receiver.create()));
        }

        public UnboxValueNode(UnboxValueNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver) {
            return node.executeForeign(frame, receiver);
        }

    }
    // TODO: remove maxArgs - hits an assertion if maxArgs is removed - trying argumentsAsArray = true (CS)
    @CoreMethod(names = "execute", isModuleFunction = true, needsSelf = false, required = 1, argumentsAsArray = true)
    public abstract static class ExecuteNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public ExecuteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExecuteNode(ExecuteNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, Object[] arguments) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.node = ForeignObjectAccessNode.getAccess(Execute.create(Receiver.create(), arguments.length));
            }
            return node.executeForeign(frame, receiver, arguments);
        }

    }

    @CoreMethod(names = "size", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class GetSizeNode extends CoreMethodNode {

        @Child private ForeignObjectAccessNode node;

        public GetSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(GetSize.create(Receiver.create()));
        }

        public GetSizeNode(GetSizeNode prev) {
            this(prev.getContext(), prev.getSourceSection());
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, String receiver) {
            return receiver.length();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver) {
            return node.executeForeign(frame, receiver);
        }

    }

}
