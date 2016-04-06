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
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.SpawnFileAction;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyGC;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.hash.BucketsStrategy;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.MutableRope;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.platform.Graal;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.tools.simpleshell.SimpleShell;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.unsafe.UnsafeHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CoreClass(name = "Truffle::Primitive")
public abstract class TrufflePrimitiveNodes {

    @CoreMethod(unsafeNeedsAudit = true, names = "binding_of_caller", isModuleFunction = true)
    public abstract static class BindingOfCallerNode extends CoreMethodArrayArgumentsNode {

        public BindingOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
                    if (frameCount.get() == 1) {
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

    @CoreMethod(unsafeNeedsAudit = true, names = "source_of_caller", isModuleFunction = true)
    public abstract static class SourceOfCallerNode extends CoreMethodArrayArgumentsNode {

        public SourceOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject sourceOfCaller() {
            final Memo<Integer> frameCount = new Memo<>(0);

            final String source = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<String>() {

                @Override
                public String visitFrame(FrameInstance frameInstance) {
                    if (frameCount.get() == 1) {
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

    @CoreMethod(unsafeNeedsAudit = true, names = "gc_count", onSingleton = true)
    public abstract static class GCCountNode extends CoreMethodArrayArgumentsNode {

        public GCCountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int gcCount() {
            return RubyGC.getCollectionCount();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "gc_time", onSingleton = true)
    public abstract static class GCTimeNode extends CoreMethodArrayArgumentsNode {

        public GCTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public long gcTime() {
            return RubyGC.getCollectionTime();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "assert_constant", onSingleton = true, required = 1)
    public abstract static class AssertConstantNode extends CoreMethodArrayArgumentsNode {

        public AssertConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject assertConstant(Object value) {
            throw new RaiseException(coreLibrary().runtimeError("Truffle::Primitive.assert_constant can only be called lexically", this));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "assert_not_compiled", onSingleton = true)
    public abstract static class AssertNotCompiledNode extends CoreMethodArrayArgumentsNode {

        public AssertNotCompiledNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject assertNotCompiled() {
            throw new RaiseException(coreLibrary().runtimeError("Truffle::Primitive.assert_not_compiled can only be called lexically", this));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodArrayArgumentsNode {

        public JavaClassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject javaClassOf(Object value) {
            return createString(StringOperations.encodeRope(value.getClass().getSimpleName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "dump_string", onSingleton = true, required = 1)
    public abstract static class DumpStringNode extends CoreMethodArrayArgumentsNode {

        public DumpStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject dumpString(DynamicObject string) {
            final StringBuilder builder = new StringBuilder();

            final ByteList byteList = StringOperations.getByteListReadOnly(string);

            for (int i = 0; i < byteList.length(); i++) {
                builder.append(String.format("\\x%02x", byteList.get(i)));
            }

            return createString(StringOperations.encodeRope(builder.toString(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "graal?", onSingleton = true)
    public abstract static class GraalNode extends CoreMethodArrayArgumentsNode {

        public GraalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean graal() {
            return Graal.isGraal();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodArrayArgumentsNode {

        public SubstrateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean substrate() {
            return TruffleOptions.AOT;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "graal_version", onSingleton = true)
    public abstract static class GraalVersionNode extends CoreMethodArrayArgumentsNode {

        public GraalVersionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject graalVersion() {
            return createString(StringOperations.encodeRope(System.getProperty("graal.version", "unknown"), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "simple_shell", onSingleton = true)
    public abstract static class SimpleShellNode extends CoreMethodArrayArgumentsNode {

        public SimpleShellNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject simpleShell() {
            new SimpleShell(getContext()).run(getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize(), this);
            return nil();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "coverage_result", onSingleton = true)
    public abstract static class CoverageResultNode extends CoreMethodArrayArgumentsNode {

        public CoverageResultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject coverageResult() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            final Map<Object, Object> converted = new HashMap<>();

            for (Map.Entry<Source, Long[]> source : getContext().getCoverageTracker().getCounts().entrySet()) {
                final Object[] store = lineCountsStore(source.getValue());
                final DynamicObject array = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);

                if (source.getKey().getPath() != null) {
                    converted.put(createString(StringOperations.encodeRope(source.getKey().getPath(), UTF8Encoding.INSTANCE)), array);
                }
            }

            return BucketsStrategy.create(getContext(), converted.entrySet(), false);
        }

        private Object[] lineCountsStore(Long[] array) {
            final Object[] store = new Object[array.length];

            for (int n = 0; n < array.length; n++) {
                if (array[n] == null) {
                    store[n] = coreLibrary().getNilObject();
                } else {
                    store[n] = array[n];
                }
            }

            return store;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "coverage_start", onSingleton = true)
    public abstract static class CoverageStartNode extends CoreMethodArrayArgumentsNode {

        public CoverageStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject coverageStart() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            getContext().getEnv().instrumenter().install(getContext().getCoverageTracker());
            return coreLibrary().getNilObject();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "attach", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodArrayArgumentsNode {

        public AttachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public DynamicObject attach(DynamicObject file, int line, DynamicObject block) {
            return Layouts.HANDLE.createHandle(coreLibrary().getHandleFactory(), getContext().getAttachmentsManager().attach(file.toString(), line, block));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "detach", onSingleton = true, required = 1)
    public abstract static class DetachNode extends CoreMethodArrayArgumentsNode {

        public DetachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isHandle(handle)")
        public DynamicObject detach(DynamicObject handle) {
            final Instrument instrument = (Instrument) Layouts.HANDLE.getObject(handle);
            instrument.dispose();
            return coreLibrary().getNilObject();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "debug_print", onSingleton = true, required = 1)
    public abstract static class DebugPrintNode extends CoreMethodArrayArgumentsNode {

        public DebugPrintNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrint(DynamicObject string) {
            System.err.println(string.toString());
            return nil();
        }

    }

    @CoreMethod(names = "safe_puts", onSingleton = true, required = 1, unsafe = UnsafeGroup.SAFE_PUTS)
    public abstract static class SafePutsNode extends CoreMethodArrayArgumentsNode {

        public SafePutsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject safePuts(DynamicObject string) {
            for (char c : string.toString().toCharArray()) {
                if (isAsciiPrintable(c)) {
                    System.out.print(c);
                } else {
                    System.out.print('?');
                }
            }

            System.out.println();

            return nil();
        }

        private boolean isAsciiPrintable(char c) {
            return c >= 32 && c <= 126 || c == '\n' || c == '\t';
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "convert_to_mutable_rope", onSingleton = true, required = 1)
    public abstract static class ConvertToMutableRope extends CoreMethodArrayArgumentsNode {

        public ConvertToMutableRope(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject convertToMutableRope(DynamicObject string) {
            final MutableRope mutableRope = new MutableRope(StringOperations.rope(string));
            StringOperations.setRope(string, mutableRope);

            return string;
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "debug_print_rope", onSingleton = true, required = 1, optional = 1)
    public abstract static class DebugPrintRopeNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.DebugPrintRopeNode debugPrintRopeNode;

        public DebugPrintRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            debugPrintRopeNode = RopeNodesFactory.DebugPrintRopeNodeGen.create(context, sourceSection, null, null, null);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrintDefault(DynamicObject string, NotProvided printString) {
            return debugPrint(string, true);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public DynamicObject debugPrint(DynamicObject string, boolean printString) {
            System.err.println("Legend: ");
            System.err.println("BN = Bytes Null? (byte[] not yet populated)");
            System.err.println("BL = Byte Length");
            System.err.println("CL = Character Length");
            System.err.println("CR = Code Range");
            System.err.println("O = Offset (SubstringRope only)");
            System.err.println("D = Depth");
            System.err.println("LD = Left Depth (ConcatRope only)");
            System.err.println("RD = Right Depth (ConcatRope only)");

            return debugPrintRopeNode.executeDebugPrint(StringOperations.rope(string), 0, printString);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "flatten_rope", onSingleton = true, required = 1)
    public abstract static class FlattenRopeNode extends CoreMethodArrayArgumentsNode {

        public FlattenRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject flattenRope(DynamicObject string,
                                         @Cached("create(getContext(), getSourceSection())") RopeNodes.FlattenNode flattenNode) {
            final Rope flattened = flattenNode.executeFlatten(StringOperations.rope(string));

            return createString(flattened);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "jruby_home_directory", onSingleton = true)
    public abstract static class JRubyHomeDirectoryNode extends CoreMethodNode {

        public JRubyHomeDirectoryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject jrubyHomeDirectory() {
            return createString(StringOperations.encodeRope(getContext().getJRubyRuntime().getJRubyHome(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "jruby_home_directory_protocol", onSingleton = true)
    public abstract static class JRubyHomeDirectoryProtocolNode extends CoreMethodNode {

        public JRubyHomeDirectoryProtocolNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject jrubyHomeDirectoryProtocol() {
            String home = getContext().getJRubyRuntime().getJRubyHome();

            if (home.startsWith("uri:classloader:")) {
                home = home.substring("uri:classloader:".length());

                while (home.startsWith("/")) {
                    home = home.substring(1);
                }

                home = SourceLoader.JRUBY_SCHEME + "/" + home;
            }

            return createString(StringOperations.encodeRope(home, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "host_os", onSingleton = true)
    public abstract static class HostOSNode extends CoreMethodNode {

        public HostOSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject hostOS() {
            return createString(StringOperations.encodeRope(RbConfigLibrary.getOSName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "at_exit", isModuleFunction = true, needsBlock = true, required = 1)
    public abstract static class AtExitSystemNode extends CoreMethodArrayArgumentsNode {

        public AtExitSystemNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object atExit(boolean always, DynamicObject block) {
            getContext().getAtExitManager().add(block, always);
            return nil();
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "install_rubinius_primitive", isModuleFunction = true, required = 1)
    public abstract static class InstallRubiniusPrimitiveNode extends CoreMethodArrayArgumentsNode {

        public InstallRubiniusPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyMethod(rubyMethod)")
        public Object installRubiniusPrimitive(DynamicObject rubyMethod) {
            String name = Layouts.METHOD.getMethod(rubyMethod).getName();
            getContext().getRubiniusPrimitiveManager().installPrimitive(name, rubyMethod);
            return nil();
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "fixnum_lower", isModuleFunction = true, required = 1)
    public abstract static class FixnumLowerPrimitiveNode extends UnaryCoreMethodNode {

        public FixnumLowerPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int lower(int value) {
            return value;
        }

        @Specialization(guards = "canLower(value)")
        public int lower(long value) {
            return (int) value;
        }

        @Specialization(guards = "!canLower(value)")
        public long lowerFails(long value) {
            return value;
        }

        protected static boolean canLower(long value) {
            return CoreLibrary.fitsIntoInteger(value);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "synchronized", isModuleFunction = true, required = 1, needsBlock = true)
    public abstract static class SynchronizedPrimitiveNode extends YieldingCoreMethodNode {

        public SynchronizedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // We must not allow to synchronize on boxed primitives.
        @Specialization
        public Object synchronize(VirtualFrame frame, DynamicObject self, DynamicObject block) {
            synchronized (self) {
                return yield(frame, block);
            }
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "full_memory_barrier", isModuleFunction = true)
    public abstract static class FullMemoryBarrierPrimitiveNode extends CoreMethodNode {

        public FullMemoryBarrierPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object fullMemoryBarrier() {
            if (UnsafeHolder.SUPPORTS_FENCES) {
                UnsafeHolder.fullFence();
            } else {
                throw new UnsupportedOperationException();
            }
            return nil();
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "print_backtrace", onSingleton = true)
    public abstract static class PrintBacktraceNode extends CoreMethodNode {

        public PrintBacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = "ast", onSingleton = true, required = 1)
    public abstract static class ASTNode extends CoreMethodArrayArgumentsNode {

        public ASTNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = "object_type_of", onSingleton = true, required = 1)
    public abstract static class ObjectTypeOfNode extends CoreMethodArrayArgumentsNode {

        public ObjectTypeOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject objectTypeOf(DynamicObject value) {
            return getSymbol(value.getShape().getObjectType().getClass().getSimpleName());
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "spawn_process", onSingleton = true, required = 3)
    public abstract static class SpawnProcessNode extends CoreMethodArrayArgumentsNode {

        public SpawnProcessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
                throw new RaiseException(coreLibrary().errnoError(getContext().getNativePlatform().getPosix().errno(), this));
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

    @CoreMethod(unsafeNeedsAudit = true, names = "context", onSingleton = true)
    public abstract static class ContextNode extends CoreMethodArrayArgumentsNode {

        public ContextNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyContext context() {
            return getContext();
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "load", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LoadNode extends CoreMethodArrayArgumentsNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(VirtualFrame frame, DynamicObject file, boolean wrap, @Cached("create()") IndirectCallNode callNode) {
            if (wrap) {
                throw new UnsupportedOperationException();
            }

            try {
                final RubyRootNode rootNode = getContext().getCodeLoader().parse(getContext().getSourceCache().getSource(StringOperations.getString(getContext(), file)), UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, this);
                final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, rootNode, null, getContext().getCoreLibrary().getMainObject());
                callNode.call(frame, deferredCall.getCallTarget(), deferredCall.getArguments());
            } catch (IOException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().loadErrorCannotLoad(file.toString(), this));
            }

            return true;
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(VirtualFrame frame, DynamicObject file, NotProvided wrap, @Cached("create()") IndirectCallNode callNode) {
            return load(frame, file, false, callNode);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "run_jruby_root", onSingleton = true)
    public abstract static class RunJRubyRootNode extends CoreMethodArrayArgumentsNode {

        public RunJRubyRootNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object runJRubyRootNode(VirtualFrame frame, @Cached("create()")IndirectCallNode callNode) {
            coreLibrary().getGlobalVariablesObject().define(
                    "$0",
                    StringOperations.createString(getContext(),
                            ByteList.create(getContext().getJRubyInterop().getArg0())));

            String inputFile = getContext().getInitialJRubyRootNode().getPosition().getFile();

            final Source source;

            try {
                if (!inputFile.equals("-e")) {
                    inputFile = new File(inputFile).getCanonicalPath();
                }

                source = getContext().getSourceCache().getSource(inputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final RubyRootNode rootNode = getContext().getCodeLoader().parse(
                    source,
                    UTF8Encoding.INSTANCE,
                    ParserContext.TOP_LEVEL_FIRST,
                    null,
                    true,
                    null);

            final CodeLoader.DeferredCall deferredCall = getContext().getCodeLoader().prepareExecute(
                    ParserContext.TOP_LEVEL,
                    DeclarationContext.TOP_LEVEL,
                    rootNode,
                    null,
                    coreLibrary().getMainObject());

            return callNode.call(frame, deferredCall.getCallTarget(), deferredCall.getArguments());
        }
    }

    /*
     * Truffle::Primitive.create_simple_string creates a string 'test' without any part of the string escaping. Useful
     * for testing compilation of String becuase most other ways to construct a string can currently escape.
     */

    @CoreMethod(unsafeNeedsAudit = true, names = "create_simple_string", onSingleton = true)
    public abstract static class CreateSimpleStringNode extends CoreMethodArrayArgumentsNode {

        public CreateSimpleStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject createSimpleString() {
            return createString(RopeOperations.create(new byte[]{'t', 'e', 's', 't'}, UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "logical_processors", onSingleton = true)
    public abstract static class LogicalProcessorsNode extends CoreMethodNode {

        public LogicalProcessorsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int logicalProcessors() {
            return Runtime.getRuntime().availableProcessors();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "original_argv", onSingleton = true)
    public abstract static class OriginalArgvNode extends CoreMethodNode {

        public OriginalArgvNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject originalArgv() {
            final String[] argv = getContext().getJRubyInterop().getArgv();
            final Object[] array = new Object[argv.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(argv[n], UTF8Encoding.INSTANCE));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), array, array.length);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "original_load_path", onSingleton = true)
    public abstract static class OriginalLoadPathNode extends CoreMethodNode {

        public OriginalLoadPathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject originalLoadPath() {
            final String[] path = getContext().getJRubyInterop().getOriginalLoadPath();
            final Object[] array = new Object[path.length];

            for (int n = 0; n < array.length; n++) {
                array[n] = StringOperations.createString(getContext(), StringOperations.encodeRope(path[n], UTF8Encoding.INSTANCE));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), array, array.length);
        }

    }

    @CoreMethod(names = "io_safe?", onSingleton = true)
    public abstract static class IsIOSafeNode extends CoreMethodNode {

        public IsIOSafeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean ioSafe() {
            return getContext().getOptions().PLATFORM_SAFE_IO;
        }

    }

}
