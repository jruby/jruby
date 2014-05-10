/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.SourceSection;

/**
 * {@link RubyMethod} objects are copied as properties such as visibility are changed. {@link SharedMethodInfo} stores
 * the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final String name;
    private final boolean isBlock;
    private final org.jruby.ast.Node parseTree;

    public SharedMethodInfo(SourceSection sourceSection, String name, boolean isBlock, org.jruby.ast.Node parseTree) {
        this.sourceSection = sourceSection;
        this.name = name;
        this.isBlock = isBlock;
        this.parseTree = parseTree;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
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

}
