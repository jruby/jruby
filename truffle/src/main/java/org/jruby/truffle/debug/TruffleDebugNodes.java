/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.debug;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.core.array.ArrayStrategy;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.shared.SharedObjects;
import org.jruby.truffle.language.yield.YieldNode;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.tools.simpleshell.SimpleShell;

import java.util.ArrayList;
import java.util.List;

@CoreClass("Truffle::Debug")
public abstract class TruffleDebugNodes {

    @CoreMethod(names = "break_handle", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class BreakNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public DynamicObject setBreak(DynamicObject file, int line, final DynamicObject block) {
            final String fileString = StringOperations.decodeUTF8(file);

            final SourceSectionFilter filter = SourceSectionFilter.newBuilder()
                    .mimeTypeIs(RubyLanguage.MIME_TYPE)
                    .sourceIs(source -> source != null && source.getPath() != null && source.getPath().equals(fileString))
                    .lineIs(line)
                    .tagIs(StandardTags.StatementTag.class)
                    .build();

            final EventBinding<?> breakpoint = getContext().getInstrumenter().attachFactory(filter,
                    eventContext -> new ExecutionEventNode() {

                        @Child private YieldNode yieldNode = new YieldNode();

                        @Override
                        protected void onEnter(VirtualFrame frame) {
                            yieldNode.dispatch(frame, block, Layouts.BINDING.createBinding(getContext().getCoreLibrary().getBindingFactory(), frame.materialize()));
                        }

                    });

            return Layouts.HANDLE.createHandle(coreLibrary().getHandleFactory(), breakpoint);
        }

    }

    @CoreMethod(names = "remove_handle", onSingleton = true, required = 1)
    public abstract static class RemoveNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isHandle(handle)")
        public DynamicObject remove(DynamicObject handle) {
            EventBinding.class.cast(Layouts.HANDLE.getObject(handle)).dispose();
            return nil();
        }

    }

    @CoreMethod(names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject javaClassOf(Object value) {
            return createString(StringOperations.encodeRope(value.getClass().getSimpleName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "simple_shell", onSingleton = true, unsafe = UnsafeGroup.IO)
    public abstract static class SimpleShellNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject simpleShell() {
            new SimpleShell(getContext()).run(getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize(), this);
            return nil();
        }

    }

    @CoreMethod(names = "print_backtrace", onSingleton = true, unsafe = UnsafeGroup.IO)
    public abstract static class PrintBacktraceNode extends CoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject printBacktrace() {
            final List<String> rubyBacktrace = BacktraceFormatter.createDefaultFormatter(getContext())
                    .formatBacktrace(getContext(), null, getContext().getCallStack().getBacktrace(this));

            for (String line : rubyBacktrace) {
                System.err.println(line);
            }

            return nil();
        }

    }

    @CoreMethod(names = "ast", onSingleton = true, required = 1)
    public abstract static class ASTNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyMethod(method)")
        public DynamicObject astMethod(DynamicObject method) {
            return ast(Layouts.METHOD.getMethod(method));
        }

        @Specialization(guards = "isRubyUnboundMethod(method)")
        public DynamicObject astUnboundMethod(DynamicObject method) {
            return ast(Layouts.UNBOUND_METHOD.getMethod(method));
        }

        @Specialization(guards = "isRubyProc(proc)")
        public DynamicObject astProc(DynamicObject proc) {
            return ast(Layouts.PROC.getMethod(proc));
        }

        @TruffleBoundary
        private DynamicObject ast(InternalMethod method) {
            if (method.getCallTarget() instanceof RootCallTarget) {
                return ast(((RootCallTarget) method.getCallTarget()).getRootNode());
            } else {
                return nil();
            }
        }

        private DynamicObject ast(Node node) {
            if (node == null) {
                return nil();
            }

            final List<Object> array = new ArrayList<>();

            array.add(getSymbol(node.getClass().getSimpleName()));

            for (Node child : node.getChildren()) {
                array.add(ast(child));
            }

            return createArray(array.toArray(), array.size());
        }

    }

    @CoreMethod(names = "object_type_of", onSingleton = true, required = 1)
    public abstract static class ObjectTypeOfNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject objectTypeOf(DynamicObject value) {
            return getSymbol(value.getShape().getObjectType().getClass().getSimpleName());
        }
    }

    @CoreMethod(names = "shape", onSingleton = true, required = 1)
    public abstract static class ShapeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject shape(DynamicObject object) {
            return createString(StringOperations.encodeRope(object.getShape().toString(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "array_storage", onSingleton = true, required = 1)
    public abstract static class ArrayStorageNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyArray(array)")
        public DynamicObject arrayStorage(DynamicObject array) {
            String storage = ArrayStrategy.of(array).toString();
            return StringOperations.createString(getContext(), StringOperations.encodeRope(storage, USASCIIEncoding.INSTANCE));
        }

    }

    @CoreMethod(names = "shared?", onSingleton = true, required = 1)
    public abstract static class IsSharedNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean isShared(DynamicObject object) {
            return SharedObjects.isShared(object);
        }

    }

}
