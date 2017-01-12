/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.parser.ast.NodeType;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;

import java.util.Collections;
import java.util.List;

public class ReadLocalDummyParseNode extends ParseNode {

    final SourceSection sourceSection;
    final FrameSlot frameSlot;

    public ReadLocalDummyParseNode(SourceIndexLength sourcePosition, SourceSection sourceSection, FrameSlot frameSlot) {
        super(sourcePosition, false);
        this.sourceSection = sourceSection;
        this.frameSlot = frameSlot;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitOther(this);
    }

    @Override
    public List<ParseNode> childNodes() {
        return Collections.emptyList();
    }

    @Override
    public NodeType getNodeType() {
        throw new UnsupportedOperationException();
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public FrameSlot getFrameSlot() {
        return frameSlot;
    }
}
