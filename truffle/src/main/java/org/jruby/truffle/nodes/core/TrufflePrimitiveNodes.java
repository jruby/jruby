/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.Ruby;
import org.jruby.RubyGC;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.cext.CExtManager;
import org.jruby.truffle.runtime.cext.CExtSubsystem;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.BucketsStrategy;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.subsystems.SimpleShell;
import org.jruby.util.Memo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CoreClass(name = "Truffle::Primitive")
public abstract class TrufflePrimitiveNodes {

    @CoreMethod(names = "binding_of_caller", onSingleton = true)
    public abstract static class BindingOfCallerNode extends CoreMethodArrayArgumentsNode {

        public BindingOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBinding bindingOfCaller() {
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

            return new RubyBinding(
                    getContext().getCoreLibrary().getBindingClass(),
                    RubyArguments.getSelf(frame.getArguments()),
                    frame);
        }

    }

    @CoreMethod(names = "source_of_caller", onSingleton = true)
    public abstract static class SourceOfCallerNode extends CoreMethodArrayArgumentsNode {

        public SourceOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject sourceOfCaller() {
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

            return createString(source);
        }

    }

    @CoreMethod(names = "gc_count", onSingleton = true)
    public abstract static class GCCountNode extends CoreMethodArrayArgumentsNode {

        public GCCountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
        public RubyBasicObject assertConstant(Object value) {
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
        public RubyBasicObject assertNotCompiled() {
            throw new RaiseException(getContext().getCoreLibrary().runtimeError("Truffle::Primitive.assert_not_compiled can only be called lexically", this));
        }

    }

    @CoreMethod(names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodArrayArgumentsNode {

        public JavaClassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject javaClassOf(Object value) {
            return createString(value.getClass().getSimpleName());
        }

    }

    @CoreMethod(names = "dump_string", onSingleton = true, required = 1)
    public abstract static class DumpStringNode extends CoreMethodArrayArgumentsNode {

        public DumpStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject dumpString(RubyString string) {
            final StringBuilder builder = new StringBuilder();

            for (byte b : StringNodes.getByteList(string).unsafeBytes()) {
                builder.append(String.format("\\x%02x", b));
            }

            return createString(builder.toString());
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
            return Truffle.getRuntime().getName().toLowerCase(Locale.ENGLISH).contains("graal");
        }

    }

    @CoreMethod(names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodArrayArgumentsNode {

        public SubstrateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean substrate() {
            return Ruby.isSubstrateVM();
        }

    }

    @CoreMethod(names = "graal_version", onSingleton = true)
    public abstract static class GraalVersionNode extends CoreMethodArrayArgumentsNode {

        public GraalVersionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject graalVersion() {
            return createString(System.getProperty("graal.version", "unknown"));
        }

    }

    @CoreMethod(names = "simple_shell", onSingleton = true)
    public abstract static class SimpleShellNode extends CoreMethodArrayArgumentsNode {

        public SimpleShellNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject simpleShell() {
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
        public RubyBasicObject coverageResult() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            final Map<Object, Object> converted = new HashMap<>();

            for (Map.Entry<Source, Long[]> source : getContext().getCoverageTracker().getCounts().entrySet()) {
                final Object[] store = lineCountsStore(source.getValue());
                final RubyBasicObject array = createArray(store, store.length);
                converted.put(createString(source.getKey().getPath()), array);
            }

            return BucketsStrategy.create(getContext().getCoreLibrary().getHashClass(), converted.entrySet(), false);
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
        public RubyBasicObject coverageStart() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            getContext().getCoverageTracker().install();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "attach", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodArrayArgumentsNode {

        public AttachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject attach(RubyString file, int line, RubyProc block) {
            getContext().getAttachmentsManager().attach(file.toString(), line, block);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "detach", onSingleton = true, required = 2)
    public abstract static class DetachNode extends CoreMethodArrayArgumentsNode {

        public DetachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject detach(RubyString file, int line) {
            getContext().getAttachmentsManager().detach(file.toString(), line);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "cext_load", onSingleton = true, needsSelf = false, required = 3)
    public abstract static class CExtLoadNode extends CoreMethodArrayArgumentsNode {

        public CExtLoadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean cExtLoad(RubyArray initFunctions, RubyArray cFlags, RubyArray files) {
            final CExtSubsystem subsystem = CExtManager.getSubsystem();

            if (subsystem == null) {
                throw new UnsupportedOperationException();
            }

            subsystem.load(toStrings(initFunctions), toStrings(cFlags), toStrings(files));

            return true;
        }

        private String[] toStrings(RubyArray array) {
            final String[] strings = new String[ArrayNodes.getSize(array)];

            int n = 0;

            for (Object object : ArrayNodes.slowToArray(array)) {
                if (object instanceof RubyString || RubyGuards.isRubySymbol(object)) {
                    strings[n] = object.toString();
                    n++;
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(object),
                            getContext().getCoreLibrary().getStringClass(),
                            this));
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
        @Specialization
        public RubyBasicObject debugPrint(RubyString string) {
            System.err.println(string.toString());
            return nil();
        }

    }

    @CoreMethod(names = "home_directory", onSingleton = true)
    public abstract static class HomeDirectoryNode extends CoreMethodNode {

        public HomeDirectoryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject homeDirectory() {
            return createString(getContext().getRuntime().getJRubyHome());
        }

    }

    @CoreMethod(names = "host_os", onSingleton = true)
    public abstract static class HostOSNode extends CoreMethodNode {

        public HostOSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject hostOS() {
            return createString(RbConfigLibrary.getOSName());
        }

    }

    @CoreMethod(names = "at_exit", isModuleFunction = true, needsBlock = true, required = 1)
    public abstract static class AtExitSystemNode extends CoreMethodArrayArgumentsNode {

        public AtExitSystemNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object atExit(boolean always, RubyProc block) {
            getContext().getAtExitManager().add(block, always);
            return nil();
        }
    }

    @CoreMethod(names = "install_rubinius_primitive", isModuleFunction = true, required = 2)
    public abstract static class InstallRubiniusPrimitiveNode extends CoreMethodArrayArgumentsNode {

        public InstallRubiniusPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(name)")
        public Object installRubiniusPrimitive(RubyModule module, RubyBasicObject name) {
            getContext().getRubiniusPrimitiveManager().installPrimitive(module, SymbolNodes.getString(name));
            return nil();
        }
    }

}
