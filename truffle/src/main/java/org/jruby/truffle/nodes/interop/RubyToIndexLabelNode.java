/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public abstract class RubyToIndexLabelNode extends TargetableRubyNode {

    public RubyToIndexLabelNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization
    public Object doRubyString(VirtualFrame frame, RubyString index) {
        return toString(index);
    }

    @Specialization
    public Object doRubySymbol(VirtualFrame frame, RubySymbol index) {
        return toString(index);
    }

    @Specialization
    public Object doObject(VirtualFrame frame, Object index) {
        return index;
    }

    @CompilerDirectives.TruffleBoundary
    private String toString(RubyString index) {
        return index.toString();
    }

    @CompilerDirectives.TruffleBoundary
    private String toString(RubySymbol index) {
        return index.toString();
    }
}
