/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

import java.math.BigInteger;

@CoreClass(name = "Numeric")
public abstract class NumericNodes {

    @CoreMethod(names = "+@")
    public abstract static class PosNode extends CoreMethodNode {

        public PosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PosNode(PosNode prev) {
            super(prev);
        }

        @Specialization
        public int pos(int value) {
            return value;
        }

        @Specialization
        public long pos(long value) {
            return value;
        }

        @Specialization
        public RubyBignum pos(RubyBignum value) {
            return value;
        }

        @Specialization
        public double pos(double value) {
            return value;
        }

    }

    @CoreMethod(names = "nonzero?")
    public abstract static class NonZeroNode extends CoreMethodNode {

        public NonZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NonZeroNode(NonZeroNode prev) {
            super(prev);
        }

        @Specialization
        public Object nonZero(int value) {
            if (value == 0) {
                return false;
            } else {
                return value;
            }
        }

        @Specialization
        public Object nonZero(long value) {
            if (value == 0L) {
                return false;
            } else {
                return value;
            }
        }

        @Specialization
        public Object nonZero(RubyBignum value) {
            if (value.bigIntegerValue().equals(BigInteger.ZERO)) {
                return false;
            } else {
                return value;
            }
        }

        @Specialization
        public Object nonZero(double value) {
            if (value == 0.0) {
                return false;
            } else {
                return value;
            }
        }

    }

    @CoreMethod(names = "step", needsBlock = true, required = 2)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StepNode(StepNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass step(VirtualFrame frame, int from, int to, int step, RubyProc block) {
            for (int i = from; i <= to; i += step) {
                yield(frame, block, i);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
