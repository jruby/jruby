package org.jruby.util;

import java.util.concurrent.TimeUnit;

import jnr.posix.POSIX;
import jnr.posix.Timespec;
import org.jruby.Ruby;
import org.jruby.platform.Platform;

/**
 * The clock behind Process::CLOCK_MONOTONIC and JRuby's own timeouts, so the two agree.
 *
 * On Linux System.nanoTime() reads CLOCK_MONOTONIC, the same clock CRuby uses. On macOS it reads a clock that
 * stops while the system sleeps, but CLOCK_MONOTONIC there keeps counting, so read that through native POSIX
 * when it is available.
 */
public final class MonotonicClock {
    private MonotonicClock() {}

    /** Darwin's clockid_t for CLOCK_MONOTONIC */
    private static final int DARWIN_CLOCK_MONOTONIC = 6;

    private static POSIX darwinPosix(Ruby runtime) {
        if (!Platform.IS_MAC) return null;

        POSIX posix = runtime.getPosix();
        return posix.isNative() ? posix : null;
    }

    /**
     * @return the current time of the monotonic clock in nanoseconds, only meaningful relative to another call
     *         for the same runtime
     */
    public static long nanoTime(Ruby runtime) {
        POSIX posix = darwinPosix(runtime);
        if (posix != null) {
            Timespec ts = posix.allocateTimespec();
            if (posix.clock_gettime(DARWIN_CLOCK_MONOTONIC, ts) == 0) return toNanos(ts);
        }
        return System.nanoTime();
    }

    /**
     * @return the resolution of {@link #nanoTime(Ruby)} in nanoseconds
     */
    public static long resolutionNanos(Ruby runtime) {
        POSIX posix = darwinPosix(runtime);
        if (posix != null) {
            Timespec ts = posix.allocateTimespec();
            if (posix.clock_getres(DARWIN_CLOCK_MONOTONIC, ts) == 0) return toNanos(ts);
        }
        return 1;
    }

    private static long toNanos(Timespec ts) {
        return TimeUnit.SECONDS.toNanos(ts.sec()) + ts.nsec();
    }
}
