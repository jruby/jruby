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

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.interop.TruffleGlobalScope;
import com.oracle.truffle.interop.messages.Argument;
import com.oracle.truffle.interop.messages.Execute;
import com.oracle.truffle.interop.messages.GetSize;
import com.oracle.truffle.interop.messages.HasSize;
import com.oracle.truffle.interop.messages.IsBoxed;
import com.oracle.truffle.interop.messages.IsExecutable;
import com.oracle.truffle.interop.messages.IsNull;
import com.oracle.truffle.interop.messages.Read;
import com.oracle.truffle.interop.messages.Receiver;
import com.oracle.truffle.interop.messages.Unbox;
import com.oracle.truffle.interop.messages.Write;
import com.oracle.truffle.interop.node.ForeignObjectAccessNode;


@CoreClass(name = "Truffle::Interop")
public abstract class TruffleInteropNodes {

	private static TruffleGlobalScope globalScope;
	
	public static void setGlobalScope(TruffleGlobalScope globalScope) {
		TruffleInteropNodes.globalScope = globalScope;
	}
	
	public static TruffleGlobalScope getGlobalScope() {
		return globalScope;
	}
	
    @CoreMethod(names = "interop_to_ruby_primitive", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class InteropToRubyNode extends CoreMethodArrayArgumentsNode {

        public InteropToRubyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class IsExecutableNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public IsExecutableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsExecutable.create(Receiver.create()));
        }

        @Specialization
        public boolean isExecutable(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "boxed_primitive?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsBoxedPrimitiveNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public IsBoxedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsBoxed.create(Receiver.create()));
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "null?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsNullNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public IsNullNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(IsNull.create(Receiver.create()));
        }

        @Specialization
        public boolean isNull(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "has_size_property?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class HasSizePropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public HasSizePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(HasSize.create(Receiver.create()));
        }

        @Specialization
        public boolean hasSizeProperty(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) node.executeForeign(frame, receiver);
        }

    }

    @CoreMethod(names = "read_property", isModuleFunction = true, needsSelf = false, required = 2)
    public abstract static class ReadPropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public ReadPropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Read.create(Receiver.create(), Argument.create()));
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, int identifier) {
            return node.executeForeign(frame, receiver, identifier);
        }
        
        @Specialization
        public Object executeForeign(VirtualFrame frame, String receiver, int identifier) {
            return receiver.charAt(identifier);
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
    public abstract static class WritePropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public WritePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Write.create(Receiver.create(), Argument.create(), Argument.create()));
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
    public abstract static class UnboxValueNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public UnboxValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(Unbox.create(Receiver.create()));
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver) {
            return node.executeForeign(frame, receiver);
        }

    }
    // TODO: remove maxArgs - hits an assertion if maxArgs is removed - trying argumentsAsArray = true (CS)
    @CoreMethod(names = "execute", isModuleFunction = true, needsSelf = false, required = 1, argumentsAsArray = true)
    public abstract static class ExecuteNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public ExecuteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class GetSizeNode extends CoreMethodArrayArgumentsNode {

        @Child private ForeignObjectAccessNode node;

        public GetSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = ForeignObjectAccessNode.getAccess(GetSize.create(Receiver.create()));
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
    
    @CoreMethod(names = "export", isModuleFunction = true, needsSelf = false, required = 2)
    public abstract static class ExportNode extends CoreMethodArrayArgumentsNode {


        public ExportNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "name == cachedName")
        public Object export(VirtualFrame frame, RubyString name,  TruffleObject object, @Cached("name") RubyString cachedName, @Cached("rubyStringToString(cachedName)") String stringName, @Cached("getGlobal()") TruffleGlobalScope global) {
            global.exportTruffleObject(stringName, object);
            return object;
        }
        
        @Specialization
        public Object export(VirtualFrame frame, RubyString name,  TruffleObject object) {
        	globalScope.exportTruffleObject(name.toString(), object);
            return object;
        }

        protected static String rubyStringToString(RubyString rubyString) {
        	return rubyString.toString();
        }
        
        protected TruffleGlobalScope getGlobal() {
        	return globalScope;
        }

    }
    
    @CoreMethod(names = "import", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class ImportNode extends CoreMethodArrayArgumentsNode {


        public ImportNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "name == cachedName")
        public TruffleObject importObject(VirtualFrame frame, RubyString name,  @Cached("name") RubyString cachedName, @Cached("getSlot(cachedName)") FrameSlot slot, @Cached("getGlobal()") TruffleGlobalScope global) {
            return global.getTruffleObject(slot);
        }
        
        @Specialization
        public Object importObject(VirtualFrame frame, RubyString name) {
            return getGlobal().getTruffleObject(getSlot(name));
        }

        protected FrameSlot getSlot(RubyString rubyString) {
        	return globalScope.getFrameSlot(rubyString.toString());
        }
        
        protected TruffleGlobalScope getGlobal() {
        	return globalScope;
        }

    }
}
