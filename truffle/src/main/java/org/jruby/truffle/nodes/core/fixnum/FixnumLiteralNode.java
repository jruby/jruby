/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.fixnum;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

public abstract class FixnumLiteralNode {

    @NodeInfo(cost = NodeCost.NONE)
    public static class IntegerFixnumLiteralNode extends RubyNode {

        private final int value;

        public IntegerFixnumLiteralNode(RubyContext context, SourceSection sourceSection, int value) {
            super(context, sourceSection);
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return executeInteger(frame);
        }

        @Override
        public int executeInteger(VirtualFrame frame) {
            return value;
        }

        public int getValue() {
            return value;
        }

    }

    @NodeInfo(cost = NodeCost.NONE)
    public static class LongFixnumLiteralNode extends RubyNode {

        private final long value;

        public LongFixnumLiteralNode(RubyContext context, SourceSection sourceSection, long value) {
            super(context, sourceSection);
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return executeLong(frame);
        }

        @Override
        public long executeLong(VirtualFrame frame) {
            return value;
        }

        public long getValue() {
            return value;
        }

    }

}
