/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.LexicalScope;

/**
 * {@link InternalMethod} objects are copied as properties such as visibility are changed. {@link SharedMethodInfo} stores
 * the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final LexicalScope lexicalScope;
    private final String name;
    private final boolean isBlock;
    private final org.jruby.ast.Node parseTree;
    private final boolean alwaysSplit;

    public SharedMethodInfo(SourceSection sourceSection, LexicalScope lexicalScope, String name, boolean isBlock, org.jruby.ast.Node parseTree, boolean alwaysSplit) {
        assert sourceSection != null;
        assert name != null;

        this.sourceSection = sourceSection;
        this.lexicalScope = lexicalScope;
        this.name = name;
        this.isBlock = isBlock;
        this.parseTree = parseTree;
        this.alwaysSplit = alwaysSplit;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return isBlock;
    }

    public org.jruby.ast.Node getParseTree() {
        return parseTree;
    }

    public boolean shouldAlwaysSplit() {
        return alwaysSplit;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        if (isBlock) {
            builder.append("block in ");
        }

        builder.append(name);
        builder.append(":");
        builder.append(sourceSection.getShortDescription());

        return builder.toString();
    }

}
