/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ByteList;

import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;

public class Timeout {
    public static final String EXECUTOR_VARIABLE = "__executor__";

    public static void load(Ruby runtime) {
        var context = runtime.getCurrentContext();
        var Timeout = defineModule(context, "Timeout").defineMethods(context, Timeout.class);

        var executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new DaemonThreadFactory());
        executor.setRemoveOnCancelPolicy(true);
        Timeout.setInternalVariable(EXECUTOR_VARIABLE, executor);

        context.runtime.pushPostExitFunction((ctxt) -> { executor.shutdown(); return 0;});
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject recv, IRubyObject seconds, Block block) {
        return timeout(context, recv, seconds, context.nil, block);
    }

    private static final ByteList TIMEOUT_MESSAGE = ByteList.create("execution expired");
    static { TIMEOUT_MESSAGE.setEncoding(UTF8Encoding.INSTANCE); }

    private static RubyString defaultTimeoutMessage(final ThreadContext context) {
        RubyString message = newString(context, TIMEOUT_MESSAGE);
        message.setFrozen(true);
        return message;
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject recv, IRubyObject seconds, IRubyObject exceptionType, Block block) {
        return timeout(context, recv, seconds, exceptionType, context.nil, block);
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject recv, IRubyObject seconds, IRubyObject exceptionType, IRubyObject message, Block block) {
        // No seconds, just yield
        if ( nilOrZeroSeconds(context, seconds) ) {
            return block.yieldSpecific(context);
        }

        final Ruby runtime = context.runtime;
        final RubyModule timeout = runtime.getModule("Timeout");

        final RubyThread currentThread = context.getThread();
        final AtomicBoolean latch = new AtomicBoolean(false);

        final Object id = exceptionType.isNil() ? new Object() : null;
        final RubyString exceptionMessage = message.isNil() ? defaultTimeoutMessage(context) : message.convertToString();

        Runnable timeoutRunnable = id != null ?
                TimeoutTask.newAnonymousTask(currentThread, timeout, latch, id, exceptionMessage) :
                TimeoutTask.newTaskWithException(currentThread, timeout, latch, exceptionType, exceptionMessage);

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) timeout.getInternalVariable("__executor__");

        try {
            return yieldWithTimeout(executor, context, seconds, block, timeoutRunnable, latch);
        } catch (RaiseException re) {
            // if it's the exception we're expecting
            if (re.getException().getMetaClass() == getTimeoutError(context, timeout)) {
                // and we were not given a specific exception
                if (id != null) {
                    raiseTimeoutErrorIfMatches(context, timeout, re, id);
                }
            }

            // otherwise, rethrow
            throw re;
        }
    }

    private static boolean nilOrZeroSeconds(final ThreadContext context, final IRubyObject seconds) {
        return seconds instanceof RubyNumeric secs ?
                secs.isZero(context) :
                seconds.isNil() || Helpers.invoke(context, seconds, "zero?").isTrue();
    }

    private static IRubyObject yieldWithTimeout(ScheduledThreadPoolExecutor executor, ThreadContext context,
        final IRubyObject seconds, final Block block,
        final Runnable runnable, final AtomicBoolean latch) throws RaiseException {

        final long micros = (long) ( RubyTime.convertTimeInterval(context, seconds) * 1000000 );
        ScheduledFuture timeoutFuture = null;
        try {
            timeoutFuture = executor.schedule(runnable, micros, TimeUnit.MICROSECONDS);
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

        final RubyModule timeout; // Timeout module
        final Object id; // needed for 'anonymous' timeout (no exception passed)
        final IRubyObject exception; // if there's exception (type) passed to timeout
        final RubyString message; // message for exception

        private TimeoutTask(final RubyThread currentThread, final RubyModule timeout,
            final AtomicBoolean latch, final Object id, final IRubyObject exception, final RubyString message) {
            this.currentThread = currentThread;
            this.timeout = timeout;
            this.latch = latch;
            this.id = id;
            this.exception = exception;
            this.message = message;
        }

        static TimeoutTask newAnonymousTask(final RubyThread currentThread, final RubyModule timeout,
                                            final AtomicBoolean latch, final Object id, final RubyString message) {
            return new TimeoutTask(currentThread, timeout, latch, id, null, message);
        }

        static TimeoutTask newTaskWithException(final RubyThread currentThread, final RubyModule timeout,
            final AtomicBoolean latch, final IRubyObject exception, final RubyString message) {
            return new TimeoutTask(currentThread, timeout, latch, null, exception, message);
        }

        public void run() {
            if ( latch.compareAndSet(false, true) ) {
                if ( exception == null ) {
                    raiseAnonymous(currentThread.getContext());
                } else {
                    raiseException();
                }
            }
        }

        private void raiseAnonymous(ThreadContext context) {
            // TODO: MRI calls Timeout::Error.catch here with the body of the timeout; we use __identifier__.
            var anonException = (RubyObject) getTimeoutError(context, timeout).newInstance(context, message, Block.NULL_BLOCK);
            anonException.setInternalVariable("__identifier__", id);
            currentThread.raise(anonException);
        }

        private void raiseException() {
            currentThread.raise(exception, message);
        }

    }

    private static void killTimeoutThread(ThreadContext context, ScheduledFuture timeoutFuture, AtomicBoolean latch) {
        if (latch.compareAndSet(false, true) && timeoutFuture.cancel(false)) {
            // ok, exception will not fire (also cancel caused task to be removed)
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

    private static IRubyObject raiseTimeoutErrorIfMatches(ThreadContext context,
        final RubyModule timeout, final RaiseException ex, final Object id) {

        // check if it's the exception intended for us
        if ( ex.getException().getInternalVariable("__identifier__") == id ) {
            final RubyException rubyException = ex.getException();

            return RubyKernel.raise( // throws
                    context,
                    kernelModule(context),
                    new IRubyObject[] {
                        getTimeoutError(context, timeout), // Timeout::Error
                        rubyException.callMethod(context, "message"),
                        rubyException.callMethod(context, "backtrace")
                    },
                    Block.NULL_BLOCK);
        }
        return null;
    }

    private static RubyClass getTimeoutError(ThreadContext context, final RubyModule timeout) {
        return timeout.getClass(context, "Error"); // Timeout::Error
    }

}
