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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "TruffleDebug")
public abstract class TruffleDebugNodes {

    @CoreMethod(names = "java_class_of", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class JavaClassOf extends CoreMethodNode {

        public JavaClassOf(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JavaClassOf(JavaClassOf prev) {
            super(prev);
        }

        @Specialization
        public RubyString javaClassOf(Object value) {
            notDesignedForCompilation();

            return getContext().makeString(value.getClass().getName());
        }

    }

    @CoreMethod(names = "array_storage_info", isModuleMethod = true, needsSelf = false, minArgs = 1, maxArgs = 1)
    public abstract static class ArrayStorageInfo extends CoreMethodNode {

        public ArrayStorageInfo(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ArrayStorageInfo(ArrayStorageInfo prev) {
            super(prev);
        }

        @Specialization
        public RubyString javaClassOf(RubyArray array) {
            notDesignedForCompilation();
            return getContext().makeString("RubyArray(" + (array.getStore() == null ? "null" : array.getStore().getClass()) + "*" + array.getSize() + ")");
        }

    }

    @CoreMethod(names = "parse_tree", isModuleMethod = true, needsSelf = false, maxArgs = 0)
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

    @CoreMethod(names = "tree", isModuleMethod = true, needsSelf = false, maxArgs = 0)
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

            return getContext().makeString(NodeUtil.printCompactTreeToString(RubyCallStack.getCallerFrame().getCallNode().getRootNode()));
        }

    }

    @CoreMethod(names = "full_tree", isModuleMethod = true, needsSelf = false, maxArgs = 0)
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

            return getContext().makeString(NodeUtil.printTreeToString(RubyCallStack.getCallerFrame().getCallNode().getRootNode()));
        }

    }

    @CoreMethod(names = "dump_call_stack", isModuleMethod = true, needsSelf = false, maxArgs = 0)
    public abstract static class DumpCallStack extends CoreMethodNode {

        public DumpCallStack(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpCallStack(FullTreeNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder dumpCallStack() {
            notDesignedForCompilation();
            RubyCallStack.dump(getContext(), this);
            return NilPlaceholder.INSTANCE;
        }

    }

}
