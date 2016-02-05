/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.SpawnFileAction;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyGC;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.truffle.core.rope.*;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyCallStack;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.backtrace.BacktraceInterleaver;
import org.jruby.truffle.cext.CExtManager;
import org.jruby.truffle.cext.CExtSubsystem;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.core.hash.BucketsStrategy;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.tools.SimpleShell;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.unsafe.UnsafeHolder;

import java.io.IOException;
import java.util.*;

@CoreClass(name = "Truffle::Primitive")
public abstract class TrufflePrimitiveNodes {

    @CoreMethod(names = "binding_of_caller", isModuleFunction = true)
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

    @CoreMethod(names = "source_of_caller", isModuleFunction = true)
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

    @CoreMethod(names = "gc_count", onSingleton = true)
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

    @CoreMethod(names = "gc_time", onSingleton = true)
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

    @CoreMethod(names = "assert_constant", onSingleton = true, required = 1)
    public abstract static class AssertConstantNode extends CoreMethodArrayArgumentsNode {

        public AssertConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject assertConstant(Object value) {
            throw new RaiseException(getContext().getCoreLibrary().runtimeError("Truffle::Primitive.assert_constant can only be called lexically", this));
        }

    }

    @CoreMethod(names = "assert_not_compiled", onSingleton = true)
    public abstract static class AssertNotCompiledNode extends CoreMethodArrayArgumentsNode {

        public AssertNotCompiledNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject assertNotCompiled() {
            throw new RaiseException(getContext().getCoreLibrary().runtimeError("Truffle::Primitive.assert_not_compiled can only be called lexically", this));
        }

    }

