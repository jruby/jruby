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
 * Copyright (C) 2002 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.util.Set;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.internal.runtime.FutureThread;
import org.jruby.internal.runtime.NativeThread;
import org.jruby.internal.runtime.RubyRunnable;
import org.jruby.internal.runtime.ThreadLike;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectMarshal;
import static org.jruby.runtime.Visibility.*;

import org.jruby.util.cli.Options;
import org.jruby.util.io.BlockingIO;
import org.jruby.util.io.SelectorFactory;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.CompatVersion.*;

/**
 * Implementation of Ruby's <code>Thread</code> class.  Each Ruby thread is
 * mapped to an underlying Java Virtual Machine thread.
 * <p>
 * Thread encapsulates the behavior of a thread of execution, including the main
 * thread of the Ruby script.  In the descriptions that follow, the parameter
 * <code>aSymbol</code> refers to a symbol, which is either a quoted string or a
 * <code>Symbol</code> (such as <code>:name</code>).
 * 
 * Note: For CVS history, see ThreadClass.java.
 */
@JRubyClass(name="Thread")
public class RubyThread extends RubyObject implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger("RubyThread");

    /** The thread-like think that is actually executing */
    private ThreadLike threadImpl;

    /** Normal thread-local variables */
    private transient Map<IRubyObject, IRubyObject> threadLocalVariables;

    /** Context-local variables, internal-ish thread locals */
    private final Map<Object, IRubyObject> contextVariables = new WeakHashMap<Object, IRubyObject>();

    /** Whether this thread should try to abort the program on exception */
    private boolean abortOnException;

    /** The final value resulting from the thread's execution */
    private IRubyObject finalResult;

    /**
     * The exception currently being raised out of the thread. We reference
     * it here to continue propagating it while handling thread shutdown
     * logic and abort_on_exception.
     */
    private RaiseException exitingException;

    /** The ThreadGroup to which this thread belongs */
    private RubyThreadGroup threadGroup;

    /** Per-thread "current exception" */
    private IRubyObject errorInfo;

    /** Weak reference to the ThreadContext for this thread. */
    private volatile WeakReference<ThreadContext> contextRef;

    private static final boolean DEBUG = false;

    /** Thread statuses */
    public static enum Status { RUN, SLEEP, ABORTING, DEAD }

    /** Current status in an atomic reference */
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.RUN);

    /** Mail slot for cross-thread events */
    private volatile ThreadService.Event mail;

    /** The current task blocking a thread, to allow interrupting it in an appropriate way */
    private volatile BlockingTask currentBlockingTask;

    /** The list of locks this thread currently holds, so they can be released on exit */
    private final List<Lock> heldLocks = new ArrayList<Lock>();

    /** Whether or not this thread has been disposed of */
    private volatile boolean disposed = false;

    /** The thread's initial priority, for use in thread pooled mode */
    private int initialPriority;

    protected RubyThread(Ruby runtime, RubyClass type) {
        super(runtime, type);

        finalResult = runtime.getNil();
        errorInfo = runtime.getNil();
    }

    public void receiveMail(ThreadService.Event event) {
        synchronized (this) {
            // if we're already aborting, we can receive no further mail
            if (status.get() == Status.ABORTING) return;

            mail = event;
            switch (event.type) {
            case KILL:
                status.set(Status.ABORTING);
            }

            // If this thread is sleeping or stopped, wake it
            notify();
        }

        // interrupt the target thread in case it's blocking or waiting
        // WARNING: We no longer interrupt the target thread, since this usually means
        // interrupting IO and with NIO that means the channel is no longer usable.
        // We either need a new way to handle waking a target thread that's waiting
        // on IO, or we need to accept that we can't wake such threads and must wait
        // for them to complete their operation.
        //threadImpl.interrupt();

        // new interrupt, to hopefully wake it out of any blocking IO
        this.interrupt();

    }

    public synchronized void checkMail(ThreadContext context) {
        ThreadService.Event myEvent = mail;
        mail = null;
        if (myEvent != null) {
            switch (myEvent.type) {
            case RAISE:
                receivedAnException(context, myEvent.exception);
            case KILL:
                throwThreadKill();
            }
        }
    }

    public IRubyObject getErrorInfo() {
        return errorInfo;
    }

    public IRubyObject setErrorInfo(IRubyObject errorInfo) {
        this.errorInfo = errorInfo;
        return errorInfo;
    }

    public void setContext(ThreadContext context) {
        this.contextRef = new WeakReference<ThreadContext>(context);
    }

    public ThreadContext getContext() {
        return contextRef.get();
    }


    public Thread getNativeThread() {
        return threadImpl.nativeThread();
    }

    /**
     * Perform pre-execution tasks once the native thread is running, but we
     * have not yet called the Ruby code for the thread.
     */
    public void beforeStart() {
        // store initial priority, for restoring pooled threads to normal
        initialPriority = threadImpl.getPriority();

        // set to "normal" priority
        threadImpl.setPriority(Thread.NORM_PRIORITY);
    }

    /**
     * Dispose of the current thread by tidying up connections to other stuff
     */
    public synchronized void dispose() {
        if (!disposed) {
            disposed = true;

            // remove from parent thread group
            threadGroup.remove(this);

            // unlock all locked locks
            unlockAll();

            // reset thread priority to initial if pooling
            if (Options.THREADPOOL_ENABLED.load()) {
                threadImpl.setPriority(initialPriority);
            }

            // mark thread as DEAD
            beDead();

            // unregister from runtime's ThreadService
            getRuntime().getThreadService().unregisterThread(this);
        }
    }
   
    public static RubyClass createThreadClass(Ruby runtime) {
        // FIXME: In order for Thread to play well with the standard 'new' behavior,
        // it must provide an allocator that can create empty object instances which
        // initialize then fills with appropriate data.
        RubyClass threadClass = runtime.defineClass("Thread", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setThread(threadClass);

        threadClass.index = ClassIndex.THREAD;
        threadClass.setReifiedClass(RubyThread.class);

        threadClass.defineAnnotatedMethods(RubyThread.class);

        RubyThread rubyThread = new RubyThread(runtime, threadClass);
        // TODO: need to isolate the "current" thread from class creation
        rubyThread.threadImpl = new NativeThread(rubyThread, Thread.currentThread());
        runtime.getThreadService().setMainThread(Thread.currentThread(), rubyThread);
        
        // set to default thread group
        runtime.getDefaultThreadGroup().addDirectly(rubyThread);
        
        threadClass.setMarshal(ObjectMarshal.NOT_MARSHALABLE_MARSHAL);
        
        return threadClass;
    }

    /**
     * <code>Thread.new</code>
     * <p>
     * Thread.new( <i>[ arg ]*</i> ) {| args | block } -> aThread
     * <p>
     * Creates a new thread to execute the instructions given in block, and
     * begins running it. Any arguments passed to Thread.new are passed into the
     * block.
     * <pre>
     * x = Thread.new { sleep .1; print "x"; print "y"; print "z" }
     * a = Thread.new { print "a"; print "b"; sleep .2; print "c" }
     * x.join # Let the threads finish before
     * a.join # main thread exits...
     * </pre>
     * <i>produces:</i> abxyzc
     */
    @JRubyMethod(name = {"new", "fork"}, rest = true, meta = true)
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return startThread(recv, args, true, block);
    }

    /**
     * Basically the same as Thread.new . However, if class Thread is
     * subclassed, then calling start in that subclass will not invoke the
     * subclass's initialize method.
     */
    @JRubyMethod(rest = true, meta = true, compat = RUBY1_8)
    public static RubyThread start(IRubyObject recv, IRubyObject[] args, Block block) {
        return startThread(recv, args, false, block);
    }
    
    @JRubyMethod(rest = true, name = "start", meta = true, compat = RUBY1_9)
    public static RubyThread start19(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        // The error message may appear incongruous here, due to the difference
        // between JRuby's Thread model and MRI's.
        // We mimic MRI's message in the name of compatibility.
        if (! block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without a block");
        return startThread(recv, args, false, block);
    }
    
    public static RubyThread adopt(IRubyObject recv, Thread t) {
        return adoptThread(recv, t, Block.NULL_BLOCK);
    }

    private static RubyThread adoptThread(final IRubyObject recv, Thread t, Block block) {
        final Ruby runtime = recv.getRuntime();
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) recv);
        
        rubyThread.threadImpl = new NativeThread(rubyThread, t);
        ThreadContext context = runtime.getThreadService().registerNewThread(rubyThread);
        runtime.getThreadService().associateThread(t, rubyThread);
        
        context.preAdoptThread();
        
        // set to default thread group
        runtime.getDefaultThreadGroup().addDirectly(rubyThread);
        
        return rubyThread;
    }
    
    @JRubyMethod(rest = true, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        Ruby runtime = getRuntime();
        if (!block.isGiven()) throw runtime.newThreadError("must be called with a block");

        try {
            RubyRunnable runnable = new RubyRunnable(this, args, context.getFrames(0), block);
            if (RubyInstanceConfig.POOLING_ENABLED) {
                FutureThread futureThread = new FutureThread(this, runnable);
                threadImpl = futureThread;

                addToCorrectThreadGroup(context);

                threadImpl.start();

                // JRUBY-2380, associate future early so it shows up in Thread.list right away, in case it doesn't run immediately
                runtime.getThreadService().associateThread(futureThread.getFuture(), this);
            } else {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("Ruby" + thread.getName() + ": " + context.getFile() + ":" + (context.getLine() + 1));
                threadImpl = new NativeThread(this, thread);

                addToCorrectThreadGroup(context);

                // JRUBY-2380, associate thread early so it shows up in Thread.list right away, in case it doesn't run immediately
                runtime.getThreadService().associateThread(thread, this);

                threadImpl.start();
            }

            // We yield here to hopefully permit the target thread to schedule
            // MRI immediately schedules it, so this is close but not exact
            Thread.yield();
        
            return this;
        } catch (OutOfMemoryError oome) {
            if (oome.getMessage().equals("unable to create new native thread")) {
                throw runtime.newThreadError(oome.getMessage());
            }
            throw oome;
        } catch (SecurityException ex) {
          throw runtime.newThreadError(ex.getMessage());
        }
    }
    
    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit, Block block) {
        RubyThread rubyThread = new RubyThread(recv.getRuntime(), (RubyClass) recv);
        
        if (callInit) {
            rubyThread.callInit(args, block);
        } else {
            // for Thread::start, which does not call the subclass's initialize
            rubyThread.initialize(recv.getRuntime().getCurrentContext(), args, block);
        }
        
        return rubyThread;
    }
    
    public synchronized void cleanTerminate(IRubyObject result) {
        finalResult = result;
    }

    public synchronized void beDead() {
        status.set(Status.DEAD);
    }

    public void pollThreadEvents() {
        pollThreadEvents(getRuntime().getCurrentContext());
    }
    
    public void pollThreadEvents(ThreadContext context) {
        if (mail != null) checkMail(context);
    }
    
    private static void throwThreadKill() {
        throw new ThreadKill();
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    @JRubyMethod(name = "abort_on_exception", meta = true)
    public static RubyBoolean abort_on_exception_x(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        return runtime.isGlobalAbortOnExceptionEnabled() ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(name = "abort_on_exception=", required = 1, meta = true)
    public static IRubyObject abort_on_exception_set_x(IRubyObject recv, IRubyObject value) {
        recv.getRuntime().setGlobalAbortOnExceptionEnabled(value.isTrue());
        return value;
    }

    @JRubyMethod(name = "current", meta = true)
    public static RubyThread current(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getThread();
    }

    @JRubyMethod(name = "main", meta = true)
    public static RubyThread main(IRubyObject recv) {
        return recv.getRuntime().getThreadService().getMainThread();
    }

    @JRubyMethod(name = "pass", meta = true)
    public static IRubyObject pass(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        ThreadService ts = runtime.getThreadService();
        boolean critical = ts.getCritical();
        
        ts.setCritical(false);
        
        Thread.yield();
        
        ts.setCritical(critical);
        
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "list", meta = true)
    public static RubyArray list(IRubyObject recv) {
        RubyThread[] activeThreads = recv.getRuntime().getThreadService().getActiveRubyThreads();
        
        return recv.getRuntime().newArrayNoCopy(activeThreads);
    }

    private void addToCorrectThreadGroup(ThreadContext context) {
        // JRUBY-3568, inherit threadgroup or use default
        IRubyObject group = context.getThread().group();
        if (!group.isNil()) {
            ((RubyThreadGroup) group).addDirectly(this);
        } else {
            context.getRuntime().getDefaultThreadGroup().addDirectly(this);
        }
    }
    
    private IRubyObject getSymbolKey(IRubyObject originalKey) {
        if (originalKey instanceof RubySymbol) {
            return originalKey;
        } else if (originalKey instanceof RubyString) {
            return getRuntime().newSymbol(originalKey.asJavaString());
        } else if (originalKey instanceof RubyFixnum) {
            getRuntime().getWarnings().warn(ID.FIXNUMS_NOT_SYMBOLS, "Do not use Fixnums as Symbols");
            throw getRuntime().newArgumentError(originalKey + " is not a symbol");
        } else {
            throw getRuntime().newTypeError(originalKey + " is not a symbol");
        }
    }
    
    private synchronized Map<IRubyObject, IRubyObject> getThreadLocals() {
        if (threadLocalVariables == null) {
            threadLocalVariables = new HashMap<IRubyObject, IRubyObject>();
        }
        return threadLocalVariables;
    }

    private void clearThreadLocals() {
        threadLocalVariables = null;
    }

    public final Map<Object, IRubyObject> getContextVariables() {
        return contextVariables;
    }

    public boolean isAlive(){
        return threadImpl.isAlive() && status.get() != Status.ABORTING;
    }

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject op_aref(IRubyObject key) {
        IRubyObject value;
        if ((value = getThreadLocals().get(getSymbolKey(key))) != null) {
            return value;
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "[]=", required = 2)
    public IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        key = getSymbolKey(key);
        
        getThreadLocals().put(key, value);
        return value;
    }

    @JRubyMethod(name = "abort_on_exception")
    public RubyBoolean abort_on_exception() {
        return abortOnException ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "abort_on_exception=", required = 1)
    public IRubyObject abort_on_exception_set(IRubyObject val) {
        abortOnException = val.isTrue();
        return val;
    }

    @JRubyMethod(name = "alive?")
    public RubyBoolean alive_p() {
        return isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "join", optional = 1)
    public IRubyObject join(IRubyObject[] args) {
        Ruby runtime = getRuntime();
        long timeoutMillis = Long.MAX_VALUE;

        if (args.length > 0) {
            if (args.length > 1) {
                throw getRuntime().newArgumentError(args.length,1);
            }
            // MRI behavior: value given in seconds; converted to Float; less
            // than or equal to zero returns immediately; returns nil
            timeoutMillis = (long)(1000.0D * args[0].convertToFloat().getValue());
            if (timeoutMillis <= 0) {
            // TODO: not sure that we should skip calling join() altogether.
            // Thread.join() has some implications for Java Memory Model, etc.
                if (threadImpl.isAlive()) {
                    return getRuntime().getNil();
                } else {   
                   return this;
                }
            }
        }

        if (isCurrent()) {
            throw getRuntime().newThreadError("thread " + identityString() + " tried to join itself");
        }

        try {
            if (runtime.getThreadService().getCritical()) {
                // If the target thread is sleeping or stopped, wake it
                synchronized (this) {
                    notify();
                }
                
                // interrupt the target thread in case it's blocking or waiting
                // WARNING: We no longer interrupt the target thread, since this usually means
                // interrupting IO and with NIO that means the channel is no longer usable.
                // We either need a new way to handle waking a target thread that's waiting
                // on IO, or we need to accept that we can't wake such threads and must wait
                // for them to complete their operation.
                //threadImpl.interrupt();
            }

            RubyThread currentThread = getRuntime().getCurrentContext().getThread();
            final long timeToWait = Math.min(timeoutMillis, 200);

            // We need this loop in order to be able to "unblock" the
            // join call without actually calling interrupt.
            long start = System.currentTimeMillis();
            while(true) {
                currentThread.pollThreadEvents();
                threadImpl.join(timeToWait);
                if (!threadImpl.isAlive()) {
                    break;
                }
                if (System.currentTimeMillis() - start > timeoutMillis) {
                    break;
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            assert false : ie;
        } catch (ExecutionException ie) {
            ie.printStackTrace();
            assert false : ie;
        }

        if (exitingException != null) {
            // Set $! in the current thread before exiting
            getRuntime().getGlobalVariables().set("$!", (IRubyObject)exitingException.getException());
            throw exitingException;
        }

        if (threadImpl.isAlive()) {
            return getRuntime().getNil();
        } else {
            return this;
        }
    }

    @JRubyMethod
    public IRubyObject value() {
        join(new IRubyObject[0]);
        synchronized (this) {
            return finalResult;
        }
    }

    @JRubyMethod
    public IRubyObject group() {
        if (threadGroup == null) {
            return getRuntime().getNil();
        }
        
        return threadGroup;
    }
    
    void setThreadGroup(RubyThreadGroup rubyThreadGroup) {
        threadGroup = rubyThreadGroup;
    }
    
    @JRubyMethod(name = "inspect")
    @Override
    public synchronized IRubyObject inspect() {
        // FIXME: There's some code duplication here with RubyObject#inspect
        StringBuilder part = new StringBuilder();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(":");
        part.append(identityString());
        part.append(' ');
        part.append(status.toString().toLowerCase());
        part.append('>');
        return getRuntime().newString(part.toString());
    }

    @JRubyMethod(name = "key?", required = 1)
    public RubyBoolean key_p(IRubyObject key) {
        key = getSymbolKey(key);
        
        return getRuntime().newBoolean(getThreadLocals().containsKey(key));
    }

    @JRubyMethod(name = "keys")
    public RubyArray keys() {
        IRubyObject[] keys = new IRubyObject[getThreadLocals().size()];
        
        return RubyArray.newArrayNoCopy(getRuntime(), getThreadLocals().keySet().toArray(keys));
    }
    
    @JRubyMethod(name = "critical=", required = 1, meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject critical_set(IRubyObject receiver, IRubyObject value) {
        receiver.getRuntime().getThreadService().setCritical(value.isTrue());

        return value;
    }

    @JRubyMethod(name = "critical", meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject critical(IRubyObject receiver) {
        return receiver.getRuntime().newBoolean(receiver.getRuntime().getThreadService().getCritical());
    }
    
    @JRubyMethod(name = "stop", meta = true)
    public static IRubyObject stop(ThreadContext context, IRubyObject receiver) {
        RubyThread rubyThread = context.getThread();
        
        synchronized (rubyThread) {
            rubyThread.checkMail(context);
            try {
                // attempt to decriticalize all if we're the critical thread
                receiver.getRuntime().getThreadService().setCritical(false);

                rubyThread.status.set(Status.SLEEP);
                rubyThread.wait();
            } catch (InterruptedException ie) {
                rubyThread.checkMail(context);
                rubyThread.status.set(Status.RUN);
            }
        }
        
        return receiver.getRuntime().getNil();
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject kill(IRubyObject receiver, IRubyObject rubyThread, Block block) {
        if (!(rubyThread instanceof RubyThread)) throw receiver.getRuntime().newTypeError(rubyThread, receiver.getRuntime().getThread());
        return ((RubyThread)rubyThread).kill();
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject exit(IRubyObject receiver, Block block) {
        RubyThread rubyThread = receiver.getRuntime().getThreadService().getCurrentContext().getThread();

        synchronized (rubyThread) {
            rubyThread.status.set(Status.ABORTING);
            rubyThread.mail = null;
            receiver.getRuntime().getThreadService().setCritical(false);
            throw new ThreadKill();
        }
    }

    @JRubyMethod(name = "stop?")
    public RubyBoolean stop_p() {
        // not valid for "dead" state
        return getRuntime().newBoolean(status.get() == Status.SLEEP || status.get() == Status.DEAD);
    }
    
    @JRubyMethod(name = "wakeup")
    public synchronized RubyThread wakeup() {
        if(!threadImpl.isAlive() && status.get() == Status.DEAD) {
            throw getRuntime().newThreadError("killed thread");
        }

        status.set(Status.RUN);
        notifyAll();

        return this;
    }
    
    @JRubyMethod(name = "priority")
    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuntime(), threadImpl.getPriority());
    }

    @JRubyMethod(name = "priority=", required = 1)
    public IRubyObject priority_set(IRubyObject priority) {
        // FIXME: This should probably do some translation from Ruby priority levels to Java priority levels (until we have green threads)
        int iPriority = RubyNumeric.fix2int(priority);
        
        if (iPriority < Thread.MIN_PRIORITY) {
            iPriority = Thread.MIN_PRIORITY;
        } else if (iPriority > Thread.MAX_PRIORITY) {
            iPriority = Thread.MAX_PRIORITY;
        }
        
        if (threadImpl.isAlive()) {
            threadImpl.setPriority(iPriority);
        }

        return RubyFixnum.newFixnum(getRuntime(), iPriority);
    }

    @JRubyMethod(optional = 3)
    public IRubyObject raise(IRubyObject[] args, Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        if (this == context.getThread()) {
            return RubyKernel.raise(context, runtime.getKernel(), args, block);
        }
        
        debug(this, "before raising");
        RubyThread currentThread = getRuntime().getCurrentContext().getThread();

        debug(this, "raising");
        IRubyObject exception = prepareRaiseException(runtime, args, block);

        runtime.getThreadService().deliverEvent(new ThreadService.Event(currentThread, this, ThreadService.Event.Type.RAISE, exception));

        return this;
    }

    /**
     * This is intended to be used to raise exceptions in Ruby threads from non-
     * Ruby threads like Timeout's thread.
     * 
     * @param args Same args as for Thread#raise
     * @param block Same as for Thread#raise
     */
    public void internalRaise(IRubyObject[] args) {
        Ruby runtime = getRuntime();

        IRubyObject exception = prepareRaiseException(runtime, args, Block.NULL_BLOCK);

        receiveMail(new ThreadService.Event(this, this, ThreadService.Event.Type.RAISE, exception));
    }

    private IRubyObject prepareRaiseException(Ruby runtime, IRubyObject[] args, Block block) {
        if(args.length == 0) {
            IRubyObject lastException = errorInfo;
            if(lastException.isNil()) {
                return new RaiseException(runtime, runtime.getRuntimeError(), "", false).getException();
            } 
            return lastException;
        }

        IRubyObject exception;
        ThreadContext context = getRuntime().getCurrentContext();
        
        if(args.length == 1) {
            if(args[0] instanceof RubyString) {
                return runtime.getRuntimeError().newInstance(context, args, block);
            }
            
            if(!args[0].respondsTo("exception")) {
                return runtime.newTypeError("exception class/object expected").getException();
            }
            exception = args[0].callMethod(context, "exception");
        } else {
            if (!args[0].respondsTo("exception")) {
                return runtime.newTypeError("exception class/object expected").getException();
            }
            
            exception = args[0].callMethod(context, "exception", args[1]);
        }
        
        if (!runtime.getException().isInstance(exception)) {
            return runtime.newTypeError("exception object expected").getException();
        }
        
        if (args.length == 3) {
            ((RubyException) exception).set_backtrace(args[2]);
        }
        
        return exception;
    }
    
    @JRubyMethod(name = "run")
    public synchronized IRubyObject run() {
        return wakeup();
    }

    /**
     * We can never be sure if a wait will finish because of a Java "spurious wakeup".  So if we
     * explicitly wakeup and we wait less than requested amount we will return false.  We will
     * return true if we sleep right amount or less than right amount via spurious wakeup.
     */
    public synchronized boolean sleep(long millis) throws InterruptedException {
        assert this == getRuntime().getCurrentContext().getThread();
        boolean result = true;

        synchronized (this) {
            pollThreadEvents();
            try {
                status.set(Status.SLEEP);
                if (millis == -1) {
                    wait();
                } else {
                    wait(millis);
                }
            } finally {
                result = (status.get() != Status.RUN);
                pollThreadEvents();
                status.set(Status.RUN);
            }
        }

        return result;
    }

    @JRubyMethod(name = "status")
    public synchronized IRubyObject status() {
        if (threadImpl.isAlive()) {
            // TODO: no java stringity
            return getRuntime().newString(status.toString().toLowerCase());
        } else if (exitingException != null) {
            return getRuntime().getNil();
        } else {
            return getRuntime().getFalse();
        }
    }

    public static interface BlockingTask {
        public void run() throws InterruptedException;
        public void wakeup();
    }

    public static final class SleepTask implements BlockingTask {
        private final Object object;
        private final long millis;
        private final int nanos;

        public SleepTask(Object object, long millis, int nanos) {
            this.object = object;
            this.millis = millis;
            this.nanos = nanos;
        }

        public void run() throws InterruptedException {
            synchronized (object) {
                object.wait(millis, nanos);
            }
        }

        public void wakeup() {
            synchronized (object) {
                object.notify();
            }
        }
    }

    public void executeBlockingTask(BlockingTask task) throws InterruptedException {
        enterSleep();
        try {
            currentBlockingTask = task;
            pollThreadEvents();
            task.run();
        } finally {
            exitSleep();
            currentBlockingTask = null;
            pollThreadEvents();
        }
    }

    public void enterSleep() {
        status.set(Status.SLEEP);
    }

    public void exitSleep() {
        status.set(Status.RUN);
    }

    @JRubyMethod(name = {"kill", "exit", "terminate"})
    public IRubyObject kill() {
        // need to reexamine this
        RubyThread currentThread = getRuntime().getCurrentContext().getThread();
        
        // If the killee thread is the same as the killer thread, just die
        if (currentThread == this) throwThreadKill();

        debug(this, "trying to kill");

        currentThread.pollThreadEvents();

        getRuntime().getThreadService().deliverEvent(new ThreadService.Event(currentThread, this, ThreadService.Event.Type.KILL));

        debug(this, "succeeded with kill");
        
        return this;
    }

    private static void debug(RubyThread thread, String message) {
        if (DEBUG) LOG.debug(Thread.currentThread() + "(" + thread.status + "): " + message);
    }
    
    @JRubyMethod(name = {"kill!", "exit!", "terminate!"}, compat = RUBY1_8)
    public IRubyObject kill_bang() {
        throw getRuntime().newNotImplementedError("Thread#kill!, exit!, and terminate! are not safe and not supported");
    }
    
    @JRubyMethod(name = "safe_level")
    public IRubyObject safe_level() {
        throw getRuntime().newNotImplementedError("Thread-specific SAFE levels are not supported");
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject backtrace(ThreadContext context) {
        return getContext().createCallerBacktrace(context.getRuntime(), 0);
    }

    public StackTraceElement[] javaBacktrace() {
        if (threadImpl instanceof NativeThread) {
            return ((NativeThread)threadImpl).getThread().getStackTrace();
        }

        // Future-based threads can't get a Java trace
        return new StackTraceElement[0];
    }

    private boolean isCurrent() {
        return threadImpl.isCurrent();
    }

    public void exceptionRaised(RaiseException exception) {
        assert isCurrent();

        RubyException rubyException = exception.getException();
        Ruby runtime = rubyException.getRuntime();
        if (runtime.getSystemExit().isInstance(rubyException)) {
            runtime.getThreadService().getMainThread().raise(new IRubyObject[] {rubyException}, Block.NULL_BLOCK);
        } else if (abortOnException(runtime)) {
            runtime.printError(rubyException);
            RubyException systemExit = RubySystemExit.newInstance(runtime, 1);
            systemExit.message = rubyException.message;
            systemExit.set_backtrace(rubyException.backtrace());
            runtime.getThreadService().getMainThread().raise(new IRubyObject[] {systemExit}, Block.NULL_BLOCK);
            return;
        } else if (runtime.getDebug().isTrue()) {
            runtime.printError(exception.getException());
        }
        exitingException = exception;
    }

    private boolean abortOnException(Ruby runtime) {
        return (runtime.isGlobalAbortOnExceptionEnabled() || abortOnException);
    }

    public static RubyThread mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }
    
    private volatile Selector currentSelector;
    
    @Deprecated
    public boolean selectForAccept(RubyIO io) {
        return select(io, SelectionKey.OP_ACCEPT);
    }

    private synchronized Selector getSelector(SelectableChannel channel) throws IOException {
        return SelectorFactory.openWithRetryFrom(getRuntime(), channel.provider());
    }
    
    public boolean select(RubyIO io, int ops) {
        return select(io.getChannel(), io, ops);
    }
    
    public boolean select(RubyIO io, int ops, long timeout) {
        return select(io.getChannel(), io, ops, timeout);
    }

    public boolean select(Channel channel, RubyIO io, int ops) {
        return select(channel, io, ops, -1);
    }

    public boolean select(Channel channel, RubyIO io, int ops, long timeout) {
        if (channel instanceof SelectableChannel) {
            SelectableChannel selectable = (SelectableChannel)channel;
            
            synchronized (selectable.blockingLock()) {
                boolean oldBlocking = selectable.isBlocking();

                SelectionKey key = null;
                try {
                    selectable.configureBlocking(false);
                    
                    if (io != null) io.addBlockingThread(this);
                    currentSelector = getRuntime().getSelectorPool().get(selectable.provider());

                    key = selectable.register(currentSelector, ops);

                    beforeBlockingCall();
                    int result;
                    if (timeout < 0) {
                        result = currentSelector.select();
                    } else if (timeout == 0) {
                        result = currentSelector.selectNow();
                    } else {
                        result = currentSelector.select(timeout);
                    }

                    // check for thread events, in case we've been woken up to die
                    pollThreadEvents();

                    if (result == 1) {
                        Set<SelectionKey> keySet = currentSelector.selectedKeys();

                        if (keySet.iterator().next() == key) {
                            return true;
                        }
                    }

                    return false;
                } catch (IOException ioe) {
                    throw getRuntime().newRuntimeError("Error with selector: " + ioe);
                } finally {
                    // Note: I don't like ignoring these exceptions, but it's
                    // unclear how likely they are to happen or what damage we
                    // might do by ignoring them. Note that the pieces are separate
                    // so that we can ensure one failing does not affect the others
                    // running.

                    // clean up the key in the selector
                    try {
                        if (key != null) key.cancel();
                        if (currentSelector != null) currentSelector.selectNow();
                    } catch (Exception e) {
                        // ignore
                    }

                    // shut down and null out the selector
                    try {
                        if (currentSelector != null) {
                            getRuntime().getSelectorPool().put(currentSelector);
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        currentSelector = null;
                    }

                    // remove this thread as a blocker against the given IO
                    if (io != null) io.removeBlockingThread(this);

                    // go back to previous blocking state on the selectable
                    try {
                        selectable.configureBlocking(oldBlocking);
                    } catch (Exception e) {
                        // ignore
                    }

                    // clear thread state from blocking call
                    afterBlockingCall();
                }
            }
        } else {
            // can't select, just have to do a blocking call
            return true;
        }
    }
    
    public void interrupt() {
        Selector activeSelector = currentSelector;
        if (activeSelector != null) {
            activeSelector.wakeup();
        }
        BlockingIO.Condition iowait = blockingIO;
        if (iowait != null) {
            iowait.cancel();
        }
        
        BlockingTask task = currentBlockingTask;
        if (task != null) {
            task.wakeup();
        }
    }
    private volatile BlockingIO.Condition blockingIO = null;
    public boolean waitForIO(ThreadContext context, RubyIO io, int ops) {
        Channel channel = io.getChannel();

        if (!(channel instanceof SelectableChannel)) {
            return true;
        }
        try {
            io.addBlockingThread(this);
            blockingIO = BlockingIO.newCondition(channel, ops);
            boolean ready = blockingIO.await();
            
            // check for thread events, in case we've been woken up to die
            pollThreadEvents();
            return ready;
        } catch (IOException ioe) {
            throw context.getRuntime().newRuntimeError("Error with selector: " + ioe);
        } catch (InterruptedException ex) {
            // FIXME: not correct exception
            throw context.getRuntime().newRuntimeError("Interrupted");
        } finally {
            blockingIO = null;
            io.removeBlockingThread(this);
        }
    }
    public void beforeBlockingCall() {
        pollThreadEvents();
        enterSleep();
    }
    
    public void afterBlockingCall() {
        exitSleep();
        pollThreadEvents();
    }

    private void receivedAnException(ThreadContext context, IRubyObject exception) {
        RubyModule kernelModule = getRuntime().getKernel();
        debug(this, "before propagating exception");
        kernelModule.callMethod(context, "raise", exception);
    }

    public boolean wait_timeout(IRubyObject o, Double timeout) throws InterruptedException {
        if ( timeout != null ) {
            long delay_ns = (long)(timeout.doubleValue() * 1000000000.0);
            long start_ns = System.nanoTime();
            if (delay_ns > 0) {
                long delay_ms = delay_ns / 1000000;
                int delay_ns_remainder = (int)( delay_ns % 1000000 );
                executeBlockingTask(new SleepTask(o, delay_ms, delay_ns_remainder));
            }
            long end_ns = System.nanoTime();
            return ( end_ns - start_ns ) <= delay_ns;
        } else {
            executeBlockingTask(new SleepTask(o, 0, 0));
            return true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RubyThread other = (RubyThread)obj;
        if (this.threadImpl != other.threadImpl && (this.threadImpl == null || !this.threadImpl.equals(other.threadImpl))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.threadImpl != null ? this.threadImpl.hashCode() : 0);
        return hash;
    }

    public String toString() {
        return threadImpl.toString();
    }
    
    /**
     * Acquire the given lock, holding a reference to it for cleanup on thread
     * termination.
     * 
     * @param lock the lock to acquire, released on thread termination
     */
    public void lock(Lock lock) {
        assert Thread.currentThread() == getNativeThread();
        lock.lock();
        heldLocks.add(lock);
    }
    
    /**
     * Acquire the given lock interruptibly, holding a reference to it for cleanup
     * on thread termination.
     * 
     * @param lock the lock to acquire, released on thread termination
     * @throws InterruptedException if the lock acquisition is interrupted
     */
    public void lockInterruptibly(Lock lock) throws InterruptedException {
        assert Thread.currentThread() == getNativeThread();
        lock.lockInterruptibly();
        heldLocks.add(lock);
    }
    
    /**
     * Try to acquire the given lock, adding it to a list of held locks for cleanup
     * on thread termination if it is acquired. Return immediately if the lock
     * cannot be acquired.
     * 
     * @param lock the lock to acquire, released on thread termination
     */
    public boolean tryLock(Lock lock) {
        assert Thread.currentThread() == getNativeThread();
        boolean locked = lock.tryLock();
        if (locked) {
            heldLocks.add(lock);
        }
        return locked;
    }
    
    /**
     * Release the given lock and remove it from the list of locks to be released
     * on thread termination.
     * 
     * @param lock the lock to release and dereferences
     */
    public void unlock(Lock lock) {
        assert Thread.currentThread() == getNativeThread();
        lock.unlock();
        heldLocks.remove(lock);
    }
    
    /**
     * Release all locks held.
     */
    public void unlockAll() {
        assert Thread.currentThread() == getNativeThread();
        for (Lock lock : heldLocks) {
            lock.unlock();
        }
    }

    private String identityString() {
        return "0x" + Integer.toHexString(System.identityHashCode(this));
    }
}
