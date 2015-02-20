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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Truffle::Debug")
public abstract class TruffleDebugNodes {

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

}
