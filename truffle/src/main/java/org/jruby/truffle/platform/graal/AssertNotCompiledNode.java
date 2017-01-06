/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.graal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public abstract class AssertNotCompiledNode extends RubyNode {

    @SuppressWarnings("unused")
    private static volatile boolean[] sideEffect;

    @Specialization
    public DynamicObject assertNotCompiled() {
        final boolean[] compiled = new boolean[]{ CompilerDirectives.inCompiledCode() };

        // If we didn't cause the value to escape, the transfer would float above the isCompilationConstant

        sideEffect = compiled;

        if (compiled[0]) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new RaiseException(coreExceptions().internalErrorAssertNotCompiledCompiled(this));
        }

        return nil();
    }

}
