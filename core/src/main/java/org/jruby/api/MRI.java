package org.jruby.api;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.function.Supplier;

public class MRI {
    public static IRubyObject rb_sys_fail_path(ThreadContext context, String path) {
        return API.sysFailWithPath(context, path);
    }

    public static int rb_pipe(ThreadContext context, int[] pipes) {
        return API.newPipe(context, pipes);
    }

    public static int rb_cloexec_pipe(ThreadContext context, int[] pipes) {
        return API.cloexecPipe(context, pipes);
    }

    public static void rb_maygvl_fd_fix_cloexec(ThreadContext context, int fd) {
        API.fdFixCloexec(context, fd);
    }

    public static <T> T rb_rescue_typeerror(ThreadContext context, T dflt, Supplier<T> func) {
        return API.rescueTypeError(context, dflt, func);
    }
}
