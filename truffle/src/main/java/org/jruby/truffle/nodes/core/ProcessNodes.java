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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Struct;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.DefaultValueNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.runtime.signal.SignalOperations;
import sun.misc.Signal;

@SuppressWarnings("restriction")
@CoreClass(name = "Process")
public abstract class ProcessNodes {

    public final static class TimeSpec extends Struct {
        public final time_t tv_sec = new time_t();
        public final SignedLong tv_nsec = new SignedLong();

        public TimeSpec(jnr.ffi.Runtime runtime) {
            super(runtime);
        }

        public long getTVsec() {
            return tv_sec.get();
        }

        public long getTVnsec() {
            return tv_nsec.get();
        }
    }

    public interface LibCClockGetTime {
        int clock_gettime(int clock_id, TimeSpec timeSpec);
    }

    public static final int CLOCK_MONOTONIC = 1;
    public static final int CLOCK_REALTIME = 2;
    public static final int CLOCK_THREAD_CPUTIME_ID = 3; // Linux only

    @CoreMethod(names = "clock_gettime", onSingleton = true, required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "clock_id"),
            @NodeChild(type = RubyNode.class, value = "unit")
    })
    public abstract static class ClockGetTimeNode extends CoreMethodNode {

        private final DynamicObject floatSecondSymbol = getContext().getSymbol("float_second");
        private final DynamicObject nanosecondSymbol = getContext().getSymbol("nanosecond");

        public ClockGetTimeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("unit")
        public RubyNode coerceUnit(RubyNode unit) {
            return DefaultValueNodeGen.create(getContext(), getSourceSection(), floatSecondSymbol, unit);
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

        @TruffleBoundary
        @Specialization(guards = { "isThreadCPUTime(clock_id)", "isRubySymbol(unit)" })
        protected Object clock_gettime_thread_cputime(int clock_id, DynamicObject unit,
                @Cached("getLibCClockGetTime()") LibCClockGetTime libCClockGetTime) {
            TimeSpec timeSpec = new TimeSpec(jnr.ffi.Runtime.getRuntime(libCClockGetTime));
            int r = libCClockGetTime.clock_gettime(CLOCK_THREAD_CPUTIME_ID, timeSpec);
            if (r != 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().systemCallError("clock_gettime failed: " + r, this));
            }
            long nanos = timeSpec.getTVsec() * 1_000_000_000 + timeSpec.getTVnsec();
            return timeToUnit(nanos, unit);
        }

        private Object timeToUnit(long time, DynamicObject unit) {
            assert RubyGuards.isRubySymbol(unit);
            if (unit == nanosecondSymbol) {
                return time;
            } else if (unit == floatSecondSymbol) {
                return time / 1e9;
            } else {
                throw new UnsupportedOperationException(SymbolNodes.getString(unit));
            }
        }

        protected static boolean isMonotonic(int clock_id) {
            return clock_id == CLOCK_MONOTONIC;
        }

        protected static boolean isRealtime(int clock_id) {
            return clock_id == CLOCK_REALTIME;
        }

        protected static boolean isThreadCPUTime(int clock_id) {
            return clock_id == CLOCK_THREAD_CPUTIME_ID;
        }

        protected static LibCClockGetTime getLibCClockGetTime() {
            return LibraryLoader.create(LibCClockGetTime.class).library("c").load();
        }

    }

    @CoreMethod(names = "kill", onSingleton = true, required = 2)
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubySymbol(signalName)")
        public int kill(DynamicObject signalName, int pid) {
            int self = posix().getpid();

            if (self == pid) {
                Signal signal = new Signal(SymbolNodes.getString(signalName));

                SignalOperations.raise(signal);
                return 1;
            } else {
                throw new UnsupportedOperationException();
            }
        }

    }

    @CoreMethod(names = "pid", onSingleton = true)
    public abstract static class PidNode extends CoreMethodArrayArgumentsNode {

        public PidNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int pid() {
            return posix().getpid();
        }

    }

}
