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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.StringCachingGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;

import java.io.IOException;

@CoreClass(name = "Truffle::Interop")
public abstract class TruffleInteropNodes {

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

        @Child private Node node;

        public IsExecutableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.IS_EXECUTABLE.createNode();
        }

        @Specialization
        public boolean isExecutable(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) ForeignAccess.execute(node, frame, receiver, receiver);
        }

    }

    @CoreMethod(names = "boxed_primitive?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsBoxedPrimitiveNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public IsBoxedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.IS_BOXED.createNode();
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) ForeignAccess.execute(node, frame, receiver);
        }

    }

    @CoreMethod(names = "null?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class IsNullNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public IsNullNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.IS_NULL.createNode();
        }

        @Specialization
        public boolean isNull(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) ForeignAccess.execute(node, frame, receiver);
        }

    }

    @CoreMethod(names = "has_size_property?", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class HasSizePropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public HasSizePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.HAS_SIZE.createNode();
        }

        @Specialization
        public boolean hasSizeProperty(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) ForeignAccess.execute(node, frame, receiver);
        }

    }

    @CoreMethod(names = "read_property", isModuleFunction = true, needsSelf = false, required = 2)
    public abstract static class ReadPropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public ReadPropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.READ.createNode();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, int identifier) {
            return ForeignAccess.execute(node, frame, receiver, identifier);
        }
        
        @Specialization
        public Object executeForeign(VirtualFrame frame, String receiver, int identifier) {
            return receiver.charAt(identifier);
        }
        
        @Specialization
        public Object executeForeign(VirtualFrame frame, String receiver, long identifier) {
            return receiver.charAt((int) identifier);
        }


        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, long identifier) {
            return ForeignAccess.execute(node, frame, receiver, (int) identifier);
        }

        @CompilationFinal private String identifier;

        @Specialization(guards = "isRubySymbol(identifier)")
        public Object executeForeignSymbol(VirtualFrame frame, TruffleObject receiver, DynamicObject identifier) {
            if (this.identifier == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.identifier = Layouts.SYMBOL.getString(identifier).intern();
            }
            return ForeignAccess.execute(node, frame, receiver, this.identifier);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, DynamicObject identifier) {
            return ForeignAccess.execute(node, frame, receiver, slowPathToString(identifier));
        }

        @TruffleBoundary
        private static String slowPathToString(DynamicObject identifier) {
            assert RubyGuards.isRubyString(identifier);
            return identifier.toString();
        }

    }

    @CoreMethod(names = "write_property", isModuleFunction = true, needsSelf = false, required = 3)
    public abstract static class WritePropertyNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public WritePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.WRITE.createNode();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, int identifier,  Object value) {
            return ForeignAccess.execute(node, frame, receiver, identifier, value);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, long identifier,  Object value) {
            return ForeignAccess.execute(node, frame, receiver, identifier, value);
        }

        @CompilationFinal private String identifier;

        @Specialization(guards = "isRubySymbol(identifier)")
        public Object executeForeignSymbol(VirtualFrame frame, TruffleObject receiver, DynamicObject identifier,  Object value) {
            if (this.identifier == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.identifier = Layouts.SYMBOL.getString(identifier).intern();
            }
            return ForeignAccess.execute(node, frame, receiver, this.identifier, value);
        }

        @Specialization(guards = "isRubyString(identifier)")
        public Object executeForeignString(VirtualFrame frame, TruffleObject receiver, DynamicObject identifier, Object value) {
            return ForeignAccess.execute(node, frame, receiver, slowPathToString(identifier), value);
        }

        @TruffleBoundary
        private static String slowPathToString(DynamicObject identifier) {
            assert RubyGuards.isRubyString(identifier);
            return identifier.toString();
        }

    }

    @CoreMethod(names = "unbox_value", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class UnboxValueNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public UnboxValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.UNBOX.createNode();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver) {
            return ForeignAccess.execute(node, frame, receiver);
        }

    }

    @CoreMethod(names = "execute", isModuleFunction = true, needsSelf = false, required = 1, rest = true)
    public abstract static class ExecuteNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public ExecuteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver, Object[] args) {
            if (node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.node = Message.createExecute(args.length).createNode();
            }
            return ForeignAccess.execute(node, frame, receiver, args);
        }

    }

    @CoreMethod(names = "size", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class GetSizeNode extends CoreMethodArrayArgumentsNode {

        @Child private Node node;

        public GetSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.node = Message.GET_SIZE.createNode();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, String receiver) {
            return receiver.length();
        }

        @Specialization
        public Object executeForeign(VirtualFrame frame, TruffleObject receiver) {
            return ForeignAccess.execute(node, frame, receiver);
        }

    }

    @CoreMethod(names = "export", isModuleFunction = true, needsSelf = false, required = 2)
    public abstract static class ExportNode extends CoreMethodArrayArgumentsNode {

        public ExportNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(name)")
        public Object export(VirtualFrame frame, DynamicObject name, TruffleObject object) {
            getContext().exportObject(name, object);
            return object;
        }

        protected static String rubyStringToString(DynamicObject rubyString) {
            return rubyString.toString();
        }
    }

    @CoreMethod(names = "import", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class ImportNode extends CoreMethodArrayArgumentsNode {


        public ImportNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(name)")
        public Object importObject(DynamicObject name) {
            return getContext().importObject(name);
        }

    }

    @CoreMethod(names = "eval", isModuleFunction = true, needsSelf = false, required = 2)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class EvalNode extends CoreMethodArrayArgumentsNode {

        public EvalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isRubyString(mimeType)",
                "isRubyString(source)",
                "byteListsEqual(mimeType, cachedMimeType)",
                "byteListsEqual(source, cachedSource)"
        }, limit = "getCacheLimit()")
        public Object evalCached(
                VirtualFrame frame,
                DynamicObject mimeType,
                DynamicObject source,
                @Cached("privatizeByteList(mimeType)") ByteList cachedMimeType,
                @Cached("privatizeByteList(source)") ByteList cachedSource,
                @Cached("create(parse(mimeType, source))") DirectCallNode callNode
        ) {
            return callNode.call(frame, new Object[]{});
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(mimeType)", "isRubyString(source)"}, contains = "evalCached")
        public Object evalUncached(VirtualFrame frame, DynamicObject mimeType, DynamicObject source) {
            return parse(mimeType, source).call();
        }

        protected CallTarget parse(DynamicObject mimeType, DynamicObject source) {
            final String mimeTypeString = mimeType.toString();
            final Source sourceObject = Source.fromText(source.toString(), "(eval)").withMimeType(mimeTypeString);

            try {
                return getContext().getEnv().parse(sourceObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        protected int getCacheLimit() {
            return getContext().getOptions().EVAL_CACHE;
        }

    }

}