    @CoreMethod(names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodArrayArgumentsNode {

        public JavaClassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject javaClassOf(Object value) {
            return createString(StringOperations.encodeRope(value.getClass().getSimpleName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "dump_string", onSingleton = true, required = 1)
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

    @CoreMethod(names = "graal?", onSingleton = true)
    public abstract static class GraalNode extends CoreMethodArrayArgumentsNode {

        public GraalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean graal() {
            return getContext().onGraal();
        }

    }

    @CoreMethod(names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodArrayArgumentsNode {

        public SubstrateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean substrate() {
            return TruffleOptions.AOT;
        }

    }

    @CoreMethod(names = "graal_version", onSingleton = true)
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

    @CoreMethod(names = "simple_shell", onSingleton = true)
    public abstract static class SimpleShellNode extends CoreMethodArrayArgumentsNode {

        public SimpleShellNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject simpleShell() {
            new SimpleShell(getContext()).run(RubyCallStack.getCallerFrame(getContext()).getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize(), this);
            return nil();
        }

    }

    @CoreMethod(names = "coverage_result", onSingleton = true)
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
                final DynamicObject array = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), store, store.length);

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
                    store[n] = getContext().getCoreLibrary().getNilObject();
                } else {
                    store[n] = array[n];
                }
            }

            return store;
        }

    }

    @CoreMethod(names = "coverage_start", onSingleton = true)
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
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "attach", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodArrayArgumentsNode {

        public AttachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public DynamicObject attach(DynamicObject file, int line, DynamicObject block) {
            return getContext().createHandle(getContext().getAttachmentsManager().attach(file.toString(), line, block));
        }

    }

    @CoreMethod(names = "detach", onSingleton = true, required = 1)
    public abstract static class DetachNode extends CoreMethodArrayArgumentsNode {

        public DetachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isHandle(handle)")
        public DynamicObject detach(DynamicObject handle) {
            final Instrument instrument = (Instrument) Layouts.HANDLE.getObject(handle);
            instrument.dispose();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "cext_load", onSingleton = true, needsSelf = false, required = 3)
    public abstract static class CExtLoadNode extends CoreMethodArrayArgumentsNode {

        public CExtLoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = { "isRubyArray(initFunctions)", "isRubyArray(cFlags)", "isRubyArray(files)" })
        public boolean cExtLoad(DynamicObject initFunctions, DynamicObject cFlags, DynamicObject files) {
            final CExtSubsystem subsystem = CExtManager.getSubsystem();

            if (subsystem == null) {
                throw new UnsupportedOperationException();
            }

            subsystem.load(toStrings(initFunctions), toStrings(cFlags), toStrings(files));

            return true;
        }

        private String[] toStrings(DynamicObject array) {
            assert RubyGuards.isRubyArray(array);

            final String[] strings = new String[Layouts.ARRAY.getSize(array)];

            int n = 0;

            for (Object object : ArrayOperations.toIterable(array)) {
                if (RubyGuards.isRubyString(object) || RubyGuards.isRubySymbol(object)) {
                    strings[n] = object.toString();
                    n++;
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(object, "String", this));
                }
            }

            return strings;
        }

    }

    @CoreMethod(names = "cext_supported?", needsSelf = false, onSingleton = true)
    public abstract static class CExtSupportedNode extends CoreMethodArrayArgumentsNode {

        public CExtSupportedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean cExtSupported() {
            return CExtManager.getSubsystem() != null;
        }

    }

    @CoreMethod(names = "debug_print", onSingleton = true, required = 1)
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

    @CoreMethod(names = "debug_print_rope", onSingleton = true, required = 1, optional = 1)
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

    @CoreMethod(names = "flatten_rope", onSingleton = true, required = 1)
    public abstract static class FlattenRopeNode extends CoreMethodArrayArgumentsNode {

        public FlattenRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject flattenRope(DynamicObject string) {
            final Rope flattened = RopeOperations.flatten(StringOperations.rope(string));

            return createString(flattened);
        }

    }

    @CoreMethod(names = "jruby_home_directory", onSingleton = true)
    public abstract static class JRubyHomeDirectoryNode extends CoreMethodNode {

        public JRubyHomeDirectoryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject jrubyHomeDirectory() {
            return createString(StringOperations.encodeRope(getContext().getRuntime().getJRubyHome(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "host_os", onSingleton = true)
    public abstract static class HostOSNode extends CoreMethodNode {

        public HostOSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject hostOS() {
            return createString(StringOperations.encodeRope(RbConfigLibrary.getOSName(), UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true, required = 1)
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

    @CoreMethod(names = "install_rubinius_primitive", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = "fixnum_lower", isModuleFunction = true, required = 1)
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

    @CoreMethod(names = "synchronized", isModuleFunction = true, required = 1, needsBlock = true)
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

    @CoreMethod(names = "full_memory_barrier", isModuleFunction = true)
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

    @CoreMethod(names = "print_backtrace", onSingleton = true)
    public abstract static class PrintBacktraceNode extends CoreMethodNode {

        public PrintBacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject printBacktrace() {
            final List<String> rubyBacktrace = BacktraceFormatter.createDefaultFormatter(getContext())
                    .formatBacktrace(getContext(), null, RubyCallStack.getBacktrace(getContext(), this));

            for (String line : rubyBacktrace) {
                System.err.println(line);
            }

            return nil();
        }

    }

    @CoreMethod(names = "print_interleaved_backtrace", onSingleton = true)
    public abstract static class PrintInterleavedBacktraceNode extends CoreMethodNode {

        public PrintInterleavedBacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject printInterleavedBacktrace() {
            final List<String> rubyBacktrace = BacktraceFormatter.createDefaultFormatter(getContext())
                    .formatBacktrace(getContext(), null, RubyCallStack.getBacktrace(getContext(), this));

            final StackTraceElement[] javaStacktrace = new Exception().getStackTrace();

            for (String line : BacktraceInterleaver.interleave(rubyBacktrace, javaStacktrace)) {
                System.err.println(line);
            }

            return nil();
        }

    }

    @CoreMethod(names = "ast", onSingleton = true, required = 1)
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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), array.toArray(), array.size());
        }

    }

    @CoreMethod(names = "object_type_of", onSingleton = true, required = 1)
    public abstract static class ObjectTypeOfNode extends CoreMethodArrayArgumentsNode {

        public ObjectTypeOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject objectTypeOf(DynamicObject value) {
            return getSymbol(value.getShape().getObjectType().getClass().getSimpleName());
        }
    }

    @CoreMethod(names = "spawn_process", onSingleton = true, required = 3)
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
                throw new RaiseException(getContext().getCoreLibrary().errnoError(getContext().getPosix().errno(), this));
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
            return getContext().getPosix().posix_spawnp(
                    command,
                    Collections.<SpawnFileAction>emptyList(),
                    Arrays.asList(arguments),
                    Arrays.asList(environmentVariables));

        }
    }

    @CoreMethod(names = "context", onSingleton = true)
    public abstract static class ContextNode extends CoreMethodArrayArgumentsNode {

        public ContextNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyContext context() {
            return getContext();
        }
    }

    @CoreMethod(names = "load", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LoadNode extends CoreMethodArrayArgumentsNode {

        public LoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public boolean load(DynamicObject file, boolean wrap) {
            if (wrap) {
                throw new UnsupportedOperationException();
            }

            try {
                getContext().loadFile(StringOperations.getString(getContext(), file), this);
            } catch (IOException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().loadErrorCannotLoad(file.toString(), this));
            }

            return true;
        }

        @Specialization(guards = "isRubyString(file)")
        public boolean load(DynamicObject file, NotProvided wrap) {
            return load(file, false);
        }
    }

    @CoreMethod(names = "run_jruby_root", onSingleton = true)
    public abstract static class RunJRubyRootNode extends CoreMethodArrayArgumentsNode {

        public RunJRubyRootNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object runJRubyRootNode() {
            return getContext().execute(getContext().getInitialJRubyRootNode());
        }
    }

    /*
     * Truffle::Primitive.create_simple_string creates a string 'test' without any part of the string escaping. Useful
     * for testing compilation of String becuase most other ways to construct a string can currently escape.
     */

    @CoreMethod(names = "create_simple_string", onSingleton = true)
    public abstract static class CreateSimpleStringNode extends CoreMethodArrayArgumentsNode {

        public CreateSimpleStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject createSimpleString() {
            return createString(RopeOperations.create(new byte[]{'t', 'e', 's', 't'}, UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
        }
    }

    @CoreMethod(names = "logical_processors", onSingleton = true)
    public abstract static class LogicalProcessorsNode extends CoreMethodNode {

        public LogicalProcessorsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int logicalProcessors() {
            return Runtime.getRuntime().availableProcessors();
        }

    }

}
