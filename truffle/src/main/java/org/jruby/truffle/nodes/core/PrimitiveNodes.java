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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyGC;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.subsystems.SimpleShell;
import org.jruby.util.Memo;

import java.util.Locale;

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

            notDesignedForCompilation("65cfab79b2ac40d4bc39edbbc928f6b7");

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
            notDesignedForCompilation("38e9ebc407c14bbe870b8552adcca5f6");

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
            notDesignedForCompilation("ff6a46e3c72f42cb90ec8dad75244975");

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
            notDesignedForCompilation("9607cd475289496bb19402e237b68810");

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
            notDesignedForCompilation("5a401f3f58c047a694ad551172446330");

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
            notDesignedForCompilation("e6b34ce906254c85aa27e481845bad7c");

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
            notDesignedForCompilation("08e2f93a31754638a6d44db2a501d622");

            if (array.getStore() == null) {
                return getContext().makeString("null");
            } else {
                return getContext().makeString(array.getStore().getClass().getName());
            }
        }

        @Specialization
        public RubyString storageClass(RubyHash hash) {
            notDesignedForCompilation("c1bc4fd709394e3684c55a61357a9545");

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
            notDesignedForCompilation("64fed10cf37947ca80e7ce34666b272c");

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
            notDesignedForCompilation("b1f6a1cd91454f5c92e9dd90168864c1");

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

}
