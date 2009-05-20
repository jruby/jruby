/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.internal.runtime;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jruby.RubyThread;

/**
 * @author cnutter
 */
public class FutureThread implements ThreadLike {
    private volatile Future future;
    private RubyRunnable runnable;
    public RubyThread rubyThread;
    
    public FutureThread(RubyThread rubyThread, RubyRunnable runnable) {
        this.rubyThread = rubyThread;
        this.runnable = runnable;
    }

    public Future getFuture() {
        return future;
    }
    
    /**
     * Starting a new thread in terms of a thread pool is just submitting it as
     * a job to the pool.
     */
    public void start() {
        future = rubyThread.getRuntime().getExecutor().submit(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    rubyThread.getRuntime().getThreadService().dissociateThread(future);
                }
            }
        });
    }
    
    /**
     * In order to do a thread interrupt, we need to get the actual thread, stored
     * in the RubyRunnable instance and tell it to interrupt. Future does not
     * provide a mechanism for passing an interrupt to the thread running it.
     * 
     * If the runnable is not being executed by a thread (not yet, or already
     * done) do nothing.
     */
    public void interrupt() {
        if (runnable.getJavaThread() != null) {
            runnable.getJavaThread().interrupt();
        }
    }
    
    /**
     * If the future has not yet run and or is running and not yet complete.
     * 
     * We defined "alive" for a FutureThread to be the following:
     * 
     * <li>The Runnable has not yet been submitted to the Executor. This is equivalent
     * to a Ruby thread that has not yet been scheduled to run. Because Ruby
     * considered a thread alive immediately, a FutureThread with no Future yet
     * is still considered alive, because it will eventually be submitted.</li>
     * 
     * <li>The Runnable has been submitted to the Executor but no Future has
     * been returned yet. There's a bit of a race here if we expect future to
     * not be null, since the runnable could be submitted to the Executor and
     * complete execution before the future is ever assigned. This manifested
     * as Thread.current.status from within a thread coming up as false...
     * because isAlive returned false if the future was still null.</li>
     * 
     * <li>The Runnable has been submitted to the Executor, the Future has
     * been assigned, but the Future is not "done" yet. The thread is running,
     * and we report it is alive.</li>
     * 
     * All other cases are considered "not alive" and we return fals here.
     */
    public boolean isAlive() {
        return future == null || !future.isDone();
    }
    
    public void join() throws InterruptedException, ExecutionException {
        try {
            future.get();
        } catch (CancellationException ce) {
            // ignore; job was cancelled
            // FIXME: is this ok?
        }
    }
    
    /**
     * We check for zero millis here because Future appears to wait for zero if
     * you pass it zero, where Thread behavior is to wait forever.
     * 
     * We also catch and swallow CancellationException because it means the Future
     * was cancelled before it ran, and is therefore as done as it will ever be.
     * 
     * @param millis The number of millis to wait; 0 waits forever.
     * 
     * @throws java.lang.InterruptedException If the blocking join is interrupted
     * by another thread.
     * @throws java.util.concurrent.ExecutionException If an execution error is
     * raised by the underlying Future.
     */
    public void join(long millis) throws InterruptedException, ExecutionException {
        if (millis == 0) {
            join();
        } else {
            try {
                future.get(millis, TimeUnit.MILLISECONDS);
            } catch (CancellationException ce) {
                // ignore; job was cancelled
                // FIXME: Is this ok?
            } catch (TimeoutException te) {
                // do nothing, just exit
            }
        }
    }
    
    /**
     * Jobs from the thread pool do not support setting priorities.
     * 
     * @return
     */
    public int getPriority() {
        return 1;
    }
    
    public void setPriority(int priority) {
        //nativeThread.setPriority(priority);
    }
    
    public boolean isCurrent() {
        return rubyThread == rubyThread.getRuntime().getCurrentContext().getThread();
    }
    
    public boolean isInterrupted() {
        return future.isCancelled();
    }
}
