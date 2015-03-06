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
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

import java.math.BigInteger;

@CoreClass(name = "Integer")
public abstract class IntegerNodes {

    @CoreMethod(names = "downto", needsBlock = true, required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class DownToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public DownToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DownToNode(DownToNode prev) {
            super(prev);
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
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
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

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object downto(VirtualFrame frame, int from, double to, RubyProc block) {
            notDesignedForCompilation("4331ea794e124d19886b34a3205d40df");
            return downto(frame, from, (int) Math.ceil(to), block);
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimesNode(TimesNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization
        public RubyArray times(VirtualFrame frame, int n, UndefinedPlaceholder block) {
            notDesignedForCompilation("cfb9b1a6e1684e718b939b8b5243c89e");

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
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
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
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
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
            notDesignedForCompilation("90d984f1de18406ebcaf9481ad2a7168");

            if (fixnumOrBignum == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignum = insert(new FixnumOrBignumNode(getContext(), getSourceSection()));
            }

            outer: for (BigInteger i = BigInteger.ZERO; i.compareTo(n.bigIntegerValue()) < 0; i = i.add(BigInteger.ONE)) {
                while (true) {
                    try {
                        yield(frame, block, fixnumOrBignum.fixnumOrBignum(i));
                        continue outer;
                    } catch (BreakException e) {
                        breakProfile.enter();
                        return e.getResult();
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
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
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

    @CoreMethod(names = "upto", needsBlock = true, required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public UpToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpToNode(UpToNode prev) {
            super(prev);
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
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
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

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, double to, RubyProc block) {
            notDesignedForCompilation("9566ac2a936b4affa220629ada3510d3");
            return upto(frame, from, (int) Math.floor(to), block);
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, long to, RubyProc block) {
            notDesignedForCompilation("b66372fe472d497dae86f87a08d4b089");

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
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
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

            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
