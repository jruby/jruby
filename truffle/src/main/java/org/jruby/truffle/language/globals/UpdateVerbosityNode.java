/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class UpdateVerbosityNode extends RubyNode {

    @Child private RubyNode child;

    public UpdateVerbosityNode(RubyContext context, SourceSection sourceSection, RubyNode child) {
        super(context, sourceSection);
        this.child = child;
    }

    public Object execute(VirtualFrame frame) {
        final Object childValue = child.execute(frame);
        setVerbose(childValue);
        return childValue;
    }

    @TruffleBoundary
    private void setVerbose(Object childValue) {
        if (childValue instanceof Boolean) {
            getContext().getJRubyInterop().setVerbose((boolean) childValue);
        } else if (childValue == nil()) {
            getContext().getJRubyInterop().setVerboseNil();
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
