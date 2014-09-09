/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild("body")
public abstract class AssertCompilationConstantNode extends RubyNode {

    public AssertCompilationConstantNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public AssertCompilationConstantNode(AssertCompilationConstantNode prev) {
        super(prev);
    }

    @Specialization
    public boolean assertCompilationConstant(boolean value) {
        return CompilerAsserts.compilationConstant(value);
    }

    @Specialization
    public int assertCompilationConstant(int value) {
        return CompilerAsserts.compilationConstant(value);
    }

    @Specialization
    public long assertCompilationConstant(long value) {
        return CompilerAsserts.compilationConstant(value);
    }

    @Specialization
    public double assertCompilationConstant(double value) {
        return CompilerAsserts.compilationConstant(value);
    }

    @Specialization
    public Object assertCompilationConstant(Object value) {
        return CompilerAsserts.compilationConstant(value);
    }

}
