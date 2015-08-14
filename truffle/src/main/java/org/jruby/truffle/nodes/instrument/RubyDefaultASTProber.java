/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.instrument;

import com.oracle.truffle.api.instrument.ASTProber;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.RubySyntaxTag;

public class RubyDefaultASTProber implements NodeVisitor, ASTProber {

    @Override
    public boolean visit(Node node) {
        if (node.isInstrumentable()) {
            if (node instanceof RubyNode) {
                final RubyNode rubyNode = (RubyNode) node;

                if (rubyNode.isAtNewline()) {
                    // Identify statements using "newline" nodes created by the JRuby parser.
                    rubyNode.probe().tagAs(StandardSyntaxTag.STATEMENT, null);
                    rubyNode.probe().tagAs(RubySyntaxTag.LINE, null);
                }

                if (rubyNode.getParent() instanceof RubyRootNode) {
                    rubyNode.probe().tagAs(RubySyntaxTag.CALL, null);
                }

            // A RubyRootNode can't have a probe because it doesn't have a parent.  So, we do the next best thing and
            // tag its immediate child.  The trace instrument will know to look at the parent (RubyRootNode) based upon
            // the context implied by the tag.  We need to tag at the RubyRootNode because the semantics of set_trace_func
            // are such that the receiver must be resolved, so we have to push as far into the callee as we can to have
            // a properly constructed frame.
            } else if (node.getParent() instanceof RubyRootNode) {
                node.probe().tagAs(RubySyntaxTag.CALL, null);
            }
        }

        return true;
    }

    @Override
    public void probeAST(Node node) {
        node.accept(this);
    }
}
