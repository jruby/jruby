/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class DisablingBacktracesNode extends RubyNode {

    @Child private RubyNode child;

    private static final ThreadLocal<Boolean> BACTRACES_DISABLED = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public DisablingBacktracesNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final boolean backtracesPreviouslyDisabled = getBacktracesDisabled();
        try {
            setBacktracesDisabled(true);
            return child.execute(frame);
        } finally {
            setBacktracesDisabled(backtracesPreviouslyDisabled);
        }
    }

    @TruffleBoundary
    private static Boolean getBacktracesDisabled() {
        return BACTRACES_DISABLED.get();
    }

    @TruffleBoundary
    private void setBacktracesDisabled(boolean value) {
        BACTRACES_DISABLED.set(value);
    }

    public static boolean areBacktracesDisabled() {
        return getBacktracesDisabled();
    }

}
