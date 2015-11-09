package org.jruby.truffle.runtime.ffi;

import jnr.ffi.Struct;

public final class TimeSpec extends Struct {
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
