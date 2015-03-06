/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.signal.SignalOperations;

import sun.misc.Signal;

@CoreClass(name = "Process")
public abstract class ProcessNodes {

    public static final int CLOCK_MONOTONIC = 1;
    public static final int CLOCK_REALTIME = 2;

    @CoreMethod(names = "clock_gettime", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class ClockGetTimeNode extends CoreMethodNode {

        private final RubySymbol floatSecondSymbol;
        private final RubySymbol nanosecondSymbol;

        public ClockGetTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            floatSecondSymbol = context.newSymbol("float_second");
            nanosecondSymbol = context.newSymbol("nanosecond");
        }

        public ClockGetTimeNode(ClockGetTimeNode prev) {
            super(prev);
            floatSecondSymbol = prev.floatSecondSymbol;
            nanosecondSymbol = prev.nanosecondSymbol;
        }

        @Specialization(guards = "isMonotonic(arguments[0])")
        Object clock_gettime_monotonic(int clock_id, UndefinedPlaceholder unit) {
            return clock_gettime_monotonic(CLOCK_MONOTONIC, floatSecondSymbol);
        }

        @Specialization(guards = "isRealtime(arguments[0])")
        Object clock_gettime_realtime(int clock_id, UndefinedPlaceholder unit) {
            return clock_gettime_realtime(CLOCK_REALTIME, floatSecondSymbol);
        }

        @Specialization(guards = "isMonotonic(arguments[0])")
        Object clock_gettime_monotonic(int clock_id, RubySymbol unit) {
            long time = System.nanoTime();
            return timeToUnit(time, unit);
        }

        @Specialization(guards = "isRealtime(arguments[0])")
        Object clock_gettime_realtime(int clock_id, RubySymbol unit) {
            long time = System.currentTimeMillis() * 1000000;
            return timeToUnit(time, unit);
        }

        Object timeToUnit(long time, RubySymbol unit) {
            if (unit == nanosecondSymbol) {
                return time;
            } else if (unit == floatSecondSymbol) {
                return time / 1e9;
            } else {
                throw new UnsupportedOperationException(unit.toString());
            }
        }

        static boolean isMonotonic(int clock_id) {
            return clock_id == CLOCK_MONOTONIC;
        }

        static boolean isRealtime(int clock_id) {
            return clock_id == CLOCK_REALTIME;
        }

    }

    @CoreMethod(names = "kill", isModuleFunction = true, required = 2)
    public abstract static class KillNode extends CoreMethodNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KillNode(KillNode prev) {
            super(prev);
        }

        @Specialization
        public int kill(RubySymbol signalName, int pid) {
            notDesignedForCompilation("7d7ea483d671490497f72a8c2d3454e4");

            int self = getContext().getRuntime().getPosix().getpid();

            if (self == pid) {
                Signal signal = new Signal(signalName.toString());

                SignalOperations.raise(signal);
                return 1;
            } else {
                throw new UnsupportedOperationException();
            }
        }

    }

    @CoreMethod(names = "pid", isModuleFunction = true)
    public abstract static class PidNode extends CoreMethodNode {

        public PidNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PidNode(PidNode prev) {
            super(prev);
        }

        @Specialization
        public int pid() {
            notDesignedForCompilation("20e260ef11a242438615839efdf11890");

            return getContext().getRuntime().getPosix().getpid();
        }

    }

}
