package org.jruby.truffle.runtime.ffi;

public interface LibCClockGetTime {
    int clock_gettime(int clock_id, TimeSpec timeSpec);
}
