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
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyRegexp;
import org.jruby.RubyThread;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
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
        RubyClass RuntimeError = runtime.getRuntimeError();
        RubyClass TimeoutError = runtime.defineClassUnder("Error", RuntimeError, RuntimeError.getAllocator(), timeout);
        timeout.defineConstant("ExitException", TimeoutError);

        // Here we create an "anonymous" exception type used for unrolling the stack.
        // MRI creates a new one for *every call* to timeout, which can be costly.
        // We opt to use a single exception type for all cases to avoid this overhead.
        RubyClass anonException = runtime.defineClassUnder("AnonymousException", runtime.getException(), runtime.getException().getAllocator(), timeout);
        anonException.setBaseName(null); // clear basename so it's anonymous when raising

        // These are not really used by timeout, but exposed for compatibility
        timeout.defineConstant("THIS_FILE", RubyRegexp.newRegexp(runtime, "timeout\\.rb", new RegexpOptions()));
        timeout.defineConstant("CALLER_OFFSET", RubyFixnum.newFixnum(runtime, 0));

        // Timeout module methods
        timeout.defineAnnotatedMethods(Timeout.class);

        // Toplevel defines
        runtime.getObject().defineConstant("TimeoutError", TimeoutError);
        runtime.getObject().deprecateConstant(runtime, "TimeoutError");
        runtime.getObject().defineAnnotatedMethods(TimeoutToplevel.class);
    }

    private static ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new DaemonThreadFactory());

    public static class TimeoutToplevel {
        @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE)
        public static IRubyObject timeout(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "Object#timeout is deprecated, use Timeout.timeout instead");
            return Helpers.invoke(context, context.runtime.getModule("Timeout"), "timeout", args, block);
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject recv, IRubyObject seconds, Block block) {
        IRubyObject timeout = context.runtime.getModule("Timeout");

        // No seconds, just yield
        if ( nilOrZeroSeconds(context, seconds) ) {
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
        Runnable timeoutRunnable = TimeoutTask.newAnnonymousTask(currentThread, timeout, latch, id);

        try {
            return yieldWithTimeout(context, seconds, block, timeoutRunnable, latch);
        }
        catch (RaiseException re) {
            raiseTimeoutErrorIfMatches(context, timeout, re, id);
            throw re;
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject recv, IRubyObject seconds, IRubyObject exceptionType, Block block) {
        IRubyObject timeout = context.runtime.getModule("Timeout");
        // No seconds, just yield
        if ( nilOrZeroSeconds(context, seconds) ) {
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
        Runnable timeoutRunnable = exceptionType.isNil() ?
                TimeoutTask.newAnnonymousTask(currentThread, timeout, latch, id) :
                TimeoutTask.newTaskWithException(currentThread, timeout, latch, exceptionType);

        try {
            return yieldWithTimeout(context, seconds, block, timeoutRunnable, latch);
        }
        catch (RaiseException re) {
            // if it's the exception we're expecting
            if (re.getException().getMetaClass() == getAnonymousException(timeout)) {
                // and we were not given a specific exception
                if ( exceptionType.isNil() ) {
                    raiseTimeoutErrorIfMatches(context, timeout, re, id);
                }
            }

            // otherwise, rethrow
            throw re;
        }
    }

    private static boolean nilOrZeroSeconds(final ThreadContext context, final IRubyObject seconds) {
        return seconds.isNil() || Helpers.invoke(context, seconds, "zero?").isTrue();
    }

    private static IRubyObject yieldWithTimeout(ThreadContext context,
        final IRubyObject seconds, final Block block,
        final Runnable runnable, final AtomicBoolean latch) throws RaiseException {

        final long micros = (long) ( RubyTime.convertTimeInterval(context, seconds) * 1000000 );
        Future timeoutFuture = null;
        try {
            timeoutFuture = timeoutExecutor.schedule(runnable, micros, TimeUnit.MICROSECONDS);
            return block.yield(context, seconds);
        }
        finally {
            if ( timeoutFuture != null ) killTimeoutThread(context, timeoutFuture, latch);
            // ... when timeoutFuture == null there's likely an error thrown from schedule
        }
    }

    private static class TimeoutTask implements Runnable {

        final RubyThread currentThread;
        final AtomicBoolean latch;

        final IRubyObject timeout; // Timeout module
        final IRubyObject id; // needed for 'anonymous' timeout (no exception passed)
        final IRubyObject exception; // if there's exception (type) passed to timeout

        private TimeoutTask(final RubyThread currentThread, final IRubyObject timeout,
            final AtomicBoolean latch, final IRubyObject id, final IRubyObject exception) {
            this.currentThread = currentThread;
            this.timeout = timeout;
            this.latch = latch;
            this.id = id;
            this.exception = exception;
        }

        static TimeoutTask newAnnonymousTask(final RubyThread currentThread, final IRubyObject timeout,
            final AtomicBoolean latch, final IRubyObject id) {
            return new TimeoutTask(currentThread, timeout, latch, id, null);
        }

        static TimeoutTask newTaskWithException(final RubyThread currentThread, final IRubyObject timeout,
            final AtomicBoolean latch, final IRubyObject exception) {
            return new TimeoutTask(currentThread, timeout, latch, null, exception);
        }

        public void run() {
            if ( latch.compareAndSet(false, true) ) {
                if ( exception == null ) {
                    raiseAnnonymous();
                }
                else {
                    raiseException();
                }
            }
        }

        private void raiseAnnonymous() {
            final Ruby runtime = timeout.getRuntime();
            IRubyObject anonException = getAnonymousException(timeout).newInstance(runtime.getCurrentContext(), runtime.newString("execution expired"), Block.NULL_BLOCK);
            anonException.getInternalVariables().setInternalVariable("__identifier__", id);
            currentThread.internalRaise(new IRubyObject[] { anonException });
        }

        private void raiseException() {
            final Ruby runtime = timeout.getRuntime();
            currentThread.internalRaise(new IRubyObject[]{ exception, runtime.newString("execution expired") });
        }

    }

    private static void killTimeoutThread(ThreadContext context, final Future timeoutFuture, final AtomicBoolean latch) {
        if (latch.compareAndSet(false, true) && timeoutFuture.cancel(false)) {
            // ok, exception will not fire
            if (timeoutExecutor instanceof ScheduledThreadPoolExecutor && timeoutFuture instanceof Runnable) {
                ((ScheduledThreadPoolExecutor) timeoutExecutor).remove((Runnable) timeoutFuture);
            }
        } else {
            // future is not cancellable, wait for it to run and then poll
            try {
                timeoutFuture.get();
            }
            catch (ExecutionException ex) {}
            catch (InterruptedException ex) {}
            // poll to propagate exception from child thread
            context.pollThreadEvents();
        }
    }

    private static IRubyObject raiseBecauseCritical(ThreadContext context) {
        Ruby runtime = context.runtime;

        return RubyKernel.raise(
                context,
                runtime.getKernel(),
                new IRubyObject[] {
                    runtime.getThreadError(),
                    runtime.newString("timeout within critical section")
                },
                Block.NULL_BLOCK);
    }

    private static IRubyObject raiseTimeoutErrorIfMatches(ThreadContext context,
        final IRubyObject timeout, final RaiseException ex, final IRubyObject id) {
        // check if it's the exception intended for us (@see prepareRunnable):
        if ( ex.getException().getInternalVariable("__identifier__") == id ) {
            final RubyException rubyException = ex.getException();

            return RubyKernel.raise( // throws
                    context,
                    context.runtime.getKernel(),
                    new IRubyObject[] {
                        getClassFrom(timeout, "Error"), // Timeout::Error
                        rubyException.callMethod(context, "message"),
                        rubyException.callMethod(context, "backtrace")
                    },
                    Block.NULL_BLOCK);
        }
        return null;
    }

    // Timeout::AnonymousException (@see above)
    private static RubyClass getAnonymousException(final IRubyObject timeout) {
        return getClassFrom(timeout, "AnonymousException");
    }

    private static RubyClass getClassFrom(final IRubyObject timeout, final String name) {
        return ((RubyModule) timeout).getClass(name); // Timeout::[name]
    }

}
