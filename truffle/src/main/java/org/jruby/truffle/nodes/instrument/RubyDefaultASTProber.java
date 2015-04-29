/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.instrument;

import com.oracle.truffle.api.instrument.ASTProber;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import org.jruby.truffle.nodes.RubyNode;

public class RubyDefaultASTProber implements NodeVisitor, ASTProber {

    @Override
    public boolean visit(Node node) {
        if (node.isInstrumentable() && node instanceof RubyNode) {

            final RubyNode rubyNode = (RubyNode) node;

            if (rubyNode.isAtNewline()) {
                // Identify statements using "newline" nodes created by the JRuby parser.
                rubyNode.probe().tagAs(StandardSyntaxTag.STATEMENT, null);
            }
        }
        return true;
    }

    @Override
    public void probeAST(Node node) {
        node.accept(this);
    }
}
