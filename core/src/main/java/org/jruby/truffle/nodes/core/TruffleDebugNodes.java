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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.debug.*;
import org.jruby.truffle.runtime.methods.*;

@CoreClass(name = "Debug")
public abstract class TruffleDebugNodes {

    @CoreMethod(names = "tree", isModuleMethod = true, needsSelf = false, appendCallNode = true, minArgs = 1, maxArgs = 1)
    public abstract static class TreeNode extends CoreMethodNode {

        public TreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TreeNode(TreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString tree(Node callNode) {
            return getContext().makeString(NodeUtil.printCompactTreeToString(callNode.getRootNode()));
        }

    }

    @CoreMethod(names = "full_tree", isModuleMethod = true, needsSelf = false, appendCallNode = true, minArgs = 1, maxArgs = 1)
    public abstract static class FullTreeNode extends CoreMethodNode {

        public FullTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FullTreeNode(FullTreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString fullTree(Node callNode) {
            return getContext().makeString(NodeUtil.printTreeToString(callNode.getRootNode()));
        }

    }

    @CoreMethod(names = "parse_tree", isModuleMethod = true, needsSelf = false, appendCallNode = true, minArgs = 1, maxArgs = 1)
    public abstract static class ParseTreeNode extends CoreMethodNode {

        public ParseTreeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ParseTreeNode(ParseTreeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString parseTree(Node callNode) {
            return getContext().makeString(((RubyRootNode) callNode.getRootNode()).getParseTree().toString(true, 0));
        }

    }

}
