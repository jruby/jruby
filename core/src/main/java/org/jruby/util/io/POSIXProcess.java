package org.jruby.util.io;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Signal;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

import java.io.InputStream;
import java.io.OutputStream;

/**
* Created by headius on 7/22/14.
*/
public class POSIXProcess extends Process {
    private final Ruby runtime;
    private final long finalPid;
    volatile Integer exitValue;
    volatile Integer status;

    public POSIXProcess(Ruby runtime, long finalPid) {
        this.runtime = runtime;
        this.finalPid = finalPid;
        exitValue = null;
    }

    @Override
    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public synchronized int waitFor() throws InterruptedException {
        if (exitValue == null) {
            int[] stat_loc = {0};
            Ruby runtime = this.runtime;
            ThreadContext context = runtime.getCurrentContext();

            context.blockingThreadPoll();

            retry: while (true) {
                stat_loc[0] = 0;
                // TODO: investigate WNOHANG
                int result = runtime.getPosix().waitpid((int)finalPid, stat_loc, 0);
                if (result == -1) {
                    Errno errno = Errno.valueOf(runtime.getPosix().errno());
                    switch (errno) {
                        case EINTR:
                            context.pollThreadEvents();
                            continue retry;
                        case ECHILD:
                            return -1;
                        default:
                            throw new RuntimeException("unexpected waitpid errno: " + Errno.valueOf(runtime.getPosix().errno()));
                    }
                }
                break;
            }
            // FIXME: Is this different across platforms? Got it all from Darwin's wait.h

            status = stat_loc[0];
            if (PosixShim.WAIT_MACROS.WIFEXITED((long)status)) {
                exitValue = PosixShim.WAIT_MACROS.WEXITSTATUS((long)status);
            } else if (PosixShim.WAIT_MACROS.WIFSIGNALED((long)status)) {
                exitValue = PosixShim.WAIT_MACROS.WTERMSIG((long)status);
            } else if (PosixShim.WAIT_MACROS.WIFSTOPPED((long)status)) {
                exitValue = PosixShim.WAIT_MACROS.WSTOPSIG((long)status);
            }
        }

        return exitValue;
    }

    @Override
    public int exitValue() {
        try {
            return waitFor();
        } catch (InterruptedException ie) {
            throw new IllegalThreadStateException();
        }
    }

    @Override
    public void destroy() {
        runtime.getPosix().kill((int)finalPid, Signal.SIGTERM.intValue());
    }

    public int status() {
        try {
            waitFor();
        } catch (InterruptedException ie) {
            throw new IllegalThreadStateException();
        }
        return status == null ? -1 : status;
    }
}
