/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.timeout;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyRegexp;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.RegexpOptions;

public class Timeout implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule timeout = runtime.defineModule("Timeout");
        RubyClass superclass = runtime.is1_9() ? runtime.getRuntimeError() : runtime.getInterrupt();
        RubyClass timeoutError = runtime.defineClassUnder("Error", superclass, superclass.getAllocator(), timeout);
        runtime.defineClassUnder("ExitException", runtime.getException(), runtime.getException().getAllocator(), timeout);

        // Here we create an "anonymous" exception type used for unrolling the stack.
        // MRI creates a new one for *every call* to timeout, which can be costly.
        // We opt to use a single exception type for all cases to avoid this overhead.
        RubyClass anonEx = runtime.defineClassUnder("AnonymousException", runtime.getException(), runtime.getException().getAllocator(), timeout);
        anonEx.setBaseName(null); // clear basename so it's anonymous when raising

        // These are not really used by timeout, but exposed for compatibility
        timeout.defineConstant("THIS_FILE", RubyRegexp.newRegexp(runtime, "timeout\\.rb", new RegexpOptions()));
        timeout.defineConstant("CALLER_OFFSET", RubyFixnum.newFixnum(runtime, 0));

        // Timeout module methods
        timeout.defineAnnotatedMethods(Timeout.class);

        // Toplevel defines
        runtime.getObject().defineConstant("TimeoutError", timeoutError);
        runtime.getObject().defineAnnotatedMethods(TimeoutToplevel.class);
    }

    private static ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new DaemonThreadFactory());

    public static class TimeoutToplevel {
        @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE)
        public static IRubyObject timeout(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            RubyModule timeout = context.runtime.getModule("Timeout");
            
            switch (args.length) {
            case 1:
                return Timeout.timeout(context, timeout, args[0], block);
            case 2:
                return Timeout.timeout(context, timeout, args[0], args[1], block);
            default:
                Arity.raiseArgumentError(context.runtime, args.length, 1, 2);
                return context.runtime.getNil();
            }
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject timeout, IRubyObject seconds, Block block) {
        // No seconds, just yield
        if (seconds.isNil() || Helpers.invoke(context, seconds, "zero?").isTrue()) {
            return block.yieldSpecific(context);
        }

        final Ruby runtime = context.runtime;

        // No timeout in critical section
        if (runtime.getThreadService().getCritical()) {
            return raiseBecauseCritical(context);
        }

        final RubyThread currentThread = context.getThread();
        final AtomicBoolean latch = new AtomicBoolean(false);

        IRubyObject id = new RubyObject(runtime, runtime.getObject());
        Runnable timeoutRunnable = prepareRunnable(currentThread, runtime, latch, id);
        Future timeoutFuture = null;

        try {
            try {
                timeoutFuture = timeoutExecutor.schedule(timeoutRunnable,
                        (long)(seconds.convertToFloat().getDoubleValue() * 1000000), TimeUnit.MICROSECONDS);

                return block.yield(context, seconds);
            } finally {
                killTimeoutThread(context, timeoutFuture, latch);
            }
        } catch (RaiseException re) {
            if (re.getException().getInternalVariable("__identifier__") == id) {
                return raiseTimeoutError(context, re);
            } else {
                throw re;
            }
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject timeout, IRubyObject seconds, IRubyObject exceptionType, Block block) {
        // No seconds, just yield
        if (seconds.isNil() || Helpers.invoke(context, seconds, "zero?").isTrue()) {
            return block.yieldSpecific(context);
        }

        final Ruby runtime = context.runtime;

        // No timeout in critical section
        if (runtime.getThreadService().getCritical()) {
            return raiseBecauseCritical(context);
        }

        final RubyThread currentThread = context.getThread();
        final AtomicBoolean latch = new AtomicBoolean(false);

        IRubyObject id = new RubyObject(runtime, runtime.getObject());
        RubyClass anonException = (RubyClass)runtime.getClassFromPath("Timeout::AnonymousException");
        Runnable timeoutRunnable = exceptionType.isNil() ?
                prepareRunnable(currentThread, runtime, latch, id) :
                prepareRunnableWithException(currentThread, exceptionType, runtime, latch);
        Future timeoutFuture = null;

        try {
            try {
                timeoutFuture = timeoutExecutor.schedule(timeoutRunnable,
                        (long)(seconds.convertToFloat().getDoubleValue() * 1000000), TimeUnit.MICROSECONDS);

                return block.yield(context, seconds);
            } finally {
                killTimeoutThread(context, timeoutFuture, latch);
            }
        } catch (RaiseException re) {
            // if it's the exception we're expecting
            if (re.getException().getMetaClass() == anonException) {
                // and we were not given a specific exception
                if (exceptionType.isNil()) {
                    // and it's the exception intended for us
                    if (re.getException().getInternalVariable("__identifier__") == id) {
                        return raiseTimeoutError(context, re);
                    }
                }
            }

            // otherwise, rethrow
            throw re;
        }
    }

    private static Runnable prepareRunnable(final RubyThread currentThread, final Ruby runtime, final AtomicBoolean latch, final IRubyObject id) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                if (latch.compareAndSet(false, true)) {
                    if (currentThread.alive_p().isTrue()) {
                        RubyClass anonException = (RubyClass)runtime.getClassFromPath("Timeout::AnonymousException");
                        IRubyObject anonExceptionObj = anonException.newInstance(runtime.getCurrentContext(), runtime.newString("execution expired"), Block.NULL_BLOCK);
                        anonExceptionObj.getInternalVariables().setInternalVariable("__identifier__", id);
                        currentThread.internalRaise(new IRubyObject[] {anonExceptionObj});
                    }
                }
            }
        };
        return timeoutRunnable;
    }

    private static Runnable prepareRunnableWithException(final RubyThread currentThread, final IRubyObject exception, final Ruby runtime, final AtomicBoolean latch) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                if (latch.compareAndSet(false, true)) {
                    if (currentThread.alive_p().isTrue()) {
                        currentThread.internalRaise(new IRubyObject[]{exception, runtime.newString("execution expired")});
                    }
                }
            }
        };
        return timeoutRunnable;
    }

    private static void killTimeoutThread(ThreadContext context, Future timeoutFuture, AtomicBoolean latch) {
        if (latch.compareAndSet(false, true) && timeoutFuture.cancel(false)) {
            // ok, exception will not fire
            if (timeoutExecutor instanceof ScheduledThreadPoolExecutor && timeoutFuture instanceof Runnable) {
                ((ScheduledThreadPoolExecutor) timeoutExecutor).remove((Runnable) timeoutFuture);
            }
        } else {
            // future is not cancellable, wait for it to run and then poll
            try {
                timeoutFuture.get();
            } catch (ExecutionException ex) {
            } catch (InterruptedException ex) {
            }

            // poll to propagate exception from child thread
            context.pollThreadEvents();
        }
    }

    private static IRubyObject raiseBecauseCritical(ThreadContext context) {
        Ruby runtime = context.runtime;

        return RubyKernel.raise(context, runtime.getKernel(), new IRubyObject[]{runtime.getThreadError(), runtime.newString("timeout within critical section")}, Block.NULL_BLOCK);
    }

    private static IRubyObject raiseTimeoutError(ThreadContext context, RaiseException re) {
        Ruby runtime = context.runtime;

        return RubyKernel.raise(
                context,
                runtime.getKernel(),
                new IRubyObject[]{
                    runtime.getClassFromPath("Timeout::Error"),
                    re.getException().callMethod(context, "message"),
                    re.getException().callMethod(context, "backtrace")},
                Block.NULL_BLOCK);
    }
}
