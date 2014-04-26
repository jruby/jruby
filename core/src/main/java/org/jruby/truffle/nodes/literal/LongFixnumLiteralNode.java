/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.literal;

import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeInfo(shortName = "fixnum(long)")
public class LongFixnumLiteralNode extends RubyNode {

    private final long value;

    public LongFixnumLiteralNode(RubyContext context, SourceSection sourceSection, long value) {
        super(context, sourceSection);
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeLongFixnum(frame);
    }

    @Override
    public long executeLongFixnum(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("expression");
    }

    public long getValue() {
        return value;
    }

}
