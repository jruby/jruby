package org.jruby.ext;

import jnr.constants.platform.Errno;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;

public class JRubyPOSIXHelper {
    /**
     * Helper for handling common POSIX situations where a negative return value
     * from a function call indicates an error, and errno must be consulted to
     * determine how exactly the function failed.
     * @param runtime Ruby runtime
     * @param result return value of a POSIX call
     */

    public static void checkErrno(Ruby runtime, int result) {
        if (result < 0) {
        // FIXME: The error message is a bit off.
        // e.g., No such process - No such process (Errno::ESRCH)
        // Note the repetition of 'No such process'.
            Errno errno = Errno.valueOf(runtime.getPosix().errno());
            String name = errno.name();
            String msg  = errno.toString();
            RubyClass errnoClass = runtime.getErrno().getClass(runtime.getCurrentContext(), name);
            if (errnoClass != null) {
                throw RaiseException.from(runtime, errnoClass, msg);
            }
        }
    }

}
