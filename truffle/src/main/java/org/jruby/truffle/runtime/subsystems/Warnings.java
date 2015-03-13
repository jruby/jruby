/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;

public class Warnings {

    private final RubyContext context;

    public Warnings(RubyContext context) {
        this.context = context;
    }

    public void warn(String format, Object... args) {
        CompilerDirectives.transferToInterpreter();

        warn(String.format(format, args));
    }

    public void warn(String message) {
        CompilerDirectives.transferToInterpreter();

        final SourceSection sourceSection = RubyCallStack.getTopMostUserCallNode().getEncapsulatingSourceSection();
        context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, sourceSection.getSource().getName(), sourceSection.getStartLine(), message);
    }

}
