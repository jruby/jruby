/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.Collections;
import java.util.List;

public class TruffleFragmentParseNode extends ParseNode {

    private final RubyNode fragment;

    public TruffleFragmentParseNode(SourceIndexLength position, boolean containsAssignment, RubyNode fragment) {
        super(position, containsAssignment);
        this.fragment = fragment;
    }

    public RubyNode getFragment() {
        return fragment;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTruffleFragmentNode(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return Collections.emptyList();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.JAVASCRIPT;
    }
}
