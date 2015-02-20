/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.debug;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild("value")
public abstract class AssertConstantNode extends RubyNode {

    public AssertConstantNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public AssertConstantNode(AssertConstantNode prev) {
        super(prev);
    }

    @Specialization
    public Object assertCompilationConstant(Object value) {
        CompilerAsserts.compilationConstant(value);
        return value;
    }

}
