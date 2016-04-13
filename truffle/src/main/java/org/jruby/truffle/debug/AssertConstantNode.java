/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChild("value")
public abstract class AssertConstantNode extends RubyNode {

    public AssertConstantNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @SuppressWarnings("unused")
    private static volatile boolean[] sideEffect;

    @Specialization
    public Object assertCompilationConstant(Object value) {
        final boolean[] compilationConstant = new boolean[]{ CompilerDirectives.isCompilationConstant(value) };

        // If we didn't cause the value to escape, the transfer would float above the isCompilationConstant

        sideEffect = compilationConstant;

        if (!compilationConstant[0]) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new RaiseException(coreLibrary().internalErrorAssertConstantNotConstant(this));
        }

        return value;
    }

}
