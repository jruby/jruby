package org.jruby.api;

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.PosixShim;

import java.util.function.Supplier;

import static org.jruby.api.Error.typeError;

public class API {
    /**
     * Equivalent to rb_sys_fail_path(ThreadContext, String).
     *
     * @param context
     * @param path
     * @return
     */
    public static IRubyObject sysFailWithPath(ThreadContext context, String path) {
        throw context.runtime.newSystemCallError("bad path for cloexec: " + path);
    }

    /**
     * Equivalent to {@link MRI#rb_pipe(ThreadContext, int[])}.
     *
     * @param context
     * @param pipes
     * @return
     */
    public static int newPipe(ThreadContext context, int[] pipes) {
        int ret;
        ret = cloexecPipe(context, pipes);
        return ret;
    }

    /**
     * Equivalent to {@link MRI#rb_cloexec_pipe(ThreadContext, int[])}.
     *
     * @param context
     * @param pipes
     * @return
     */
    public static int cloexecPipe(ThreadContext context, int[] pipes) {
        int ret;

        ret = context.runtime.getPosix().pipe(pipes);

        if (ret == -1) return -1;

        fdFixCloexec(context, pipes[0]);
        fdFixCloexec(context, pipes[1]);
        return ret;
    }

    /**
     * Equivalent to MRI#rb_maygvl_fd_fix_cloexec(ThreadContext, int).
     *
     * @param context
     * @param fd
     */
    public static void fdFixCloexec(ThreadContext context, int fd) {
        PosixShim shim = new PosixShim(context.runtime);
        OpenFile.fdFixCloexec(shim, fd);
    }

    /**
     * Equivalent to MRI#rb_rescue_typeerror(ThreadContext, Object, Supplier).
     *
     * @param context
     * @param dflt
     * @param func
     * @return
     * @param <T>
     */
    public static <T> T rescueTypeError(ThreadContext context, T dflt, Supplier<T> func) {
        boolean exceptionRequiresBacktrace = context.exceptionRequiresBacktrace;
        try {
            context.setExceptionRequiresBacktrace(false);

            return func.get();
        } catch (TypeError te) {
            return dflt;
        } finally {
            context.setExceptionRequiresBacktrace(exceptionRequiresBacktrace);
        }
    }

    public static class ModeAndPermission {
        public IRubyObject mode;
        public IRubyObject permission;

        public ModeAndPermission(IRubyObject mode, IRubyObject permission) {
            this.mode = mode;
            this.permission = permission;
        }
    }
}
