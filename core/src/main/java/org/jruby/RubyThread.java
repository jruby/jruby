/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.internal.runtime.NativeThread;
import org.jruby.internal.runtime.RubyRunnable;
import org.jruby.internal.runtime.ThreadLike;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.BlockingIO;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.runtime.Visibility.*;
import static org.jruby.runtime.backtrace.BacktraceData.EMPTY_STACK_TRACE;

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

    private static final Logger LOG = LoggerFactory.getLogger(RubyThread.class);
    // static { LOG.setDebugEnable(true); }

    /** The thread-like think that is actually executing */
    private volatile ThreadLike threadImpl = ThreadLike.DUMMY;

    /** Fiber-local variables */
    private volatile transient Map<IRubyObject, IRubyObject> fiberLocalVariables;

    /** Normal thread-local variables (local to parent thread if in a fiber) */
    private volatile transient Map<IRubyObject, IRubyObject> threadLocalVariables;

    /** Context-local variables, internal-ish thread locals */
    private final Map<Object, IRubyObject> contextVariables = new WeakHashMap<Object, IRubyObject>();

    /** Whether this thread should try to abort the program on exception */
    private volatile boolean abortOnException;

    /** Whether this thread should report_on_exception when this thread GCs, when it terminates, or never */
    private volatile IRubyObject reportOnException;

    /** Whether this thread's terminating exception has been captured by any code *after* the thread terminated. */
    private volatile boolean exceptionCaptured;

    /** The final value resulting from the thread's execution */
    private volatile IRubyObject finalResult;

    private String file; private int line; // Thread.new location (for inspect)

    /**
     * The exception currently being raised out of the thread. We reference
     * it here to continue propagating it while handling thread shutdown
     * logic and abort_on_exception.
     */
    private volatile RaiseException exitingException;

    /** The ThreadGroup to which this thread belongs */
    private volatile RubyThreadGroup threadGroup;

    /** Per-thread "current exception" */
    private volatile IRubyObject errorInfo;

    /** Weak reference to the ThreadContext for this thread. */
    private volatile WeakReference<ThreadContext> contextRef;

    /** Whether to scan for cross-thread events */
    //private volatile boolean handleInterrupt = true;

    /** Stack of interrupt masks active for this thread */
    private final List<RubyHash> interruptMaskStack = Collections.synchronizedList(new ArrayList<RubyHash>());

    /** Thread-local tuple used for sleeping (semaphore, millis, nanos) */
    private final SleepTask2 sleepTask = new SleepTask2();

    public static final int RUBY_MIN_THREAD_PRIORITY = -3;
    public static final int RUBY_MAX_THREAD_PRIORITY = 3;

    /** Thread statuses */
    public static enum Status {
        RUN, SLEEP, ABORTING, DEAD;

        public final ByteList bytes;

        Status() {
            bytes = new ByteList(toString().toLowerCase().getBytes(RubyEncoding.UTF8));
        }
    }

    /** Current status in an atomic reference */
    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.RUN);

    /** Mail slot for cross-thread events */
    private final Queue<IRubyObject> pendingInterruptQueue = new ConcurrentLinkedQueue<>();

    /** A function to use to unblock this thread, if possible */
    private volatile Unblocker unblockFunc;

    /** Argument to pass to the unblocker */
    private volatile Object unblockArg;

    /** The list of locks this thread currently holds, so they can be released on exit */
    private final List<Lock> heldLocks = new Vector<Lock>();

    /** Whether or not this thread has been disposed of */
    private volatile boolean disposed = false;

    /** Interrupt flags */
    private volatile int interruptFlag = 0;

    /** Interrupt mask to use for disabling certain types */
    private volatile int interruptMask;

    /** Short circuit to avoid-re-scanning for interrupts */
    private volatile boolean pendingInterruptQueueChecked = false;

    private volatile Selector currentSelector;

    private volatile RubyThread fiberCurrentThread;

    private static final AtomicIntegerFieldUpdater<RubyThread> INTERRUPT_FLAG_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(RubyThread.class, "interruptFlag");

    private static final int TIMER_INTERRUPT_MASK         = 0x01;
    private static final int PENDING_INTERRUPT_MASK       = 0x02;
    private static final int POSTPONED_JOB_INTERRUPT_MASK = 0x04;
    private static final int TRAP_INTERRUPT_MASK	      = 0x08;

    private static final int INTERRUPT_NONE = 0;
    private static final int INTERRUPT_IMMEDIATE = 1;
    private static final int INTERRUPT_ON_BLOCKING = 2;
    private static final int INTERRUPT_NEVER = 3;

    protected RubyThread(Ruby runtime, RubyClass type) {
        super(runtime, type);

        finalResult = errorInfo = runtime.getNil();
        reportOnException = runtime.getReportOnException();
    }

    public RubyThread(Ruby runtime, RubyClass klass, Runnable runnable) {
        this(runtime, klass);

        startThread(runtime.getCurrentContext(), runnable);
    }

    private void executeInterrupts(ThreadContext context, boolean blockingTiming) {
        Ruby runtime = context.runtime;
        int interrupt;

        boolean postponedJobInterrupt = false;

        while ((interrupt = getInterrupts()) != 0) {
            boolean timerInterrupt = (interrupt & TIMER_INTERRUPT_MASK) == TIMER_INTERRUPT_MASK;
            boolean pendingInterrupt = (interrupt & PENDING_INTERRUPT_MASK) == PENDING_INTERRUPT_MASK;

//            if (postponedJobInterrupt) {
//                postponedJobFlush(context);
//            }
            // Missing: signal handling...but perhaps we don't need it on JVM

            if (pendingInterrupt && pendingInterruptActive()) {
                IRubyObject err = pendingInterruptDeque(context, blockingTiming ? INTERRUPT_ON_BLOCKING : INTERRUPT_NONE);

                if (err == UNDEF) {
                    // no error
                } else if (err == RubyFixnum.zero(runtime) ||
                        err == RubyFixnum.one(runtime) ||
                        err == RubyFixnum.two(runtime)) {
                    toKill();
                } else {
                    afterBlockingCall();
                    if (status.get() == Status.SLEEP) {
                        exitSleep();
                    }
                    // if it's a Ruby exception, force the cause through
                    IRubyObject[] args;
                    if (err instanceof RubyException) {
                        args = Helpers.arrayOf(err, RubyHash.newKwargs(runtime, "cause", ((RubyException) err).cause));
                    } else {
                        args = Helpers.arrayOf(err);
                    }
                    RubyKernel.raise(context, runtime.getKernel(), args, Block.NULL_BLOCK);
                }
            }

            // Missing: timer interrupt...needed?
        }
    }

    private void postponedJobFlush(ThreadContext context) {
        // unsure if this function has any relevance in JRuby

//        int savedPostponedJobInterruptMask = interruptMask & POSTPONED_JOB_INTERRUPT_MASK;
//
//        errorInfo = context.nil;
//        interruptMask |= POSTPONED_JOB_INTERRUPT_MASK;
    }

    private boolean pendingInterruptActive() {
        if (pendingInterruptQueueChecked) return false;
        if (pendingInterruptQueue.isEmpty()) return false;
        return true;
    }

    private void toKill() {
        pendingInterruptClear();
        throwThreadKill();
    }

    private void pendingInterruptClear() {
        pendingInterruptQueue.clear();
    }

    private int getInterrupts() {
        int interrupt;
        while (true) {
            interrupt = interruptFlag;
            if (INTERRUPT_FLAG_UPDATER.compareAndSet(this, interrupt, interrupt & interruptMask)) {
                break;
            }
        }
        return interrupt & ~interruptMask;
    }

    private IRubyObject pendingInterruptDeque(ThreadContext context, int timing) {
        for (Iterator<IRubyObject> iterator = pendingInterruptQueue.iterator(); iterator.hasNext();) {
            IRubyObject err = iterator.next();
            int maskTiming = pendingInterruptCheckMask(context, err);

            switch (maskTiming) {
                case INTERRUPT_ON_BLOCKING:
                    if (timing != INTERRUPT_ON_BLOCKING) break;
                case INTERRUPT_NONE:
                case INTERRUPT_IMMEDIATE:
                    iterator.remove();
                    return err;
                case INTERRUPT_NEVER:
                    break;
            }
        }

        pendingInterruptQueueChecked = true;

        return UNDEF;
    }

    private int pendingInterruptCheckMask(ThreadContext context, IRubyObject err) {
        List<IRubyObject> ancestors = err.getMetaClass().getAncestorList();
        int ancestorsLen = ancestors.size();

        List<RubyHash> maskStack = interruptMaskStack;
        int maskStackLen = maskStack.size();

        for (int i = 0; i < maskStackLen; i++) {
            RubyHash mask = maskStack.get(maskStackLen - (i + 1));

            for (int j = 0; j < ancestorsLen; j++) {
                IRubyObject klass = ancestors.get(j);
                IRubyObject sym;

                if (!(sym = mask.op_aref(context, klass)).isNil()) {
                    String symStr = sym.toString();
                    switch (symStr) {
                        case "immediate": return INTERRUPT_IMMEDIATE;
                        case "on_blocking": return INTERRUPT_ON_BLOCKING;
                        case "never": return INTERRUPT_NEVER;
                        default:
                            throw context.runtime.newThreadError("unknown mask signature");
                    }
                }
            }
        }
        return INTERRUPT_NONE;
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
        return contextRef == null ? null : contextRef.get();
    }

    public Thread getNativeThread() {
        return threadImpl.nativeThread();
    }

    public void setFiberCurrentThread(RubyThread fiberCurrentThread) {
        this.fiberCurrentThread = fiberCurrentThread;
    }

    public RubyThread getFiberCurrentThread() {
        if (fiberCurrentThread == null) return this;
        return fiberCurrentThread;
    }

    /**
     * Perform pre-execution tasks once the native thread is running, but we
     * have not yet called the Ruby code for the thread.
     */
    public void beforeStart() {
    }

    /**
     * Dispose of the current thread by tidying up connections to other stuff
     */
    public void dispose() {
        if (disposed) return;

        synchronized (this) {
            if (disposed) return;

            disposed = true;

            // remove from parent thread group
            threadGroup.remove(this);

            // unlock all locked locks
            unlockAll();

            // mark thread as DEAD
            beDead();
        }

        // unregister from runtime's ThreadService
        getRuntime().getThreadService().unregisterThread(this);
    }

    public static RubyClass createThreadClass(Ruby runtime) {
        // FIXME: In order for Thread to play well with the standard 'new' behavior,
        // it must provide an allocator that can create empty object instances which
        // initialize then fills with appropriate data.
        RubyClass threadClass = runtime.defineClass("Thread", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setThread(threadClass);

        threadClass.setClassIndex(ClassIndex.THREAD);
        threadClass.setReifiedClass(RubyThread.class);

        threadClass.defineAnnotatedMethods(RubyThread.class);

        RubyThread rubyThread = new RubyThread(runtime, threadClass);
        // TODO: need to isolate the "current" thread from class creation
        rubyThread.threadImpl = new NativeThread(rubyThread, Thread.currentThread());
        runtime.getThreadService().setMainThread(Thread.currentThread(), rubyThread);

        // set to default thread group
        runtime.getDefaultThreadGroup().addDirectly(rubyThread);

        threadClass.setMarshal(ObjectMarshal.NOT_MARSHALABLE_MARSHAL);

        // set up Thread::Backtrace::Location class
        RubyClass backtrace = threadClass.defineClassUnder("Backtrace", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        RubyClass location = backtrace.defineClassUnder("Location", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        location.defineAnnotatedMethods(Location.class);

        runtime.setLocation(location);

        return threadClass;
    }

    public static class Location extends RubyObject {
        public Location(Ruby runtime, RubyClass klass, RubyStackTraceElement element) {
            super(runtime, klass);
            this.element = element;
        }

        @JRubyMethod
        public IRubyObject absolute_path(ThreadContext context) {
            return context.runtime.newString(element.getFileName());
        }

        @JRubyMethod
        public IRubyObject base_label(ThreadContext context) {
            return context.runtime.newString(element.getMethodName());
        }

        @JRubyMethod
        public IRubyObject inspect(ThreadContext context) {
            return to_s(context).inspect();
        }

        @JRubyMethod
        public IRubyObject label(ThreadContext context) {
            return context.runtime.newString(element.getMethodName());
        }

        @JRubyMethod
        public IRubyObject lineno(ThreadContext context) {
            return context.runtime.newFixnum(element.getLineNumber());
        }

        @JRubyMethod
        public IRubyObject path(ThreadContext context) {
            return context.runtime.newString(element.getFileName());
        }

        @JRubyMethod
        public IRubyObject to_s(ThreadContext context) {
            return RubyString.newString(context.runtime, element.mriStyleString());
        }

        public static RubyArray newLocationArray(Ruby runtime, RubyStackTraceElement[] elements) {
            return newLocationArray(runtime, elements, 0, elements.length);
        }

        public static RubyArray newLocationArray(Ruby runtime, RubyStackTraceElement[] elements,
            final int offset, final int length) {
            final RubyClass locationClass = runtime.getLocation();

            RubyArray ary = RubyArray.newBlankArray(runtime, length);
            for ( int i = 0; i < length; i++ ) {
                ary.store(i, new RubyThread.Location(runtime, locationClass, elements[i + offset]));
            }

            return ary;
        }

        private final RubyStackTraceElement element;
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
    @JRubyMethod(rest = true, name = "start", meta = true)
    public static RubyThread start(IRubyObject recv, IRubyObject[] args, Block block) {
        // The error message may appear incongruous here, due to the difference
        // between JRuby's Thread model and MRI's.
        // We mimic MRI's message in the name of compatibility.
        if (! block.isGiven()) {
            throw recv.getRuntime().newArgumentError("tried to create Proc object without a block");
        }
        return startThread(recv, args, false, block);
    }

    @Deprecated
    public static RubyThread start19(IRubyObject recv, IRubyObject[] args, Block block) {
        return start(recv, args, block);
    }

    public static RubyThread adopt(IRubyObject recv, Thread t) {
        return adoptThread(recv, t);
    }

    private static RubyThread adoptThread(final IRubyObject recv, Thread t) {
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
        if (!block.isGiven()) throw context.runtime.newThreadError("must be called with a block");
        if (threadImpl != ThreadLike.DUMMY) throw context.runtime.newThreadError("already initialized thread");

        return startThread(context, new RubyRunnable(this, args, block));
    }

    private IRubyObject startThread(ThreadContext context, Runnable runnable) throws RaiseException, OutOfMemoryError {
        final Ruby runtime = context.runtime;
        try {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            this.file = context.getFile();
            this.line = context.getLine();
            initThreadName(runtime, thread, file, line);
            threadImpl = new NativeThread(this, thread);

            addToCorrectThreadGroup(context);

            // JRUBY-2380, associate thread early so it shows up in Thread.list right away, in case it doesn't run immediately
            runtime.getThreadService().associateThread(thread, this);

            threadImpl.start();

            // We yield here to hopefully permit the target thread to schedule
            // MRI immediately schedules it, so this is close but not exact
            Thread.yield();

            return this;
        }
        catch (OutOfMemoryError oome) {
            if (oome.getMessage().equals("unable to create new native thread")) {
                throw runtime.newThreadError(oome.getMessage());
            }
            throw oome;
        }
        catch (SecurityException ex) {
          throw runtime.newThreadError(ex.getMessage());
        }
    }

    private static final String RUBY_THREAD_PREFIX = "Ruby-";

    private static void initThreadName(final Ruby runtime, final Thread thread, final String file, final int line) {
        // "Ruby-0-Thread-16: (irb):21"
        final String newName;
        final StringBuilder name = new StringBuilder(24);
        name
                .append(RUBY_THREAD_PREFIX)
                .append(runtime.getRuntimeNumber())
                .append('-')
                .append("Thread-")
                .append(incAndGetThreadCount(runtime));
        if ( file != null ) {
            name
                    .append(':')
                    .append(' ')
                    .append(file)
                    .append(':')
                    .append(line + 1);
        }
        newName = name.toString();

        thread.setName(newName);
    }

    // TODO likely makes sense to have a counter or the Ruby class directly (could be included with JMX)
    private static final WeakHashMap<Ruby, AtomicLong> threadCount = new WeakHashMap<Ruby, AtomicLong>(4);

    private static long incAndGetThreadCount(final Ruby runtime) {
        AtomicLong counter = threadCount.get(runtime);
        if ( counter == null ) {
            synchronized (runtime) {
                counter = threadCount.get(runtime);
                if ( counter == null ) {
                    threadCount.put(runtime, counter = new AtomicLong(0));
                }
            }
        }
        return counter.incrementAndGet();
    }

    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit, Block block) {
        RubyThread rubyThread = new RubyThread(recv.getRuntime(), (RubyClass) recv);

        if (callInit) {
            rubyThread.callInit(args, block);

            if (rubyThread.threadImpl == ThreadLike.DUMMY) {
                throw recv.getRuntime().newThreadError("uninitialized thread - check " + ((RubyClass) recv).getName() + "#initialize");
            }
        } else {
            // for Thread::start, which does not call the subclass's initialize
            rubyThread.initialize(recv.getRuntime().getCurrentContext(), args, block);
        }

        return rubyThread;
    }

    protected static RubyThread startWaiterThread(final Ruby runtime, int pid, Block block) {
        final IRubyObject waiter = runtime.getClassFromPath("Process::Waiter");
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) waiter);
        rubyThread.op_aset(runtime.newSymbol("pid"), runtime.newFixnum(pid));
        rubyThread.callInit(IRubyObject.NULL_ARRAY, block);
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

    // CHECK_INTS
    public void pollThreadEvents(ThreadContext context) {
        if (anyInterrupted()) {
            executeInterrupts(context, true);
        }
    }

    // RUBY_VM_INTERRUPTED_ANY
    private boolean anyInterrupted() {
        return Thread.interrupted() || (interruptFlag & ~interruptMask) != 0;
    }

    private static void throwThreadKill() {
        throw new ThreadKill();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject handle_interrupt(ThreadContext context, IRubyObject self, IRubyObject _mask, Block block) {
        if (!block.isGiven()) {
            throw context.runtime.newArgumentError("block is needed");
        }

        final RubyHash mask = (RubyHash) TypeConverter.convertToType(_mask, context.runtime.getHash(), "to_hash");

        mask.visitAll(context, HandleInterruptVisitor, null);

        RubyThread th = context.getThread();
        th.interruptMaskStack.add(mask);
        if (th.pendingInterruptQueue.isEmpty()) {
            th.pendingInterruptQueueChecked = false;
            th.setInterrupt();
        }

        try {
            return block.call(context);
        } finally {
            th.interruptMaskStack.remove(th.interruptMaskStack.size() - 1);
            th.setInterrupt();

            th.pollThreadEvents(context);
        }
    }

    private static final RubyHash.VisitorWithState HandleInterruptVisitor = new RubyHash.VisitorWithState() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Object state) {
            if (value instanceof RubySymbol) {
                RubySymbol sym = (RubySymbol) value;
                switch (sym.toString()) {
                    case "immediate" : return;
                    case "on_blocking" : return;
                    case "never" : return;
                    default : throw key.getRuntime().newArgumentError("unknown mask signature");
                }
            }
        }
    };

    @JRubyMethod(name = "pending_interrupt?", meta = true, optional = 1)
    public static IRubyObject pending_interrupt_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return context.getThread().pending_interrupt_p(context, args);
    }

    @JRubyMethod(name = "pending_interrupt?", optional = 1)
    public IRubyObject pending_interrupt_p(ThreadContext context, IRubyObject[] args) {
        if (pendingInterruptQueue.isEmpty()) {
            return context.runtime.getFalse();
        } else {
            if (args.length == 1) {
                IRubyObject err = args[0];
                if (!(err instanceof RubyModule)) {
                    throw context.runtime.newTypeError("class or module required for rescue clause");
                }
                if (pendingInterruptInclude(err)) {
                    return context.runtime.getTrue();
                } else {
                    return context.runtime.getFalse();
                }
            } else {
                return context.runtime.getTrue();
            }
        }
    }

    @JRubyMethod(name = "name=", required = 1)
    public IRubyObject setName(IRubyObject name) {
        final Ruby runtime = getRuntime();

        if (!name.isNil()) {
            RubyString nameStr = StringSupport.checkEmbeddedNulls(runtime, name);
            Encoding enc = nameStr.getEncoding();
            if (!enc.isAsciiCompatible()) {
                throw runtime.newArgumentError("ASCII incompatible encoding (" + enc + ")");
            }
            threadImpl.setRubyName(runtime.freezeAndDedupString(nameStr).asJavaString());
        } else {
            threadImpl.setRubyName(null);
        }

        return name;
    }

    @JRubyMethod(name = "name")
    public IRubyObject getName() {
        Ruby runtime = getRuntime();

        CharSequence rubyName = threadImpl.getRubyName();

        if (rubyName == null) return runtime.getNil();

        return RubyString.newString(getRuntime(), rubyName);
    }

    private boolean pendingInterruptInclude(IRubyObject err) {
        Iterator<IRubyObject> iterator = pendingInterruptQueue.iterator();
        while (iterator.hasNext()) {
            IRubyObject e = iterator.next();
            if (((RubyModule)e).op_le(err).isTrue()) {
                return true;
            }
        }
        return false;
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

    @JRubyMethod(meta = true)
    public static RubyThread current(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getThread();
    }

    @JRubyMethod(meta = true)
    public static RubyThread main(IRubyObject recv) {
        return recv.getRuntime().getThreadService().getMainThread();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject pass(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        ThreadService ts = runtime.getThreadService();
        boolean critical = ts.getCritical();

        ts.setCritical(false);

        try {
            Thread.yield();
        } finally {
            ts.setCritical(critical);
        }

        return runtime.getNil();
    }

    @JRubyMethod(meta = true)
    public static RubyArray list(IRubyObject recv) {
        RubyThread[] activeThreads = recv.getRuntime().getThreadService().getActiveRubyThreads();

        return RubyArray.newArrayMayCopy(recv.getRuntime(), activeThreads);
    }

    private void addToCorrectThreadGroup(ThreadContext context) {
        // JRUBY-3568, inherit threadgroup or use default
        IRubyObject group = context.getThread().group();
        if (!group.isNil()) {
            ((RubyThreadGroup) group).addDirectly(this);
        } else {
            context.runtime.getDefaultThreadGroup().addDirectly(this);
        }
    }

    private IRubyObject getSymbolKey(IRubyObject originalKey) {
        if (originalKey instanceof RubySymbol) {
            return originalKey;
        } else if (originalKey instanceof RubyString) {
            return getRuntime().newSymbol(originalKey.asJavaString());
        } else {
            throw getRuntime().newTypeError(originalKey + " is not a symbol nor a string");
        }
    }

    private synchronized Map<IRubyObject, IRubyObject> getFiberLocals() {
        if (fiberLocalVariables == null) {
            fiberLocalVariables = new HashMap<IRubyObject, IRubyObject>();
        }
        return fiberLocalVariables;
    }

    private synchronized Map<IRubyObject, IRubyObject> getThreadLocals() {
        return getFiberCurrentThread().getThreadLocals0();
    }

    private synchronized Map<IRubyObject, IRubyObject> getThreadLocals0() {
        if (threadLocalVariables == null) {
            threadLocalVariables = new HashMap<IRubyObject, IRubyObject>();
        }
        return threadLocalVariables;
    }

    @Override
    public final Map<Object, IRubyObject> getContextVariables() {
        return contextVariables;
    }

    public boolean isAlive(){
        return threadImpl.isAlive() && status.get() != Status.DEAD;
    }

    @JRubyMethod(name = "[]", required = 1)
    public synchronized IRubyObject op_aref(IRubyObject key) {
        IRubyObject value;
        if ((value = getFiberLocals().get(getSymbolKey(key))) != null) {
            return value;
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "[]=", required = 2)
    public synchronized IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        checkFrozen();

        key = getSymbolKey(key);

        getFiberLocals().put(key, value);
        return value;
    }

    @JRubyMethod(name = "thread_variable?", required = 1)
    public synchronized IRubyObject thread_variable_p(ThreadContext context, IRubyObject key) {
        return context.runtime.newBoolean(getThreadLocals().containsKey(getSymbolKey(key)));
    }

    @JRubyMethod(name = "thread_variable_get", required = 1)
    public synchronized IRubyObject thread_variable_get(ThreadContext context, IRubyObject key) {
        IRubyObject value;
        if ((value = getThreadLocals().get(getSymbolKey(key))) != null) {
            return value;
        }
        return context.nil;
    }

    @JRubyMethod(name = "thread_variable_set", required = 2)
    public synchronized IRubyObject thread_variable_set(ThreadContext context, IRubyObject key, IRubyObject value) {
        checkFrozen();

        key = getSymbolKey(key);

        getThreadLocals().put(key, value);

        return value;
    }

    @JRubyMethod(name = "thread_variables")
    public synchronized IRubyObject thread_variables(ThreadContext context) {
        Map<IRubyObject, IRubyObject> vars = getThreadLocals();
        RubyArray ary = RubyArray.newArray(context.runtime, vars.size());
        for (Map.Entry<IRubyObject, IRubyObject> entry : vars.entrySet()) {
            ary.append(entry.getKey());
        }
        return ary;
    }


    @JRubyMethod
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

    @Deprecated
    public IRubyObject join(IRubyObject[] args) {
        return join(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject join(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long timeoutMillis = Long.MAX_VALUE;

        if (args.length > 0 && !args[0].isNil()) {
            if (args.length > 1) {
                throw runtime.newArgumentError(args.length, 1);
            }
            // MRI behavior: value given in seconds; converted to Float; less
            // than or equal to zero returns immediately; returns nil
            timeoutMillis = (long)(1000.0D * args[0].convertToFloat().getValue());
            if (timeoutMillis <= 0) {
            // TODO: not sure that we should skip calling join() altogether.
            // Thread.join() has some implications for Java Memory Model, etc.
                if (threadImpl.isAlive()) {
                    return context.nil;
                } else {
                   return this;
                }
            }
        }

        if (isCurrent()) {
            throw runtime.newThreadError("thread " + identityString() + " tried to join itself");
        }

        RubyThread currentThread = context.getThread();

        try {
            currentThread.enterSleep();

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
        } finally {
            currentThread.exitSleep();
        }

        if (exitingException != null) {
            // Set $! in the current thread before exiting
            runtime.getGlobalVariables().set("$!", (IRubyObject)exitingException.getException());
            exceptionCaptured = true;
            throw exitingException;

        }

        // check events before leaving
        currentThread.pollThreadEvents(context);

        if (threadImpl.isAlive()) {
            return context.nil;
        } else {
            return this;
        }
    }

    @JRubyMethod
    public IRubyObject value() {
        join(getRuntime().getCurrentContext(), NULL_ARRAY);
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

    @JRubyMethod
    @Override
    public synchronized IRubyObject inspect() {
        // FIXME: There's some code duplication here with RubyObject#inspect
        StringBuilder part = new StringBuilder(32);
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(':');
        part.append(identityString());
        CharSequence name = threadImpl.getRubyName(); // thread.name
        if (notEmpty(name)) {
            part.append('@').append(name);
        }
        if (notEmpty(file) && line >= 0) {
            part.append('@').append(file).append(':').append(line + 1);
        }
        part.append(' ');
        part.append(status.toString().toLowerCase());
        part.append('>');
        return getRuntime().newString(part.toString());
    }

    private boolean notEmpty(CharSequence str) {
        return str != null && str.toString().length() > 0;
    }

    @JRubyMethod(name = "key?", required = 1)
    public RubyBoolean key_p(IRubyObject key) {
        key = getSymbolKey(key);

        return getRuntime().newBoolean(getFiberLocals().containsKey(key));
    }

    @JRubyMethod
    public RubyArray keys() {
        IRubyObject[] keys = new IRubyObject[getFiberLocals().size()];

        return RubyArray.newArrayMayCopy(getRuntime(), getFiberLocals().keySet().toArray(keys));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject stop(ThreadContext context, IRubyObject receiver) {
        RubyThread rubyThread = context.getThread();

        if (context.runtime.getThreadService().getActiveRubyThreads().length == 1) {
            throw context.runtime.newThreadError("stopping only thread\n\tnote: use sleep to stop forever");
        }

        synchronized (rubyThread) {
            rubyThread.pollThreadEvents(context);
            Status oldStatus = rubyThread.status.get();
            try {
                // attempt to decriticalize all if we're the critical thread
                receiver.getRuntime().getThreadService().setCritical(false);

                rubyThread.status.set(Status.SLEEP);
                rubyThread.wait();
            } catch (InterruptedException ie) {
            } finally {
                rubyThread.pollThreadEvents(context);
                rubyThread.status.set(oldStatus);
            }
        }

        return context.nil;
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject kill(IRubyObject receiver, IRubyObject rubyThread, Block block) {
        if (!(rubyThread instanceof RubyThread)) throw receiver.getRuntime().newTypeError(rubyThread, receiver.getRuntime().getThread());
        return ((RubyThread)rubyThread).kill();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject exit(IRubyObject receiver, Block block) {
        RubyThread rubyThread = receiver.getRuntime().getThreadService().getCurrentContext().getThread();

        return rubyThread.kill();
    }

    @JRubyMethod(name = "stop?")
    public RubyBoolean stop_p() {
        // not valid for "dead" state
        return getRuntime().newBoolean(status.get() == Status.SLEEP || status.get() == Status.DEAD);
    }

    @JRubyMethod
    public synchronized RubyThread wakeup() {
        if(!threadImpl.isAlive() && status.get() == Status.DEAD) {
            throw getRuntime().newThreadError("killed thread");
        }

        status.set(Status.RUN);
        interrupt();

        return this;
    }

    @JRubyMethod
    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuntime(), javaPriorityToRubyPriority(threadImpl.getPriority()));
    }

    @JRubyMethod(name = "priority=", required = 1)
    public IRubyObject priority_set(IRubyObject priority) {
        int iPriority = RubyNumeric.fix2int(priority);

        if (iPriority < RUBY_MIN_THREAD_PRIORITY) {
            iPriority = RUBY_MIN_THREAD_PRIORITY;
        } else if (iPriority > RUBY_MAX_THREAD_PRIORITY) {
            iPriority = RUBY_MAX_THREAD_PRIORITY;
        }

        if (threadImpl.isAlive()) {
            int jPriority = rubyPriorityToJavaPriority(iPriority);
            if (jPriority < Thread.MIN_PRIORITY) {
                jPriority = Thread.MIN_PRIORITY;
            } else if (jPriority > Thread.MAX_PRIORITY) {
                jPriority = Thread.MAX_PRIORITY;
            }
            threadImpl.setPriority(jPriority);
        }

        return RubyFixnum.newFixnum(getRuntime(), iPriority);
    }

    /* helper methods to translate Java thread priority (1-10) to
     * Ruby thread priority (-3 to 3) using a quadratic polynomial ant its
     * inverse passing by (Ruby,Java): (-3,1), (0,5) and (3,10)
     * i.e., j = r^2/18 + 3*r/2 + 5
     *       r = 3/2*sqrt(8*j + 41) - 27/2
     */
    public static int javaPriorityToRubyPriority(int javaPriority) {
        double d = 1.5 * Math.sqrt(8.0 * javaPriority + 41) - 13.5;
        return Math.round((float) d);
    }

    public static int rubyPriorityToJavaPriority(int rubyPriority) {
        double d = (rubyPriority * rubyPriority) / 18.0 + 1.5 * rubyPriority + 5;
        return Math.round((float) d);
    }

    /**
     * Simplified utility method for just raising an existing exception in this
     * thread.
     *
     * @param exception the exception to raise
     * @return this thread
     */
    public final IRubyObject raise(IRubyObject exception) {
        return raise(new IRubyObject[]{exception}, Block.NULL_BLOCK);
    }

    /**
     * Simplified utility method for just raising an existing exception in this
     * thread.
     *
     * @param exception the exception to raise
     * @param message the message to use
     * @return this thread
     */
    public final IRubyObject raise(IRubyObject exception, RubyString message) {
        return raise(new IRubyObject[]{exception, message}, Block.NULL_BLOCK);
    }

    @JRubyMethod(optional = 3)
    public IRubyObject raise(IRubyObject[] args, Block block) {
        Ruby runtime = getRuntime();

        RubyThread currentThread = runtime.getCurrentContext().getThread();

        return genericRaise(runtime, args, currentThread);
    }

    public IRubyObject genericRaise(Ruby runtime, IRubyObject[] args, RubyThread currentThread) {
        if (!isAlive()) return runtime.getNil();

        if (currentThread == this) {
            RubyKernel.raise(runtime.getCurrentContext(), runtime.getKernel(), args, Block.NULL_BLOCK);
            // should not reach here
        }

        IRubyObject exception = prepareRaiseException(runtime, args, Block.NULL_BLOCK);

        pendingInterruptEnqueue(exception);
        interrupt();

        return runtime.getNil();
    }

    private IRubyObject prepareRaiseException(Ruby runtime, IRubyObject[] args, Block block) {
        if (args.length == 0) {
            if (errorInfo.isNil()) {
                return new RaiseException(runtime, runtime.getRuntimeError(), "", false).getException();
            }
            return errorInfo;
        }

        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject arg = args[0];

        IRubyObject tmp;
        final RubyException exception;
        if (args.length == 1) {
            if (arg instanceof RubyString) {
                tmp = runtime.getRuntimeError().newInstance(context, args, block);
            }
            else if (arg instanceof ConcreteJavaProxy ) {
                return arg;
            }
            else if ( ! arg.respondsTo("exception") ) {
                throw runtime.newTypeError("exception class/object expected");
            } else {
                tmp = arg.callMethod(context, "exception");
            }
        } else {
            if ( ! arg.respondsTo("exception") ) {
                throw runtime.newTypeError("exception class/object expected");
            }

            tmp = arg.callMethod(context, "exception", args[1]);
        }

        if (!runtime.getException().isInstance(tmp)) {
            throw runtime.newTypeError("exception object expected");
        }

        exception = (RubyException) tmp;

        if (args.length == 3) {
            exception.set_backtrace(args[2]);
        }

        IRubyObject cause = context.getErrorInfo();
        if (cause != exception) {
            exception.setCause(cause);
        }

        return exception;
    }

    @JRubyMethod
    public synchronized IRubyObject run() {
        return wakeup();
    }

    /**
     * Sleep the current thread for millis, waking up on any thread interrupts.
     *
     * We can never be sure if a wait will finish because of a Java "spurious wakeup".  So if we
     * explicitly wakeup and we wait less than requested amount we will return false.  We will
     * return true if we sleep right amount or less than right amount via spurious wakeup.
     *
     * @param millis Number of milliseconds to sleep. Zero sleeps forever.
     */
    public boolean sleep(long millis) throws InterruptedException {
        assert this == getRuntime().getCurrentContext().getThread();
        sleepTask.millis = millis;
        try {
            long timeSlept = executeTask(getContext(), null, sleepTask);
            if (millis == 0 || timeSlept >= millis) {
                // sleep was unbounded or we slept long enough
                return true;
            } else {
                // sleep was bounded and we did not sleep long enough
                return false;
            }
        } finally {
            // ensure we've re-acquired the semaphore, or a subsequent sleep may return immediately
            sleepTask.semaphore.drainPermits();
        }
    }

    public IRubyObject status() {
        return status(getRuntime());
    }
    @JRubyMethod
    public IRubyObject status(ThreadContext context) {
        return status(context.runtime);
    }

    private synchronized IRubyObject status(Ruby runtime) {
        if (threadImpl.isAlive()) {
            return runtime.getThreadStatus(status.get());
        } else if (exitingException != null) {
            return runtime.getNil();
        } else {
            return runtime.getFalse();
        }
    }

    @Deprecated
    public static interface BlockingTask {
        public void run() throws InterruptedException;
        public void wakeup();
    }

    public interface Unblocker<Data> {
        public void wakeup(RubyThread thread, Data self);
    }

    public interface Task<Data, Return> extends Unblocker<Data> {
        public Return run(ThreadContext context, Data data) throws InterruptedException;
        public void wakeup(RubyThread thread, Data data);
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

        @Override
        public void run() throws InterruptedException {
            synchronized (object) {
                object.wait(millis, nanos);
            }
        }

        @Override
        public void wakeup() {
            synchronized (object) {
                object.notify();
            }
        }
    }

    /**
     * A Task for sleeping.
     *
     * The Semaphore is immediately drained on construction, so that any subsequent acquire will block.
     * The sleep is interrupted by releasing a permit. All permits are drained again on exit to ensure
     * the next sleep blocks.
     */
    private static class SleepTask2 implements Task<Object, Long> {
        final Semaphore semaphore = new Semaphore(1);
        long millis;
        {semaphore.drainPermits();}

        @Override
        public Long run(ThreadContext context, Object data) throws InterruptedException {
            long start = System.currentTimeMillis();

            try {
                if (millis == 0) {
                    semaphore.tryAcquire(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } else {
                    semaphore.tryAcquire(millis, TimeUnit.MILLISECONDS);
                }

                return System.currentTimeMillis() - start;
            } finally {
                semaphore.drainPermits();
            }
        }

        @Override
        public void wakeup(RubyThread thread, Object data) {
            semaphore.release();
        }
    }

    @Deprecated
    public void executeBlockingTask(BlockingTask task) throws InterruptedException {
        try {
            this.currentBlockingTask = task;
            enterSleep();
            pollThreadEvents();
            task.run();
        } finally {
            exitSleep();
            currentBlockingTask = null;
            pollThreadEvents();
        }
    }

    public <Data, Return> Return executeTask(ThreadContext context, Data data, Task<Data, Return> task) throws InterruptedException {
        try {
            this.unblockArg = data;
            this.unblockFunc = task;

            // check for interrupt before going into blocking call
            pollThreadEvents(context);

            enterSleep();

            return task.run(context, data);
        } finally {
            exitSleep();
            this.unblockFunc = null;
            this.unblockArg = null;
            pollThreadEvents(context);
        }
    }

    public void enterSleep() {
        status.set(Status.SLEEP);
    }

    public void exitSleep() {
        if (status.get() != Status.ABORTING) {
            status.set(Status.RUN);
        }
    }

    @JRubyMethod(name = {"kill", "exit", "terminate"})
    public IRubyObject kill() {
        Ruby runtime = getRuntime();
        // need to reexamine this
        RubyThread currentThread = runtime.getCurrentContext().getThread();

        if (currentThread == runtime.getThreadService().getMainThread()) {
            // rb_exit to hard exit process...not quite right for us
        }

        status.set(Status.ABORTING);

        return genericKill(runtime, currentThread);
    }

    private IRubyObject genericKill(Ruby runtime, RubyThread currentThread) {
        // If the killee thread is the same as the killer thread, just die
        if (currentThread == this) throwThreadKill();

        pendingInterruptEnqueue(RubyFixnum.zero(runtime));
        interrupt();

        return this;
    }

    private void pendingInterruptEnqueue(IRubyObject v) {
        pendingInterruptQueue.add(v);
        pendingInterruptQueueChecked = false;
    }

    /**
     * Used for finalizers that need to kill a Ruby thread. Finalizers run in
     * a VM thread to which we do not want to attach a ThreadContext and within
     * which we do not want to check for Ruby thread events. This mechanism goes
     * directly to mail delivery, bypassing all Ruby Thread-related steps.
     */
    public void dieFromFinalizer() {
        genericKill(getRuntime(), null);
    }

    //private static void debug(RubyThread thread, String message) {
    //    if (LOG.isDebugEnabled()) LOG.debug( "{} ({}): {}", Thread.currentThread(), thread.status, message );
    //}

    @JRubyMethod
    public IRubyObject safe_level() {
        throw getRuntime().newNotImplementedError("Thread-specific SAFE levels are not supported");
    }

    public IRubyObject backtrace(ThreadContext context) {
        return backtrace20(context, NULL_ARRAY);
    }

    @JRubyMethod(name = "backtrace", optional = 2)
    public IRubyObject backtrace20(ThreadContext context, IRubyObject[] args) {
        ThreadContext myContext = getContext();

        // context can be nil if we have not started or GC has claimed our context
        if (myContext == null) return context.nil;

        Thread nativeThread = getNativeThread();

        // nativeThread can be null if the thread has terminated and GC has claimed it
        if (nativeThread == null) return context.nil;

        // nativeThread may have finished
        if (!nativeThread.isAlive()) return context.nil;

        Ruby runtime = context.runtime;
        Integer[] ll = RubyKernel.levelAndLengthFromArgs(runtime, args, 0);
        Integer level = ll[0], length = ll[1];

        return myContext.createCallerBacktrace(level, length, getNativeThread().getStackTrace());
    }

    @JRubyMethod(optional = 2)
    public IRubyObject backtrace_locations(ThreadContext context, IRubyObject[] args) {
        ThreadContext myContext = getContext();

        if (myContext == null) return context.nil;

        Thread nativeThread = getNativeThread();

        // nativeThread can be null if the thread has terminated and GC has claimed it
        if (nativeThread == null) return context.nil;

        // nativeThread may have finished
        if (!nativeThread.isAlive()) return context.nil;

        Ruby runtime = context.runtime;
        Integer[] ll = RubyKernel.levelAndLengthFromArgs(runtime, args, 0);
        Integer level = ll[0], length = ll[1];

        return myContext.createCallerLocations(level, length, getNativeThread().getStackTrace());
    }

    @JRubyMethod(name = "report_on_exception=")
    public IRubyObject report_on_exception_set(ThreadContext context, IRubyObject state) {
        if (state.isNil()) {
            reportOnException = state;
        } else {
            reportOnException = context.runtime.newBoolean(state.isTrue());
        }
        return this;
    }

    @JRubyMethod(name = "report_on_exception")
    public IRubyObject report_on_exception(ThreadContext context) {
        return reportOnException;
    }

    @JRubyMethod(name = "report_on_exception=", meta = true)
    public static IRubyObject report_on_exception_set(ThreadContext context, IRubyObject self, IRubyObject state) {
        Ruby runtime = context.runtime;
        
        if (state.isNil()) {
            runtime.setReportOnException(state);
        } else {
            runtime.setReportOnException(runtime.newBoolean(state.isTrue()));
        }

        return self;
    }

    @JRubyMethod(name = "report_on_exception", meta = true)
    public static IRubyObject report_on_exception(ThreadContext context, IRubyObject self) {
        return context.runtime.getReportOnException();
    }

    public StackTraceElement[] javaBacktrace() {
        if (threadImpl instanceof NativeThread) {
            return ((NativeThread)threadImpl).getThread().getStackTrace();
        }

        // Future-based threads can't get a Java trace
        return EMPTY_STACK_TRACE;
    }

    private boolean isCurrent() {
        return threadImpl.isCurrent();
    }

    public void exceptionRaised(RaiseException exception) {
        assert isCurrent();

        RubyException rubyException = exception.getException();
        Ruby runtime = rubyException.getRuntime();
        if (runtime.getSystemExit().isInstance(rubyException)) {
            runtime.getThreadService().getMainThread().raise(rubyException);
        } else if (abortOnException(runtime)) {
            runtime.getThreadService().getMainThread().raise(rubyException);
            return;
        } else if (reportOnException.isTrue()) {
            printReportExceptionWarning();
            runtime.printError(exception.getException());
        }
        exitingException = exception;
    }

    protected void printReportExceptionWarning() {
        PrintStream errorStream = getRuntime().getErrorStream();
        String name = threadImpl.getReportName();
        errorStream.println("warning: thread \"" + name + "\" terminated with exception:");
    }

    /**
     * For handling all non-Ruby exceptions bubbling out of threads
     * @param exception
     */
    @SuppressWarnings("deprecation")
    public void exceptionRaised(Throwable exception) {
        if (exception instanceof RaiseException) {
            exceptionRaised((RaiseException)exception);
            return;
        }

        assert isCurrent();

        Ruby runtime = getRuntime();
        if (abortOnException(runtime) && exception instanceof Error) {
            // re-propagate on main thread
            exceptionCaptured = true;
            runtime.getThreadService().getMainThread().raise(JavaUtil.convertJavaToUsableRubyObject(runtime, exception));
        } else {
            // just rethrow on this thread, let system handlers report it
            Helpers.throwException(exception);
        }
    }

    private boolean abortOnException(Ruby runtime) {
        return (runtime.isGlobalAbortOnExceptionEnabled() || abortOnException);
    }

    public static RubyThread mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations or the given timeout.
     *
     * @param io the RubyIO that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @return true if the IO's channel became ready for the requested operations, false if
     *         it was not selectable.
     */
    public boolean select(RubyIO io, int ops) {
        return select(io.getChannel(), io.getOpenFile(), ops);
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations or the given timeout.
     *
     * @param io the RubyIO that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @param timeout a timeout in ms to limit the select. Less than zero selects forever,
     *                zero selects and returns ready channels or nothing immediately, and
     *                greater than zero selects for at most that many ms.
     * @return true if the IO's channel became ready for the requested operations, false if
     *         it timed out or was not selectable.
     */
    public boolean select(RubyIO io, int ops, long timeout) {
        return select(io.getChannel(), io.getOpenFile(), ops, timeout);
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations.
     *
     * @param channel the channel to perform a select against. If this is not
     *                a selectable channel, then this method will just return true.
     * @param fptr the fptr that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @return true if the channel became ready for the requested operations, false if
     *         it was not selectable.
     */
    public boolean select(Channel channel, OpenFile fptr, int ops) {
        return select(channel, fptr, ops, -1);
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations.
     *
     * @param channel the channel to perform a select against. If this is not
     *                a selectable channel, then this method will just return true.
     * @param io the RubyIO that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @return true if the channel became ready for the requested operations, false if
     *         it was not selectable.
     */
    public boolean select(Channel channel, RubyIO io, int ops) {
        return select(channel, io == null ? null : io.getOpenFile(), ops, -1);
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations or the given timeout.
     *
     * @param channel the channel to perform a select against. If this is not
     *                a selectable channel, then this method will just return true.
     * @param io the RubyIO that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @param timeout a timeout in ms to limit the select. Less than zero selects forever,
     *                zero selects and returns ready channels or nothing immediately, and
     *                greater than zero selects for at most that many ms.
     * @return true if the channel became ready for the requested operations, false if
     *         it timed out or was not selectable.
     */
    public boolean select(Channel channel, RubyIO io, int ops, long timeout) {
        return select(channel, io == null ? null : io.getOpenFile(), ops, timeout);
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations or the given timeout.
     *
     * @param channel the channel to perform a select against. If this is not
     *                a selectable channel, then this method will just return true.
     * @param fptr the fptr that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@see java.nio.channels.SelectionKey}.
     * @param timeout a timeout in ms to limit the select. Less than zero selects forever,
     *                zero selects and returns ready channels or nothing immediately, and
     *                greater than zero selects for at most that many ms.
     * @return true if the channel became ready for the requested operations, false if
     *         it timed out or was not selectable.
     */
    public boolean select(Channel channel, OpenFile fptr, int ops, long timeout) {
        // Use selectables but only if they're not associated with a file (which has odd select semantics)
        ChannelFD fd = fptr == null ? null : fptr.fd();
        if (channel instanceof SelectableChannel && fd != null) {
            SelectableChannel selectable = (SelectableChannel)channel;

            synchronized (selectable.blockingLock()) {
                boolean oldBlocking = selectable.isBlocking();

                SelectionKey key;
                try {
                    selectable.configureBlocking(false);

                    if (fptr != null) fptr.addBlockingThread(this);
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
                    throw getRuntime().newIOErrorFromException(ioe);
                } finally {
                    // Note: I don't like ignoring these exceptions, but it's
                    // unclear how likely they are to happen or what damage we
                    // might do by ignoring them. Note that the pieces are separate
                    // so that we can ensure one failing does not affect the others
                    // running.

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
                    if (fptr != null) fptr.removeBlockingThread(this);

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

    @SuppressWarnings("deprecated")
    public synchronized void interrupt() {
        setInterrupt();

        Selector activeSelector = currentSelector;
        if (activeSelector != null) {
            activeSelector.wakeup();
        }
        BlockingIO.Condition iowait = blockingIO;
        if (iowait != null) {
            iowait.cancel();
        }

        Unblocker task = this.unblockFunc;
        if (task != null) {
            task.wakeup(this, unblockArg);
        }

        // deprecated
        {
            BlockingTask t = currentBlockingTask;
            if (t != null) {
                t.wakeup();
            }
        }

        // If this thread is sleeping or stopped, wake it
        notify();
    }

    public void setInterrupt() {
        while (true) {
            int oldFlag = interruptFlag;
            if (INTERRUPT_FLAG_UPDATER.compareAndSet(this, oldFlag, oldFlag | PENDING_INTERRUPT_MASK)) {
                return;
            }
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
            throw context.runtime.newRuntimeError("Error with selector: " + ioe);
        } catch (InterruptedException ex) {
            // FIXME: not correct exception
            throw context.runtime.newRuntimeError("Interrupted");
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

    public RubyThreadGroup getThreadGroup() {
        return threadGroup;
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
        if (this.threadImpl != other.threadImpl && (this.threadImpl == ThreadLike.DUMMY || !this.threadImpl.equals(other.threadImpl))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 97 * 3 + (this.threadImpl != ThreadLike.DUMMY ? this.threadImpl.hashCode() : 0);
    }

    @Override
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

    /**
     * This is intended to be used to raise exceptions in Ruby threads from non-
     * Ruby threads like Timeout's thread.
     *
     * @param args Same args as for Thread#raise
     */
    @Deprecated
    public void internalRaise(IRubyObject[] args) {
        Ruby runtime = getRuntime();

        genericRaise(runtime, args, runtime.getCurrentContext().getThread());
    }

    @Deprecated
    public void receiveMail(ThreadService.Event event) {
    }

    @Deprecated
    public void checkMail(ThreadContext context) {
    }

    @Deprecated
    private volatile BlockingTask currentBlockingTask;

    @Deprecated
    public boolean selectForAccept(RubyIO io) {
        return select(io, SelectionKey.OP_ACCEPT);
    }

    @Deprecated // moved to ruby kernel
    public static IRubyObject exclusive(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.runtime;
        ThreadService ts = runtime.getThreadService();
        boolean critical = ts.getCritical();

        ts.setCritical(true);

        try {
            return block.yieldSpecific(context);
        } finally {
            ts.setCritical(critical);
        }
    }
}
