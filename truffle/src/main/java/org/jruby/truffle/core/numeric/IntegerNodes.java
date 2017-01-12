/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;

import java.math.BigInteger;

@CoreClass("Integer")
public abstract class IntegerNodes {

    @CoreMethod(names = "downto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class DownToNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode downtoInternalCall;

        @Specialization
        public Object downto(VirtualFrame frame, int from, int to, DynamicObject block) {
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
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return nil();
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, double to, DynamicObject block) {
            return downto(frame, from, (int) Math.ceil(to), block);
        }

        @Specialization
        public Object downto(VirtualFrame frame, long from, long to, DynamicObject block) {
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
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return nil();
        }

        @Specialization
        public Object downto(VirtualFrame frame, long from, double to, DynamicObject block) {
            return downto(frame, from, (long) Math.ceil(to), block);
        }

        @Specialization(guards = "isDynamicObject(from) || isDynamicObject(to)")
        public Object downto(VirtualFrame frame, Object from, Object to, DynamicObject block) {
            if (downtoInternalCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                downtoInternalCall = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            return downtoInternalCall.callWithBlock(frame, from, "downto_internal", block, to);
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        // TODO CS 2-May-15 we badly need OSR in this node

        @Specialization
        public DynamicObject times(VirtualFrame frame, int n, NotProvided block) {
            // TODO (eregon, 16 June 2015): this should return an enumerator
            final int[] array = new int[n];

            for (int i = 0; i < n; i++) {
                array[i] = i;
            }

            return createArray(array, n);
        }

        @Specialization
        public int times(VirtualFrame frame, int n, DynamicObject block,
                @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
            int i = 0;
            loopProfile.profileCounted(n);
            try {
                for (; loopProfile.inject(i < n); i++) {
                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, i);
                }
            }

            return n;
        }

        @Specialization
        public long times(VirtualFrame frame, long n, DynamicObject block,
                @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
            long i = 0;
            loopProfile.profileCounted(n);
            try {
                for (; loopProfile.inject(i < n); i++) {
                    yield(frame, block, i);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, i < Integer.MAX_VALUE ? (int) i : Integer.MAX_VALUE);
                }
            }

            return n;
        }

        @Specialization(guards = "isRubyBignum(n)")
        public Object times(VirtualFrame frame, DynamicObject n, DynamicObject block,
                            @Cached("create(getSourceIndexLength())") FixnumOrBignumNode fixnumOrBignumNode) {

            for (BigInteger i = BigInteger.ZERO; i.compareTo(Layouts.BIGNUM.getValue(n)) < 0; i = i.add(BigInteger.ONE)) {
                yield(frame, block, fixnumOrBignumNode.fixnumOrBignum(i));
            }

            return n;
        }

    }

    @CoreMethod(names = { "to_i", "to_int" })
    public abstract static class ToINode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int toI(int n) {
            return n;
        }

        @Specialization
        public long toI(long n) {
            return n;
        }

        @Specialization(guards = "isRubyBignum(n)")
        public DynamicObject toI(DynamicObject n) {
            return n;
        }

    }

    @CoreMethod(names = "upto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode uptoInternalCall;

        @Specialization
        public Object upto(VirtualFrame frame, int from, int to, DynamicObject block) {
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
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return nil();
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, double to, DynamicObject block) {
            return upto(frame, from, (int) Math.floor(to), block);
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, long to, DynamicObject block) {
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
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return nil();
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, double to, DynamicObject block) {
            return upto(frame, from, (long) Math.ceil(to), block);
        }

        @Specialization(guards = "isDynamicObject(from) || isDynamicObject(to)")
        public Object upto(VirtualFrame frame, Object from, Object to, DynamicObject block) {
            if (uptoInternalCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                uptoInternalCall = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            return uptoInternalCall.callWithBlock(frame, from, "upto_internal", block, to);
        }

    }

}
