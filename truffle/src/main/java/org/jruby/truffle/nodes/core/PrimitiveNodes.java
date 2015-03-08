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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyGC;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.truffle.runtime.subsystems.SimpleShell;
import org.jruby.util.Memo;

import java.util.*;

@CoreClass(name = "Truffle::Primitive")
public abstract class PrimitiveNodes {

    @CoreMethod(names = "binding_of_caller", onSingleton = true)
    public abstract static class BindingOfCallerNode extends CoreMethodNode {

        public BindingOfCallerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindingOfCallerNode(BindingOfCallerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBinding bindingOfCaller() {
            /*
             * When you use this method you're asking for the binding of the caller at the call site. When we get into
             * this method, that is then the binding of the caller of the caller.
             */

            notDesignedForCompilation();

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

    @CoreMethod(names = "gc_count", onSingleton = true)
    public abstract static class GCCountNode extends CoreMethodNode {

        public GCCountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GCCountNode(GCCountNode prev) {
            super(prev);
        }

        @Specialization
        public int gcCount() {
            return RubyGC.getCollectionCount();
        }

    }

    @CoreMethod(names = "gc_time", onSingleton = true)
    public abstract static class GCTimeNode extends CoreMethodNode {

        public GCTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GCTimeNode(GCTimeNode prev) {
            super(prev);
        }

        @Specialization
        public long gcTime() {
            return RubyGC.getCollectionTime();
        }

    }

    @CoreMethod(names = "assert_constant", onSingleton = true, required = 1)
    public abstract static class AssertConstantNode extends CoreMethodNode {

        public AssertConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AssertConstantNode(AssertConstantNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass assertConstant(Object value) {
            throw new RaiseException(getContext().getCoreLibrary().runtimeError("Truffle::Debug.assert_constant can only be called lexically", this));
        }

    }

    @CoreMethod(names = "dump_call_stack", onSingleton = true)
    public abstract static class DumpCallStackNode extends CoreMethodNode {

        public DumpCallStackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpCallStackNode(DumpCallStackNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass dumpCallStack() {
            notDesignedForCompilation();

            for (String line : Backtrace.DEBUG_FORMATTER.format(getContext(), null, RubyCallStack.getBacktrace(this))) {
                System.err.println(line);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "flush_stdout", onSingleton = true)
    public abstract static class FlushStdoutNode extends CoreMethodNode {

        public FlushStdoutNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FlushStdoutNode(FlushStdoutNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass flush() {
            getContext().getRuntime().getOut().flush();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "full_tree", onSingleton = true)
    public abstract static class FullTreeNode extends CoreMethodNode {

        public FullTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FullTreeNode(FullTreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString fullTree() {
            notDesignedForCompilation();

            return getContext().makeString(NodeUtil.printTreeToString(Truffle.getRuntime().getCallerFrame().getCallNode().getRootNode()));
        }

    }

    @CoreMethod(names = "java_class_of", onSingleton = true, required = 1)
    public abstract static class JavaClassOfNode extends CoreMethodNode {

        public JavaClassOfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JavaClassOfNode(JavaClassOfNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString javaClassOf(Object value) {
            notDesignedForCompilation();

            return getContext().makeString(value.getClass().getName());
        }

    }

    @CoreMethod(names = "dump_string", onSingleton = true, required = 1)
    public abstract static class DumpStringNode extends CoreMethodNode {

        public DumpStringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpStringNode(DumpStringNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString dumpString(RubyString string) {
            notDesignedForCompilation();

            final StringBuilder builder = new StringBuilder();
            builder.append("\"");

            for (byte b : string.getBytes().unsafeBytes()) {
                builder.append(String.format("\\x%02x", b));
            }

            builder.append("\"");

            return getContext().makeString(builder.toString());
        }

    }

    @CoreMethod(names = "source_attribution_tree", onSingleton = true)
    public abstract static class SourceAttributionTreeNode extends CoreMethodNode {

        public SourceAttributionTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SourceAttributionTreeNode(SourceAttributionTreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString sourceAttributionTree() {
            notDesignedForCompilation();

            return getContext().makeString(NodeUtil.printSourceAttributionTree(Truffle.getRuntime().getCallerFrame().getCallNode().getRootNode()));
        }

    }

    @CoreMethod(names = "storage_class", onSingleton = true, required = 1)
    public abstract static class StorageClassNode extends CoreMethodNode {

        public StorageClassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StorageClassNode(StorageClassNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString storageClass(RubyArray array) {
            notDesignedForCompilation();

            if (array.getStore() == null) {
                return getContext().makeString("null");
            } else {
                return getContext().makeString(array.getStore().getClass().getName());
            }
        }

        @Specialization
        public RubyString storageClass(RubyHash hash) {
            notDesignedForCompilation();

            if (hash.getStore() == null) {
                return getContext().makeString("null");
            } else {
                return getContext().makeString(hash.getStore().getClass().getName());
            }
        }

    }

    @CoreMethod(names = "panic", onSingleton = true)
    public abstract static class PanicNode extends CoreMethodNode {

        public PanicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PanicNode(PanicNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass doPanic() {
            DebugOperations.panic(getContext(), this, null);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "parse_tree", onSingleton = true)
    public abstract static class ParseTreeNode extends CoreMethodNode {

        public ParseTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ParseTreeNode(ParseTreeNode prev) {
            super(prev);
        }

        @Specialization
        public Object parseTree(VirtualFrame frame) {
            notDesignedForCompilation();

            final org.jruby.ast.Node parseTree = RubyCallStack.getCallingMethod(frame).getSharedMethodInfo().getParseTree();

            if (parseTree == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return getContext().makeString(parseTree.toString(true, 0));
            }
        }

    }

    @CoreMethod(names = "tree", onSingleton = true)
    public abstract static class TreeNode extends CoreMethodNode {

        public TreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TreeNode(TreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString tree() {
            notDesignedForCompilation();

            return getContext().makeString(NodeUtil.printCompactTreeToString(Truffle.getRuntime().getCallerFrame().getCallNode().getRootNode()));
        }

    }

    @CoreMethod(names = "graal?", onSingleton = true)
    public abstract static class GraalNode extends CoreMethodNode {

        public GraalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GraalNode(GraalNode prev) {
            super(prev);
        }

        @Specialization
        public boolean graal() {
            return Truffle.getRuntime().getName().toLowerCase(Locale.ENGLISH).contains("graal");
        }

    }

    @CoreMethod(names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodNode {

        public SubstrateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubstrateNode(SubstrateNode prev) {
            super(prev);
        }

        @Specialization
        public boolean substrate() {
            return getContext().getRuntime().isSubstrateVM();
        }

    }

    @CoreMethod(names = "graal_version", onSingleton = true)
    public abstract static class GraalVersionNode extends CoreMethodNode {

        public GraalVersionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GraalVersionNode(GraalVersionNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString graalVersion() {
            return getContext().makeString(System.getProperty("graal.version", "unknown"));
        }

    }

    @CoreMethod(names = "simple_shell", onSingleton = true)
    public abstract static class SimpleShellNode extends CoreMethodNode {

        public SimpleShellNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SimpleShellNode(SimpleShellNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass simpleShell() {
            new SimpleShell(getContext()).run(Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize(), this);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "coverage_result", onSingleton = true)
    public abstract static class CoverageResultNode extends CoreMethodNode {

        public CoverageResultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CoverageResultNode(CoverageResultNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash coverageResult() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            final List<KeyValue> keyValues = new ArrayList<>();

            for (Map.Entry<Source, Long[]> source : getContext().getCoverageTracker().getCounts().entrySet()) {
                final Object[] store = lineCountsStore(source.getValue());
                final RubyArray array = new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, store.length);
                keyValues.add(new KeyValue(getContext().makeString(source.getKey().getPath()), array));
            }

            return HashOperations.verySlowFromEntries(getContext(), keyValues, false);
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
    public abstract static class CoverageStartNode extends CoreMethodNode {

        public CoverageStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CoverageStartNode(CoverageStartNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass coverageStart() {
            if (getContext().getCoverageTracker() == null) {
                throw new UnsupportedOperationException("coverage is disabled");
            }

            getContext().getCoverageTracker().install();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "attach", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodNode {

        public AttachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AttachNode(AttachNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass attach(RubyString file, int line, RubyProc block) {
            getContext().getAttachmentsManager().attach(file.toString(), line, block);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "detach", onSingleton = true, required = 2)
    public abstract static class DetachNode extends CoreMethodNode {

        public DetachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DetachNode(DetachNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass detach(RubyString file, int line) {
            getContext().getAttachmentsManager().detach(file.toString(), line);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
