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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyProc;

import java.math.BigInteger;

@CoreClass(name = "Integer")
public abstract class IntegerNodes {

    @CoreMethod(names = "downto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class DownToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public DownToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, int to, RubyProc block) {
            int count = 0;

            try {
                outer:
                for (int i = from; i >= to; i--) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return nil();
        }

        @Specialization
        public Object downto(VirtualFrame frame, long from, int to, RubyProc block) {
            return downto(frame, from, (long) to, block);
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, long to, RubyProc block) {
            return downto(frame, (long) from, to, block);
        }

        @Specialization
        public Object downto(VirtualFrame frame, long from, long to, RubyProc block) {
            // TODO BJF 22-Apr-2015 how to handle reportLoopCount(long)
            int count = 0;

            try {
                outer:
                for (long i = from; i >= to; i--) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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
            notDesignedForCompilation();
            return downto(frame, from, (int) Math.ceil(to), block);
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        // TODO CS 2-May-15 we badly need OSR in this node

        @Child private FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray times(VirtualFrame frame, int n, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final int[] array = new int[n];

            for (int i = 0; i < n; i++) {
                array[i] = i;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array, n);
        }

        @Specialization
        public Object times(VirtualFrame frame, int n, RubyProc block) {
            int count = 0;

            try {
                outer: for (int i = 0; i < n; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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
                outer: for (long i = 0; i < n; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return n;
        }

        @Specialization
        public Object times(VirtualFrame frame, RubyBignum n, RubyProc block) {
            notDesignedForCompilation();

            if (fixnumOrBignum == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignum = insert(new FixnumOrBignumNode(getContext(), getSourceSection()));
            }

            outer: for (BigInteger i = BigInteger.ZERO; i.compareTo(n.bigIntegerValue()) < 0; i = i.add(BigInteger.ONE)) {
                while (true) {
                    try {
                        yield(frame, block, fixnumOrBignum.fixnumOrBignum(i));
                        continue outer;
                    } catch (NextException e) {
                        nextProfile.enter();
                        continue outer;
                    } catch (RedoException e) {
                        redoProfile.enter();
                    }
                }
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

        @Specialization
        public RubyBignum toI(RubyBignum n) {
            return n;
        }

    }

    @CoreMethod(names = "upto", needsBlock = true, required = 1, returnsEnumeratorIfNoBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public UpToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, int to, RubyProc block) {
            int count = 0;

            try {
                outer:
                for (int i = from; i <= to; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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
            notDesignedForCompilation();
            return upto(frame, from, (int) Math.floor(to), block);
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, long to, RubyProc block) {
            notDesignedForCompilation();

            int count = 0;

            try {
                outer:
                for (long i = from; i <= to; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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
