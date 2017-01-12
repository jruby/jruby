/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.core.cast.DefaultValueNodeGen;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.platform.posix.ClockGetTime;
import org.jruby.truffle.platform.posix.TimeSpec;

@CoreClass("Process")
public abstract class ProcessNodes {

    // These are just distinct values, not clock_gettime(3) values.
    public static final int CLOCK_MONOTONIC = 1;
    public static final int CLOCK_REALTIME = 2;
    public static final int CLOCK_THREAD_CPUTIME = 3; // Linux only
    public static final int CLOCK_MONOTONIC_RAW = 4; // Linux only

    @CoreMethod(names = "clock_gettime", onSingleton = true, required = 1, optional = 1, lowerFixnum = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "clock_id"),
            @NodeChild(type = RubyNode.class, value = "unit")
    })
    public abstract static class ClockGetTimeNode extends CoreMethodNode {

        public static final int CLOCK_THREAD_CPUTIME_ID = 3; // Linux only
        public static final int CLOCK_MONOTONIC_RAW_ID = 4; // Linux only

        private final DynamicObject floatSecondSymbol = getSymbol("float_second");
        private final DynamicObject floatMicrosecondSymbol = getSymbol("float_microsecond");
        private final DynamicObject nanosecondSymbol = getSymbol("nanosecond");

        @CreateCast("unit")
        public RubyNode coerceUnit(RubyNode unit) {
            return DefaultValueNodeGen.create(floatSecondSymbol, unit);
        }

        @Specialization(guards = { "isMonotonic(clock_id)", "isRubySymbol(unit)" })
        protected Object clock_gettime_monotonic(int clock_id, DynamicObject unit) {
            long time = System.nanoTime();
            return timeToUnit(time, unit);
        }

        @Specialization(guards = { "isRealtime(clock_id)", "isRubySymbol(unit)" })
        protected Object clock_gettime_realtime(int clock_id, DynamicObject unit) {
            long time = System.currentTimeMillis() * 1_000_000;
            return timeToUnit(time, unit);
        }

        @Specialization(guards = { "isThreadCPUTime(clock_id)", "isRubySymbol(unit)" })
        protected Object clock_gettime_thread_cputime(int clock_id, DynamicObject unit) {
            return clock_gettime_clock_id(CLOCK_THREAD_CPUTIME_ID, unit);
        }

        @Specialization(guards = { "isMonotonicRaw(clock_id)", "isRubySymbol(unit)" })
        protected Object clock_gettime_monotonic_raw(int clock_id, DynamicObject unit) {
            return clock_gettime_clock_id(CLOCK_MONOTONIC_RAW_ID, unit);
        }

        @TruffleBoundary
        private Object clock_gettime_clock_id(int clock_id, DynamicObject unit) {
            final ClockGetTime libCClockGetTime = getContext().getNativePlatform().getClockGetTime();
            TimeSpec timeSpec = new TimeSpec(jnr.ffi.Runtime.getRuntime(libCClockGetTime));
            int r = libCClockGetTime.clock_gettime(clock_id, timeSpec);
            if (r != 0) {
                throw new RaiseException(coreExceptions().systemCallError("clock_gettime failed: " + r, r, this));
            }
            long nanos = timeSpec.getTVsec() * 1_000_000_000 + timeSpec.getTVnsec();
            return timeToUnit(nanos, unit);
        }

        private Object timeToUnit(long time, DynamicObject unit) {
            assert RubyGuards.isRubySymbol(unit);
            if (unit == nanosecondSymbol) {
                return time;
            } else if (unit == floatMicrosecondSymbol) {
                return time / 1e3;
            } else if (unit == floatSecondSymbol) {
                return time / 1e9;
            } else {
                throw new UnsupportedOperationException(Layouts.SYMBOL.getString(unit));
            }
        }

        protected static boolean isMonotonic(int clock_id) {
            return clock_id == CLOCK_MONOTONIC;
        }

        protected static boolean isRealtime(int clock_id) {
            return clock_id == CLOCK_REALTIME;
        }

        protected static boolean isThreadCPUTime(int clock_id) {
            return clock_id == CLOCK_THREAD_CPUTIME;
        }

        protected static boolean isMonotonicRaw(int clock_id) {
            return clock_id == CLOCK_MONOTONIC_RAW;
        }

    }

    @CoreMethod(names = "pid", onSingleton = true)
    public abstract static class PidNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int pid() {
            return posix().getpid();
        }

    }

}
