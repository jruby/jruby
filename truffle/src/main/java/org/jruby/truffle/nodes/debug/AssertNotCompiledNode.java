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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyNilClass;

public abstract class AssertNotCompiledNode extends RubyNode {

    public AssertNotCompiledNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    private static volatile boolean[] sideEffect;

    @Specialization
    public RubyNilClass assertNotCompiled() {
        final boolean[] compiled = new boolean[]{CompilerDirectives.inCompiledCode()};

        sideEffect = compiled;

        if (compiled[0]) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new RaiseException(getContext().getCoreLibrary().internalError("Call to Truffle::Primitive.assert_not_compiled was compiled", this));
        }

        return nil();
    }

}
