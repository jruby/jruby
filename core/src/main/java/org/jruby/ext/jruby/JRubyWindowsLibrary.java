package org.jruby.ext.jruby;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyProcess;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ThreadedRunnable;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ShellLauncher;

import java.io.IOException;
import java.util.Arrays;

/**
 * Special cases and hacks to better support Windows
 */
public class JRubyWindowsLibrary implements Library {

    @Deprecated
    public static POpenTuple popenSpecial(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        try {
            ShellLauncher.POpenProcess process = ShellLauncher.popen3(runtime, args, false);
            RubyIO input = process.getInput() != null ?
                    new RubyIO(runtime, process.getInput()) :
                    new RubyIO(runtime, process.getInputStream());
            RubyIO output = process.getOutput() != null ?
                    new RubyIO(runtime, process.getOutput()) :
                    new RubyIO(runtime, process.getOutputStream());
            RubyIO error = process.getError() != null ?
                    new RubyIO(runtime, process.getError()) :
                    new RubyIO(runtime, process.getErrorStream());

            // ensure the OpenFile knows it's a process; see OpenFile#finalize
            input.getOpenFile().setProcess(process);
            output.getOpenFile().setProcess(process);
            error.getOpenFile().setProcess(process);

            // set all streams as popenSpecial streams, so we don't shut down process prematurely
            input.setPopenSpecial(true);
            output.setPopenSpecial(true);
            error.setPopenSpecial(true);

            // process streams are not seekable
//            input.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);
//            output.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);
//            error.getOpenFile().getMainStreamSafe().getDescriptor().
//              setCanBeSeekable(false);

            return new POpenTuple(input, output, error, process);
//        } catch (BadDescriptorException e) {
//            throw runtime.newErrnoEBADFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    public void load(Ruby runtime, boolean wrap) {
        RubyModule windows = runtime.defineModuleUnder("Windows", runtime.getModule("JRuby"));
        windows.defineAnnotatedMethods(JRubyWindowsLibrary.class);
    }

    @JRubyMethod(name = "popen3", rest = true, meta = true)
    public static IRubyObject popen3(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;

        // TODO: handle opts
        if (args.length > 0 && args[args.length - 1] instanceof RubyHash) {
            args = Arrays.copyOf(args, args.length - 1);
        }

        final POpenTuple tuple = popenSpecial(context, args);
        final long pid = ShellLauncher.getPidFromProcess(tuple.process);

        // array trick to be able to reference enclosing RubyThread
        final RubyThread[] waitThread = new RubyThread[1];
        waitThread[0] = new RubyThread(
                runtime,
                (RubyClass) runtime.getClassFromPath("Process::WaitThread"),
                new ProcessWaitThread(waitThread, runtime, pid, tuple));

        RubyArray yieldArgs = RubyArray.newArrayLight(runtime,
                tuple.output,
                tuple.input,
                tuple.error,
                waitThread[0]);

        if (block.isGiven()) {
            try {
                return block.yield(context, yieldArgs);
            } finally {
                cleanupPOpen(tuple);

                IRubyObject status = waitThread[0].join(IRubyObject.NULL_ARRAY);
                context.setLastExitStatus(status);
            }
        }

        return yieldArgs;
    }

    public static IRubyObject popen4(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;

        try {
            JRubyWindowsLibrary.POpenTuple tuple = JRubyWindowsLibrary.popenSpecial(context, args);

            RubyArray yieldArgs = RubyArray.newArrayLight(runtime,
                    runtime.newFixnum(ShellLauncher.getPidFromProcess(tuple.process)),
                    tuple.output,
                    tuple.input,
                    tuple.error);

            if (block.isGiven()) {
                try {
                    return block.yield(context, yieldArgs);
                } finally {
                    cleanupPOpen(tuple);
                    // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                    context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple.process.waitFor() << 8, ShellLauncher.getPidFromProcess(tuple.process)));
                }
            }
            return yieldArgs;
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    private static void cleanupPOpen(JRubyWindowsLibrary.POpenTuple tuple) {
        if (tuple.input.getOpenFile().isOpen()) {
            try {
                tuple.input.close();
            } catch (RaiseException re) {}
        }
        if (tuple.output.getOpenFile().isOpen()) {
            try {
                tuple.output.close();
            } catch (RaiseException re) {}
        }
        if (tuple.error.getOpenFile().isOpen()) {
            try {
                tuple.error.close();
            } catch (RaiseException re) {}
        }
    }

    private static class POpenTuple {
        public POpenTuple(RubyIO i, RubyIO o, RubyIO e, Process p) {
            input = i; output = o; error = e; process = p;
        }
        public final RubyIO input;
        public final RubyIO output;
        public final RubyIO error;
        public final Process process;
    }

    private static class ProcessWaitThread implements ThreadedRunnable {

        private final RubyThread[] waitThread;
        private final Ruby runtime;
        private final long pid;
        private final POpenTuple tuple;
        volatile Thread javaThread;

        public ProcessWaitThread(RubyThread[] waitThread, Ruby runtime, long pid, POpenTuple tuple) {
            this.waitThread = waitThread;
            this.runtime = runtime;
            this.pid = pid;
            this.tuple = tuple;
        }

        @Override
        public Thread getJavaThread() {
            return javaThread;
        }

        @Override
        public void run() {
            javaThread = Thread.currentThread();
            RubyThread rubyThread;
            // spin a bit until this happens; should almost never spin
            while ((rubyThread = waitThread[0]) == null) {
                Thread.yield();
            }

            runtime.getThreadService().registerNewThread(rubyThread);

            rubyThread.op_aset(runtime.newSymbol("pid"),  runtime.newFixnum(pid));

            try {
                int exitValue = tuple.process.waitFor();

                // RubyStatus uses real native status now, so we unshift Java's shifted exit status
                RubyProcess.RubyStatus status = RubyProcess.RubyStatus.newProcessStatus(
                        runtime,
                        exitValue << 8,
                        pid);

                rubyThread.cleanTerminate(status);
            } catch (Throwable t) {
                rubyThread.exceptionRaised(t);
            } finally {
                rubyThread.dispose();
            }
        }

    }
}
