package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.PosixShim;

public class API {
    public static IRubyObject rb_sys_fail_path(Ruby runtime, String path) {
        throw runtime.newSystemCallError("bad path for cloexec: " + path);
    }

    public static int rb_pipe(Ruby runtime, int[] pipes) {
        int ret;
        ret = rb_cloexec_pipe(runtime, pipes);
//        if (ret == -1) {
//            if (rb_gc_for_fd(errno)) {
//                ret = rb_cloexec_pipe(pipes);
//            }
//        }
//        if (ret == 0) {
//            rb_update_max_fd(pipes[0]);
//            rb_update_max_fd(pipes[1]);
//        }
        return ret;
    }

    public static int rb_cloexec_pipe(Ruby runtime, int[] pipes) {
        int ret;

//#if defined(HAVE_PIPE2)
//        static int try_pipe2 = 1;
//        if (try_pipe2) {
//            ret = runtime.posix.pippipe2(fildes, O_CLOEXEC);
//            if (ret != -1)
//                return ret;
//        /* pipe2 is available since Linux 2.6.27, glibc 2.9. */
//            if (errno == ENOSYS) {
//                try_pipe2 = 0;
//                ret = pipe(fildes);
//            }
//        }
//        else {
//            ret = pipe(fildes);
//        }
//#else
        ret = runtime.getPosix().pipe(pipes);
//#endif
        if (ret == -1) return -1;
//#ifdef __CYGWIN__
//        if (ret == 0 && fildes[1] == -1) {
//            close(fildes[0]);
//            fildes[0] = -1;
//            errno = ENFILE;
//            return -1;
//        }
//#endif
        rb_maygvl_fd_fix_cloexec(runtime, pipes[0]);
        rb_maygvl_fd_fix_cloexec(runtime, pipes[1]);
        return ret;
    }

    public static void rb_maygvl_fd_fix_cloexec(Ruby runtime, int fd) {
        PosixShim shim = new PosixShim(runtime);
        OpenFile.fdFixCloexec(shim, fd);
    }
}
