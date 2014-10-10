/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "TruffleDebug")
public abstract class TruffleDebugNodes {

    @CoreMethod(names = "array_storage_info", isModuleFunction = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ArrayStorageInfoNode extends CoreMethodNode {

        public ArrayStorageInfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ArrayStorageInfoNode(ArrayStorageInfoNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString javaClassOf(RubyArray array) {
            notDesignedForCompilation();
            return getContext().makeString("RubyArray(" + (array.getStore() == null ? "null" : array.getStore().getClass()) + "*" + array.getSize() + ")");
        }

    }

    @CoreMethod(names = "dump_call_stack", isModuleFunction = true, needsSelf = false, maxArgs = 0)
    public abstract static class DumpCallStackNode extends CoreMethodNode {

        public DumpCallStackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpCallStackNode(FullTreeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder dumpCallStack() {
            notDesignedForCompilation();

            for (String line : Backtrace.DEBUG_FORMATTER.format(getContext(), null, RubyCallStack.getBacktrace(this))) {
                System.err.println(line);
            }

            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "flush_stdout", isModuleFunction = true, needsSelf = false, maxArgs = 0)
    public abstract static class FlushStdoutNode extends CoreMethodNode {

        public FlushStdoutNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FlushStdoutNode(PanicNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder flush() {
            getContext().getRuntime().getOut().flush();
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "full_tree", isModuleFunction = true, needsSelf = false, maxArgs = 0)
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

    @CoreMethod(names = "java_class_of", isModuleFunction = true, needsSelf = false, minArgs = 1, maxArgs = 1)
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

    @CoreMethod(names = "panic", isModuleFunction = true, needsSelf = false, maxArgs = 0)
    public abstract static class PanicNode extends CoreMethodNode {

        public PanicNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PanicNode(PanicNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder doPanic() {
            panic();
            return NilPlaceholder.INSTANCE;
        }

    }

    @CoreMethod(names = "parse_tree", isModuleFunction = true, needsSelf = false, maxArgs = 0)
    public abstract static class ParseTreeNode extends CoreMethodNode {

        public ParseTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ParseTreeNode(ParseTreeNode prev) {
            super(prev);
        }

        @Specialization
        public Object parseTree() {
            notDesignedForCompilation();

            final org.jruby.ast.Node parseTree = RubyCallStack.getCurrentMethod().getSharedMethodInfo().getParseTree();

            if (parseTree == null) {
                return NilPlaceholder.INSTANCE;
            } else {
                return getContext().makeString(parseTree.toString(true, 0));
            }
        }

    }

    @CoreMethod(names = "slow_path", isModuleFunction = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class SlowPathNode extends CoreMethodNode {

        public SlowPathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SlowPathNode(SlowPathNode prev) {
            super(prev);
        }

        @CompilerDirectives.SlowPath
        @Specialization
        public Object slowPath(Object value) {
            return value;
        }

    }

    @CoreMethod(names = "tree", isModuleFunction = true, needsSelf = false, maxArgs = 0)
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
