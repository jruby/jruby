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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;

import java.math.BigInteger;

@CoreClass(name = "Integer")
public abstract class IntegerNodes {

    @CoreMethod(names = "downto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class DownToNode extends YieldingCoreMethodNode {

        public DownToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, int to, RubyProc block) {
            int count = 0;

            try {
                for (int i = from; i >= to; i--) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return nil();
        }

        @Specialization
        public Object downto(VirtualFrame frame, long from, long to, RubyProc block) {
            // TODO BJF 22-Apr-2015 how to handle reportLoopCount(long)
            int count = 0;

            try {
                for (long i = from; i >= to; i--) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return nil();
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, double to, RubyProc block) {
            return downto(frame, from, (int) Math.ceil(to), block);
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        // TODO CS 2-May-15 we badly need OSR in this node

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject times(VirtualFrame frame, int n, NotProvided block) {
            // TODO (eregon, 16 June 2015): this should return an enumerator
            final int[] array = new int[n];

            for (int i = 0; i < n; i++) {
                array[i] = i;
            }

            return createArray(array, n);
        }

        @Specialization
        public Object times(VirtualFrame frame, int n, RubyProc block) {
            int count = 0;

            try {
                for (int i = 0; i < n; i++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return n;
        }

        @Specialization
        public Object times(VirtualFrame frame, long n, RubyProc block) {
            int count = 0;

            try {
                for (long i = 0; i < n; i++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return n;
        }

        @Specialization(guards = "isRubyBignum(n)")
        public Object times(VirtualFrame frame, RubyBasicObject n, RubyProc block,
                @Cached("create(getContext(), getSourceSection())") FixnumOrBignumNode fixnumOrBignumNode) {

            for (BigInteger i = BigInteger.ZERO; i.compareTo(BignumNodes.getBigIntegerValue(n)) < 0; i = i.add(BigInteger.ONE)) {
                yield(frame, block, fixnumOrBignumNode.fixnumOrBignum(i));
            }

            return n;
        }

    }

    @CoreMethod(names = {"to_i", "to_int"})
    public abstract static class ToINode extends CoreMethodArrayArgumentsNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int toI(int n) {
            return n;
        }

        @Specialization
        public long toI(long n) {
            return n;
        }

        @Specialization(guards = "isRubyBignum(n)")
        public RubyBasicObject toI(RubyBasicObject n) {
            return n;
        }

    }

    @CoreMethod(names = "upto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        public UpToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, int to, RubyProc block) {
            int count = 0;

            try {
                for (int i = from; i <= to; i++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return nil();
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, double to, RubyProc block) {
            return upto(frame, from, (int) Math.floor(to), block);
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, long to, RubyProc block) {
            int count = 0;

            try {
                for (long i = from; i <= to; i++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return nil();
        }

    }

}
