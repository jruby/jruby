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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Integer")
public abstract class IntegerNodes {

    @CoreMethod(names = "chr")
    public abstract static class ChrNode extends CoreMethodNode {

        public ChrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChrNode(ChrNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chr(int n) {
            notDesignedForCompilation();

            // TODO(CS): not sure about encoding here
            return getContext().makeString((char) n);
        }

    }

    @CoreMethod(names = "floor")
    public abstract static class FloorNode extends CoreMethodNode {

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FloorNode(FloorNode prev) {
            super(prev);
        }

        @Specialization
        public int floor(int n) {
            return n;
        }

        @Specialization
        public long floor(long n) {
            return n;
        }

        @Specialization
        public RubyBignum floor(RubyBignum n) {
            return n;
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimesNode(TimesNode prev) {
            super(prev);
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
            notDesignedForCompilation();

            outer: for (RubyBignum i = bignum(0); i.compare(n) < 0; i = i.add(1)) {
                while (true) {
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

    @CoreMethod(names = "upto", needsBlock = true, required = 1)
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
