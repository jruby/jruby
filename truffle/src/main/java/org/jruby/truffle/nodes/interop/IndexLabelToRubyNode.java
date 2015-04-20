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

public abstract class IndexLabelToRubyNode extends TargetableRubyNode {

    public IndexLabelToRubyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public IndexLabelToRubyNode(IndexLabelToRubyNode prev) {
        super(prev.getContext(), prev.getSourceSection());
    }

    @Specialization
    public Object doString(VirtualFrame frame, String index) {
        return toString(index);
    }

    @Specialization
    public Object doObject(VirtualFrame frame, Object index) {
        return index;
    }

    @CompilerDirectives.TruffleBoundary
    private RubyString toString(String index) {
        return getContext().makeString(index);
    }
}
