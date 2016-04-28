/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import jnr.posix.SpawnFileAction;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.array.ArrayStrategy;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.tools.simpleshell.SimpleShell;
import org.jruby.util.Memo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CoreClass(name = "Truffle")
public abstract class TruffleNodes {

    @CoreMethod(names = "binding_of_caller", isModuleFunction = true)
    public abstract static class BindingOfCallerNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject bindingOfCaller() {
            /*
             * When you use this method you're asking for the binding of the caller at the call site. When we get into
             * this method, that is then the binding of the caller of the caller.
             */

            final Memo<Integer> frameCount = new Memo<>(0);

            final MaterializedFrame frame = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<MaterializedFrame>() {

                @Override
                public MaterializedFrame visitFrame(FrameInstance frameInstance) {
                    if (frameCount.get() == 2) {
                        return frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE, false).materialize();
                    } else {
                        frameCount.set(frameCount.get() + 1);
                        return null;
                    }
                }

            });

            if (frame == null) {
                return nil();
            }

            return BindingNodes.createBinding(getContext(), frame);
        }

    }

    @CoreMethod(names = "source_of_caller", isModuleFunction = true)
    public abstract static class SourceOfCallerNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject sourceOfCaller() {
            final Memo<Integer> frameCount = new Memo<>(0);

            final String source = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<String>() {

                @Override
                public String visitFrame(FrameInstance frameInstance) {
                    if (frameCount.get() == 2) {
                        return frameInstance.getCallNode().getEncapsulatingSourceSection().getSource().getName();
                    } else {
                        frameCount.set(frameCount.get() + 1);
                        return null;
                    }
                }

            });

            if (source == null) {
                return nil();
            }

            return createString(StringOperations.encodeRope(source, UTF8Encoding.INSTANCE));
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

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true, required = 1, unsafe = UnsafeGroup.AT_EXIT)
    public abstract static class AtExitSystemNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object atExit(boolean always, DynamicObject block) {
            getContext().getAtExitManager().add(block, always);
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

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), array.toArray(), array.size());
        }

    }

    @CoreMethod(names = "object_type_of", onSingleton = true, required = 1)
    public abstract static class ObjectTypeOfNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject objectTypeOf(DynamicObject value) {
            return getSymbol(value.getShape().getObjectType().getClass().getSimpleName());
        }
    }

    @CoreMethod(names = "spawn_process", onSingleton = true, required = 3, unsafe = UnsafeGroup.PROCESSES)
    public abstract static class SpawnProcessNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = {
                "isRubyString(command)",
                "isRubyArray(arguments)",
                "isRubyArray(environmentVariables)" })
        public int spawn(DynamicObject command,
                         DynamicObject arguments,
                         DynamicObject environmentVariables) {

            final long longPid = call(
                    StringOperations.getString(getContext(), command),
                    toStringArray(arguments),
                    toStringArray(environmentVariables));
            assert longPid <= Integer.MAX_VALUE;
            // VMWaitPidPrimitiveNode accepts only int
            final int pid = (int) longPid;

            if (pid == -1) {
                // TODO (pitr 07-Sep-2015): needs compatibility improvements
                throw new RaiseException(coreExceptions().errnoError(getContext().getNativePlatform().getPosix().errno(), this));
            }

            return pid;
        }

        private String[] toStringArray(DynamicObject rubyStrings) {
            final int size = Layouts.ARRAY.getSize(rubyStrings);
            final Object[] unconvertedStrings = ArrayOperations.toObjectArray(rubyStrings);
            final String[] strings = new String[size];

            for (int i = 0; i < size; i++) {
                assert Layouts.STRING.isString(unconvertedStrings[i]);
                strings[i] = StringOperations.getString(getContext(), (DynamicObject) unconvertedStrings[i]);
            }

            return strings;
        }

        @TruffleBoundary
        private long call(String command, String[] arguments, String[] environmentVariables) {
            // TODO (pitr 04-Sep-2015): only simple implementation, does not support file actions or other options
            return getContext().getNativePlatform().getPosix().posix_spawnp(
                    command,
                    Collections.<SpawnFileAction>emptyList(),
                    Arrays.asList(arguments),
                    Arrays.asList(environmentVariables));

        }
    }


    @CoreMethod(names = "load", isModuleFunction = true, required = 1, optional = 1, unsafe = UnsafeGroup.LOAD)
    public abstract static class LoadNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(file)")
        public boolean load(VirtualFrame frame, DynamicObject file, boolean wrap, @Cached("create()") IndirectCallNode callNode) {
            if (wrap) {
                throw new UnsupportedOperationException();
            }

            try {
                final RubyRootNode rootNode = getContext().getCodeLoader().parse(getContext().getSourceCache().getSource(StringOperations.getString(getContext(), file)), UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, this);
                final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, rootNode, null, getContext().getCoreLibrary().getMainObject());
                deferredCall.call(frame, callNode);
            } catch (IOException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().loadErrorCannotLoad(file.toString(), this));
            }

            return true;
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(VirtualFrame frame, DynamicObject file, NotProvided wrap, @Cached("create()") IndirectCallNode callNode) {
            return load(frame, file, false, callNode);
        }
    }

    @CoreMethod(names = "array_storage", onSingleton = true, required = 1)
    public abstract static class ArrayStorageNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyArray(array)")
        public DynamicObject arrayStorage(DynamicObject array) {
            String storage = ArrayStrategy.of(array).toString();
            return StringOperations.createString(getContext(), StringOperations.createRope(storage, USASCIIEncoding.INSTANCE));
        }

    }

}
