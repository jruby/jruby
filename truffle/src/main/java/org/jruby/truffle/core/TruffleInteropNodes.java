/*
 * Copyright (c) 2014, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
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
import org.jruby.truffle.nodes.StringCachingGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.io.IOException;

@CoreClass(name = "Truffle::Interop")
public abstract class TruffleInteropNodes {

    // TODO CS 21-Dec-15 this shouldn't be needed any more - we can handle byte, short, float etc natively

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
        public boolean isBoxedPrimitive(VirtualFrame frame, boolean receiver) {
            return receiver;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, byte receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, short receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, long receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, float receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, double receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, CharSequence receiver) {
            return true;
        }

        @Specialization
        public boolean isBoxedPrimitive(VirtualFrame frame, TruffleObject receiver) {
            return (boolean) ForeignAccess.execute(node, frame, receiver);
        }

        @Specialization(guards = {"!isTruffleObject(receiver)", "!isJavaCharSequence(receiver)"})
        public boolean isBoxedPrimitive(VirtualFrame frame, Object receiver) {
            return false;
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
    @ImportStatic(StringCachingGuards.class)
    public abstract static class ReadPropertyNode extends CoreMethodArrayArgumentsNode {

        public ReadPropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // TODO CS 21-Dec-15 should Truffle provide foreign access for strings?

        @Specialization
        public Object readProperty(String receiver, int identifier) {
            return receiver.charAt(identifier);
        }

        @Specialization
        public Object readProperty(String receiver, long identifier) {
            return receiver.charAt((int) identifier);
        }

        @Specialization(guards = {"!isRubySymbol(identifier)", "!isRubyString(identifier)"})
        public Object readProperty(VirtualFrame frame,
                                   TruffleObject receiver,
                                   Object identifier,
                                   @Cached("createReadNode()") Node readNode) {
            return ForeignAccess.execute(readNode, frame, receiver, identifier);
        }

        @Specialization(guards = {"isRubySymbol(identifier)", "identifier == cachedIdentifier"})
        public Object readProperty(VirtualFrame frame,
                                   TruffleObject receiver,
                                   DynamicObject identifier,
                                   @Cached("identifier") DynamicObject cachedIdentifier,
                                   @Cached("identifier.toString()") String identifierString,
                                   @Cached("createReadNode()") Node readNode) {
            return ForeignAccess.execute(readNode, frame, receiver, identifierString);
        }

        @Specialization(guards = {"isRubyString(identifier)", "ropesEqual(identifier, cachedIdentifier)"})
        public Object readProperty(VirtualFrame frame,
                                   TruffleObject receiver,
                                   DynamicObject identifier,
                                   @Cached("privatizeRope(identifier)") Rope cachedIdentifier,
                                   @Cached("identifier.toString()") String identifierString,
                                   @Cached("createReadNode()") Node readNode) {
            return ForeignAccess.execute(readNode, frame, receiver, identifierString);
        }

        protected static Node createReadNode() {
            return Message.READ.createNode();
        }

        protected int getCacheLimit() {
            return getContext().getOptions().EVAL_CACHE;
        }

    }

    @CoreMethod(names = "write_property", isModuleFunction = true, needsSelf = false, required = 3)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class WritePropertyNode extends CoreMethodArrayArgumentsNode {

        public WritePropertyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"!isRubySymbol(identifier)", "!isRubyString(identifier)"})
        public Object writeProperty(VirtualFrame frame,
                                    TruffleObject receiver,
                                    Object identifier,
                                    Object value,
                                    @Cached("createWriteNode()") Node writeNode) {
            return ForeignAccess.execute(writeNode, frame, receiver, identifier, value);
        }

        @Specialization(guards = {"isRubySymbol(identifier)", "identifier == cachedIdentifier"})
        public Object writeProperty(VirtualFrame frame,
                                    TruffleObject receiver,
                                    DynamicObject identifier,
                                    Object value,
                                    @Cached("identifier") DynamicObject cachedIdentifier,
                                    @Cached("identifier.toString()") String identifierString,
                                    @Cached("createWriteNode()") Node writeNode) {
            return ForeignAccess.execute(writeNode, frame, receiver, identifierString, value);
        }

        @Specialization(guards = {"isRubyString(identifier)", "ropesEqual(identifier, cachedIdentifier)"})
        public Object writeProperty(VirtualFrame frame,
                                    TruffleObject receiver,
                                    DynamicObject identifier,
                                    Object value,
                                    @Cached("privatizeRope(identifier)") Rope cachedIdentifier,
                                    @Cached("identifier.toString()") String identifierString,
                                    @Cached("createWriteNode()") Node writeNode) {
            return ForeignAccess.execute(writeNode, frame, receiver, identifierString, value);
        }

        protected static Node createWriteNode() {
            return Message.WRITE.createNode();
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
        public boolean unbox(VirtualFrame frame, boolean receiver) {
            return receiver;
        }

        @Specialization
        public byte unbox(VirtualFrame frame, byte receiver) {
            return receiver;
        }

        @Specialization
        public short unbox(VirtualFrame frame, short receiver) {
            return receiver;
        }

        @Specialization
        public long unbox(VirtualFrame frame, long receiver) {
            return receiver;
        }

        @Specialization
        public float unbox(VirtualFrame frame, float receiver) {
            return receiver;
        }

        @Specialization
        public double unbox(VirtualFrame frame, double receiver) {
            return receiver;
        }

        @Specialization
        public DynamicObject executeForeign(VirtualFrame frame, CharSequence receiver) {
            // TODO CS-21-Dec-15 this shouldn't be needed - we need to convert j.l.String to Ruby's String automatically

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), StringOperations.ropeFromByteList(ByteList.create(receiver)), null);
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
                "ropesEqual(mimeType, cachedMimeType)",
                "ropesEqual(source, cachedSource)"
        }, limit = "getCacheLimit()")
        public Object evalCached(
                VirtualFrame frame,
                DynamicObject mimeType,
                DynamicObject source,
                @Cached("privatizeRope(mimeType)") Rope cachedMimeType,
                @Cached("privatizeRope(source)") Rope cachedSource,
                @Cached("create(parse(mimeType, source))") DirectCallNode callNode
        ) {
            return callNode.call(frame, new Object[]{});
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(mimeType)", "isRubyString(source)"}, contains = "evalCached")
        public Object evalUncached(DynamicObject mimeType, DynamicObject source) {
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

    // TODO CS-21-Dec-15 this shouldn't be needed - we need to convert j.l.String to Ruby's String automatically

    @CoreMethod(names = "java_string_to_ruby", isModuleFunction = true, needsSelf = false, required = 1)
    public abstract static class JavaStringToRubyNode extends CoreMethodArrayArgumentsNode {

        public JavaStringToRubyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public DynamicObject javaStringToRuby(String string) {
            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), StringOperations.ropeFromByteList(ByteList.create(string), StringSupport.CR_UNKNOWN), null);
        }

    }

}
