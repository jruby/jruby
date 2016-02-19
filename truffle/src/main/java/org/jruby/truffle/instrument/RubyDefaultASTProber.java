/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.instrument;

import com.oracle.truffle.api.instrument.ASTProber;
import com.oracle.truffle.api.instrument.Instrumenter;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.objects.OpenModuleNode;

public class RubyDefaultASTProber implements NodeVisitor, ASTProber {

    private final Instrumenter instrumenter;

    public RubyDefaultASTProber(Instrumenter instrumenter) {
        this.instrumenter = instrumenter;
    }

    @Override
    public boolean visit(Node node) {
        if (RubyLanguage.INSTANCE.isInstrumentable(node)) {
            if (node instanceof RubyNode) {
                final RubyNode rubyNode = (RubyNode) node;

                if (rubyNode.isAtNewline()) {
                    final Probe probe = instrumenter.probe(rubyNode);
                    // Identify statements using "newline" nodes created by the JRuby parser.
                    probe.tagAs(StandardSyntaxTag.STATEMENT, null);
                    probe.tagAs(RubySyntaxTag.LINE, null);
                }

            }
        }

        return true;
    }

    @Override
    public void probeAST(Instrumenter instrumenter, RootNode rootNode) {
        rootNode.accept(this);
    }

}
