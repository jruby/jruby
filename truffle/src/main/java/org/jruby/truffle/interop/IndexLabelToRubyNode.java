/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "value", type = RubyNode.class)
public abstract class IndexLabelToRubyNode extends RubyNode {

    public IndexLabelToRubyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeWithTarget(VirtualFrame frame, Object obj);

    @Specialization
    public Object doString(VirtualFrame frame, String index) {
        return toString(index);
    }

    @Specialization
    public Object doObject(VirtualFrame frame, Object index) {
        return index;
    }

    @TruffleBoundary
    private DynamicObject toString(String index) {
        return createString(StringOperations.encodeRope(index, UTF8Encoding.INSTANCE));
    }
}
