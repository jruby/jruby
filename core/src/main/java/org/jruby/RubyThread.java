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
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;

import com.headius.backport9.stack.StackWalker;
import org.jcodings.Encoding;
import org.joni.Matcher;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.RubyNativeThread;
import org.jruby.internal.runtime.RubyRunnable;
import org.jruby.internal.runtime.ThreadLike;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.AdoptedNativeThread;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.backtrace.FrameType;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.BlockingIO;
import org.jruby.util.io.ChannelFD;
import org.jruby.util.io.OpenFile;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.common.IRubyWarnings.ID;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.runtime.Visibility.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

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

    private static final StackWalker WALKER = ThreadContext.WALKER;

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
    private volatile boolean reportOnException;

    /** The final value resulting from the thread's execution */
    private volatile IRubyObject finalResult;

    private String file; private int line; // Thread.new location (for inspect)

    /**
     * The exception currently being raised out of the thread. We reference
     * it here to continue propagating it while handling thread shutdown
     * logic and abort_on_exception.
     */
    private volatile Throwable exitingException;

    /** The ThreadGroup to which this thread belongs */
    private volatile RubyThreadGroup threadGroup;

    /** Per-thread "current exception" */
    private volatile IRubyObject errorInfo;

    /** Weak reference to the ThreadContext for this thread. */
    private volatile WeakReference<ThreadContext> contextRef;

    /** Whether to scan for cross-thread events */
    //private volatile boolean handleInterrupt = true;

    /** Stack of interrupt masks active for this thread */
    private final Vector<RubyHash> interruptMaskStack = new Vector<>(4);

    /** Thread-local tuple used for sleeping (semaphore, millis, nanos) */
    private final SleepTask2 sleepTask = new SleepTask2();

    /** Whether this is an "adopted" thread not created by Ruby code */
    private final boolean adopted;

    public static final int RUBY_MIN_THREAD_PRIORITY = -3;
    public static final int RUBY_MAX_THREAD_PRIORITY = 3;

    /** Thread statuses */
    public enum Status {
        RUN, SLEEP, DEAD, NATIVE;

        public final ByteList bytes;

        Status() {
            bytes = new ByteList(toString().toLowerCase().getBytes(RubyEncoding.UTF8), false);
        }
    }

    /** Current status in an atomic reference */
    private volatile Status status = Status.RUN;
    private final static AtomicReferenceFieldUpdater<RubyThread, Status> STATUS =
            AtomicReferenceFieldUpdater.newUpdater(RubyThread.class, Status.class, "status");

    private volatile boolean killed = false;

    /** Mail slot for cross-thread events */
    private final Queue<IRubyObject> pendingInterruptQueue = new ConcurrentLinkedQueue<>();

    /** A function to use to unblock this thread, if possible */
    private volatile Unblocker unblockFunc;

    /** Argument to pass to the unblocker */
    private volatile Object unblockArg;

    /** The list of locks this thread currently holds, so they can be released on exit */
    private final List<Lock> heldLocks = new Vector<>();

    /** Whether or not this thread has been disposed of */
    private volatile boolean disposed = false;

    /** Interrupt flags */
    private volatile int interruptFlag = 0;

    /** Interrupt mask to use for disabling certain types */
    private volatile int interruptMask;

    /** Short circuit to avoid-re-scanning for interrupts */
    private volatile boolean pendingInterruptQueueChecked = false;

    private volatile BlockingTask currentBlockingTask;

    private volatile Selector currentSelector;

    private volatile RubyThread fiberCurrentThread;

    private IRubyObject scheduler;
    private volatile int blockingCount = 1;

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

    protected RubyThread(Ruby runtime, RubyClass type, boolean adopted) {
        super(runtime, type);

        finalResult = errorInfo = runtime.getNil();
        reportOnException = runtime.isReportOnException();
        scheduler = runtime.getNil();

        this.adopted = adopted;
    }

    public RubyThread(Ruby runtime, RubyClass klass, Runnable runnable) {
        this(runtime, klass, true);

        startThread(runtime.getCurrentContext(), runnable, "<internal>", -1);
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
                } else if (err instanceof RubyFixnum fixErr && (fixErr.getLongValue() == 0 ||
                        fixErr.getLongValue() == 1 ||
                        fixErr.getLongValue() == 2)) {
                    toKill();
                } else {
                    if (getStatus() == Status.SLEEP) {
                        exitSleep();
                    }
                    // if it's a Ruby exception, force the cause through
                    IRubyObject[] args;
                    if (err instanceof RubyException) {
                        args = Helpers.arrayOf(err, RubyHash.newKwargs(runtime, "cause", ((RubyException) err).cause(context)));
                    } else {
                        args = Helpers.arrayOf(err);
                    }
                    RubyKernel.raise(context, this, args, Block.NULL_BLOCK);
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
        killed = true;
        STATUS.set(this, Status.RUN);
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
        int idx = interruptMaskStack.size();
        if (idx == 0) return INTERRUPT_NONE; // fast return

        List<IRubyObject> ancestors = getMetaClass(err).getAncestorList();
        final int ancestorsLen = ancestors.size();

        while (--idx >= 0) {
            RubyHash mask = interruptMaskStack.get(idx);

            for (IRubyObject klass: ancestors) {
                IRubyObject sym =  mask.op_aref(context, klass);
                if (!sym.isNil()) return checkInterruptMask(context, sym);
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

    public void setContext(final ThreadContext context) {
        this.contextRef = new WeakReference<>(context);
    }

    public void clearContext() {
        WeakReference<ThreadContext> contextRef = this.contextRef;
        if (contextRef != null) {
            contextRef.clear();
            this.contextRef = null;
        }
    }

    public ThreadContext getContext() {
        WeakReference<ThreadContext> contextRef = this.contextRef;
        return contextRef == null ? null : contextRef.get();
    }

    public Thread getNativeThread() {
        return threadImpl.nativeThread();
    }

    public void setFiberCurrentThread(RubyThread fiberCurrentThread) {
        this.fiberCurrentThread = fiberCurrentThread;
    }

    public RubyThread getFiberCurrentThread() {
        RubyThread fiberCurrentThread = this.fiberCurrentThread;
        return fiberCurrentThread == null ? this : fiberCurrentThread;
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

            // close scheduler, if any
            if (scheduler != null && !scheduler.isNil()) {
                FiberScheduler.close(getContext(), scheduler);
            }

            // mark thread as DEAD
            beDead();
        }

        // unregister from runtime's ThreadService
        getRuntime().getThreadService().unregisterThread(this);
    }

    public static RubyClass createThreadClass(ThreadContext context, RubyClass Object) {
        // FIXME: In order for Thread to play well with the standard 'new' behavior,
        // it must provide an allocator that can create empty object instances which
        // initialize then fills with appropriate data.
        RubyClass threadClass = defineClass(context, "Thread", Object, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyThread.class).
                marshalWith(ObjectMarshal.NOT_MARSHALABLE_MARSHAL).
                classIndex(ClassIndex.THREAD).
                defineMethods(context, RubyThread.class);

        // main thread is considered adopted, since it is initiated by the JVM
        RubyThread rubyThread = new RubyThread(context.runtime, threadClass, true);

        // TODO: need to isolate the "current" thread from class creation
        rubyThread.threadImpl = new AdoptedNativeThread(rubyThread, Thread.currentThread());
        context.runtime.getThreadService().setMainThread(Thread.currentThread(), rubyThread);
        context.runtime.getDefaultThreadGroup().addDirectly(rubyThread);  // set to default thread group

        // set up Thread::Backtrace::Location class
        var backtrace = threadClass.defineClassUnder(context, "Backtrace", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, Backtrace.class);
        RubyClass location = backtrace.defineClassUnder(context, "Location", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, Location.class);
        context.runtime.setLocation(location);

        return threadClass;
    }

    public static class Backtrace extends RubyObject {
        public Backtrace(Ruby runtime, RubyClass metaClass) {
            super(runtime, metaClass);
        }

        @JRubyMethod(module = true)
        public static IRubyObject limit(ThreadContext context, IRubyObject self) {
            return asFixnum(context, context.runtime.getInstanceConfig().getBacktraceLimit());
        }
    }

    public static class Location extends RubyObject {
        private final RubyStackTraceElement element;

        private transient RubyString baseLabel = null;
        private transient RubyString label = null;

        public Location(Ruby runtime, RubyClass klass, RubyStackTraceElement element) {
            super(runtime, klass);
            this.element = element;
        }

        @JRubyMethod
        public IRubyObject absolute_path(ThreadContext context) {
            return newString(context, context.runtime.getLoadService().getPathForLocation(element.getFileName()));
        }

        @JRubyMethod
        public IRubyObject base_label(ThreadContext context) {
            if (baseLabel == null) baseLabel = newString(context, element.getMethodName());
            return baseLabel;
        }

        @JRubyMethod
        public IRubyObject inspect(ThreadContext context) {
            return to_s(context).inspect();
        }

        @JRubyMethod
        public IRubyObject label(ThreadContext context) {
            if (element.getFrameType() == FrameType.BLOCK) {
                // NOTE: "block in " + ... logic, now, also at RubyStackTraceElement.to_s_mri
                if (label == null) label = newString(context, "block in " + element.getMethodName());
                return label;
            }

            return base_label(context);
        }

        @JRubyMethod
        public IRubyObject lineno(ThreadContext context) {
            return asFixnum(context, element.getLineNumber());
        }

        @JRubyMethod
        public IRubyObject path(ThreadContext context) {
            return newString(context, element.getFileName());
        }

        @JRubyMethod
        public IRubyObject to_s(ThreadContext context) {
            return RubyStackTraceElement.to_s_mri(context, element);
        }

        public static RubyArray newLocationArray(Ruby runtime, RubyStackTraceElement[] elements) {
            return newLocationArray(runtime, elements, 0, elements.length);
        }

        public static RubyArray newLocationArray(Ruby runtime, RubyStackTraceElement[] elements,
            final int offset, final int length) {

            IRubyObject[] ary = new IRubyObject[length];
            for ( int i = 0; i < length; i++ ) {
                ary[i] = newLocation(runtime, elements[i + offset]);
            }

            return RubyArray.newArrayNoCopy(runtime, ary);
        }

        public static Location newLocation(Ruby runtime, RubyStackTraceElement elt) {
            return new Location(runtime, runtime.getLocation(), elt);
        }

    }

    /**
     * <code>Thread.new</code>
     * <p>
     * Thread.new( <i>[ arg ]*</i> ) {| args | block }$ -&gt; aThread
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
    @JRubyMethod(name = {"new", "fork"}, rest = true, meta = true, keywords = true)
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return startThread(recv, args, true, block);
    }

    /**
     * @param recv
     * @param args
     * @param block
     * @return ""
     * @deprecated Use {@link RubyThread#start(ThreadContext, IRubyObject, IRubyObject[], Block)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyThread start(IRubyObject recv, IRubyObject[] args, Block block) {
        return start(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    /**
     * Basically the same as Thread.new . However, if class Thread is
     * subclassed, then calling start in that subclass will not invoke the
     * subclass's initialize method.
     */
    @JRubyMethod(rest = true, name = "start", meta = true)
    public static RubyThread start(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // The error message may appear incongruous here, due to the difference
        // between JRuby's Thread model and MRI's.
        // We mimic MRI's message in the name of compatibility.
        if (!block.isGiven()) throw argumentError(context, "tried to create Proc object without a block");

        return startThread(recv, args, false, block);
    }

    public static RubyThread adopt(IRubyObject recv, Thread t) {
        final Ruby runtime = recv.getRuntime();
        return adoptThread(runtime, runtime.getThreadService(), (RubyClass) recv, t);
    }

    public static RubyThread adopt(Ruby runtime, ThreadService service, Thread thread) {
        return adoptThread(runtime, service, runtime.getThread(), thread);
    }

    private static RubyThread adoptThread(final Ruby runtime, final ThreadService service,
                                          final RubyClass recv, final Thread thread) {
        final RubyThread rubyThread = new RubyThread(runtime, recv, true);

        rubyThread.threadImpl = new AdoptedNativeThread(rubyThread, thread);
        ThreadContext context = service.registerNewThread(rubyThread);
        service.associateThread(thread, rubyThread);

        context.preAdoptThread();

        // set to default thread group
        runtime.getDefaultThreadGroup().addDirectly(rubyThread);

        return rubyThread;
    }

    @JRubyMethod(rest = true, visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        int callInfo = ThreadContext.resetCallInfo(context);
        if (!block.isGiven()) throw context.runtime.newThreadError("must be called with a block");
        if (threadImpl != ThreadLike.DUMMY) throw context.runtime.newThreadError("already initialized thread");

        BlockBody body = block.getBody();
        startThread(context, new RubyRunnable(this, context, args, block, callInfo), body.getFile(), body.getLine());

        return context.nil;
    }

    private Thread startThread(ThreadContext context, Runnable runnable, String file, int line) throws RaiseException, OutOfMemoryError {
        final Ruby runtime = context.runtime;
        try {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);

            this.file = file;
            this.line = line;

            initThreadName(runtime, thread, file, line);
            
            threadImpl = new RubyNativeThread(this, thread);

            addToCorrectThreadGroup(context);

            // JRUBY-2380, associate thread early so it shows up in Thread.list right away, in case it doesn't run immediately
            runtime.getThreadService().associateThread(thread, this);

            // copy parent thread's interrupt masks
            copyInterrupts(context, context.getThread().interruptMaskStack, this.interruptMaskStack);

            // start the native thread
            thread.start();

            // We yield here to hopefully permit the target thread to schedule
            // MRI immediately schedules it, so this is close but not exact
            Thread.yield();

            return thread;
        }
        catch (OutOfMemoryError oome) {
            if ("unable to create new native thread".equals(oome.getMessage())) {
                throw runtime.newThreadError(oome.getMessage());
            }
            throw oome;
        }
        catch (SecurityException ex) {
            throw runtime.newThreadError(ex.getMessage());
        }
    }

    private static final RubyHash[] NULL_ARRAY = new RubyHash[0];

    private static void copyInterrupts(ThreadContext context, Vector<RubyHash> sourceStack, Vector<RubyHash> targetStack) {
        // We do this in a loop so we can use synchronized collections but not deadlock inside addAll.
        // See https://github.com/jruby/jruby/issues/5520
        for (RubyHash h : sourceStack.toArray(NULL_ARRAY)) {
            targetStack.add(h.dupFast(context));
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

    private static long incAndGetThreadCount(final Ruby runtime) {
        return runtime.getThreadService().incrementAndGetThreadCount();
    }

    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyThread rubyThread = new RubyThread(runtime, (RubyClass) recv, false);

        if (callInit) {
            rubyThread.callInit(args, block);

            if (rubyThread.threadImpl == ThreadLike.DUMMY) {
                throw runtime.newThreadError(str(runtime, "uninitialized thread - check " , types(runtime, (RubyClass) recv), "#initialize"));
            }
        } else {
            // for Thread::start, which does not call the subclass's initialize
            rubyThread.initialize(runtime.getCurrentContext(), args, block);
        }

        return rubyThread;
    }

    protected static RubyThread startWaiterThread(final Ruby runtime, long pid, Block block) {
        final IRubyObject waiter = runtime.getProcess().getConstantAt("Waiter"); // Process::Waiter
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) waiter, false);
        rubyThread.op_aset(runtime.newSymbol("pid"), runtime.newFixnum(pid));
        rubyThread.callInit(IRubyObject.NULL_ARRAY, block);
        return rubyThread;
    }

    public synchronized void cleanTerminate(IRubyObject result) {
        finalResult = result;
    }

    public void beDead() {
        STATUS.set(this, Status.DEAD);
    }

    public void pollThreadEvents() {
        pollThreadEvents(metaClass.runtime.getCurrentContext());
    }

    /**
     * Poll for thread events (raise, kill), propagating only events that are unmasked
     * (see {@link #handle_interrupt(ThreadContext, IRubyObject, IRubyObject, Block)} or intended to fire before
     * blocking operations if blocking = true.
     *
     * @param context the context
     * @param blocking whether to trigger blocking operation interrupts
     */
    public void pollThreadEvents(ThreadContext context, boolean blocking) {
        if (blocking) {
            blockingThreadPoll(context);
        } else {
            pollThreadEvents(context);
        }
    }

    // CHECK_INTS
    public void pollThreadEvents(ThreadContext context) {
        if (anyInterrupted()) {
            executeInterrupts(context, false);
        }
    }

    // RUBY_VM_CHECK_INTS_BLOCKING
    public void blockingThreadPoll(ThreadContext context) {
        if (pendingInterruptQueue.isEmpty() && !anyInterrupted()) {
            return;
        }

        pendingInterruptQueueChecked = false;
        setInterrupt();
        executeInterrupts(context, true);
    }

    // RUBY_VM_INTERRUPTED_ANY
    private boolean anyInterrupted() {
        return Thread.interrupted() || (interruptFlag & ~interruptMask) != 0;
    }

    /**
     * MRI: rb_threadptr_to_kill
     */
    private void throwThreadKill() {
        killed = true;
        throw new ThreadKill();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject handle_interrupt(ThreadContext context, IRubyObject self, IRubyObject _mask, Block block) {
        if (!block.isGiven()) throw argumentError(context, "block is needed");

        final RubyHash mask = _mask.convertToHash().dupFast(context);
        if (mask.isEmpty()) return block.yield(context, context.nil);

        mask.visitAll(context, HandleInterruptVisitor, null);
        mask.setFrozen(true);

        return context.getThread().handleInterrupt(context, mask, block);
    }

    private IRubyObject handleInterrupt(ThreadContext context, RubyHash mask, BiFunction<ThreadContext, IRubyObject, IRubyObject> block) {
        return handleInterrupt(context, mask, context.nil, block);
    }

    private <StateType> IRubyObject handleInterrupt(ThreadContext context, RubyHash mask, StateType state, BiFunction<ThreadContext, StateType, IRubyObject> block) {
        Vector<RubyHash> interruptMaskStack = this.interruptMaskStack;

        interruptMaskStack.add(mask);

        Queue<IRubyObject> pendingInterruptQueue = this.pendingInterruptQueue;

        if (!pendingInterruptQueue.isEmpty()) {
            pendingInterruptQueueChecked = false;
            setInterrupt();
        }

        try {
            // check for any interrupts that should fire with new masks
            pollThreadEvents();

            return block.apply(context, state);
        } finally {
            interruptMaskStack.remove(interruptMaskStack.size() - 1);

            if (!pendingInterruptQueue.isEmpty()) {
                pendingInterruptQueueChecked = false;
                setInterrupt();
            }

            // check for pending interrupts that were masked
            pollThreadEvents(context);
        }
    }

    private static final RubyHash.VisitorWithState HandleInterruptVisitor = new RubyHash.VisitorWithState<Void>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Void state) {
            checkInterruptMask(context, value);
        }
    };

    private static int checkInterruptMask(final ThreadContext context, final IRubyObject sym) {
        if (sym instanceof RubySymbol name) {
            switch (name.idString()) {
                case "immediate": return INTERRUPT_IMMEDIATE;
                case "on_blocking": return INTERRUPT_ON_BLOCKING;
                case "never": return INTERRUPT_NEVER;
            }
        }
        throw argumentError(context, "unknown mask signature");
    }

    @JRubyMethod(name = "pending_interrupt?", meta = true)
    public static IRubyObject pending_interrupt_p_s(ThreadContext context, IRubyObject self) {
        return context.getThread().pending_interrupt_p(context);
    }

    @JRubyMethod(name = "pending_interrupt?", meta = true)
    public static IRubyObject pending_interrupt_p_s(ThreadContext context, IRubyObject self, IRubyObject err) {
        return context.getThread().pending_interrupt_p(context, err);
    }

    @JRubyMethod(name = "pending_interrupt?")
    public IRubyObject pending_interrupt_p(ThreadContext context) {
        return asBoolean(context, !pendingInterruptQueue.isEmpty());
    }

    @JRubyMethod(name = "pending_interrupt?")
    public IRubyObject pending_interrupt_p(ThreadContext context, IRubyObject err) {
        if (pendingInterruptQueue.isEmpty()) return context.fals;
        if (!(err instanceof RubyModule)) throw typeError(context, "class or module required for rescue clause");

        return pendingInterruptInclude((RubyModule) err) ? context.tru : context.fals;
    }

    private boolean pendingInterruptInclude(RubyModule err) {
        Iterator<IRubyObject> iterator = pendingInterruptQueue.iterator();
        while (iterator.hasNext()) {
            IRubyObject e = iterator.next();
            if (e.getMetaClass().isKindOfModule(err)) return true;
        }
        return false;
    }

    /**
     * @param name
     * @return ""
     * @deprecated Use {@link RubyThread#setName(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject setName(IRubyObject name) {
        return setName(getCurrentContext(), name);
    }

    @JRubyMethod(name = "name=")
    public IRubyObject setName(ThreadContext context, IRubyObject name) {
        if (!name.isNil()) {
            RubyString nameStr = checkEmbeddedNulls(context, name);
            Encoding enc = nameStr.getEncoding();
            if (!enc.isAsciiCompatible()) throw argumentError(context, "ASCII incompatible encoding (" + enc + ")");

            threadImpl.setRubyName(context.runtime.freezeAndDedupString(nameStr).asJavaString());
        } else {
            threadImpl.setRubyName(null);
        }

        return name;
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyThread#getName(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public IRubyObject getName() {
        return getName(getCurrentContext());
    }

    @JRubyMethod(name = "name")
    public IRubyObject getName(ThreadContext context) {
        CharSequence rubyName = threadImpl.getRubyName();
        return rubyName == null ? context.nil : newString(context, rubyName.toString());
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
    public static IRubyObject pass(ThreadContext context, IRubyObject recv) {
        Thread.yield();

        return context.nil;
    }

    @JRubyMethod(meta = true)
    public static RubyArray list(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        RubyThread[] activeThreads = runtime.getThreadService().getActiveRubyThreads();

        return RubyArray.newArrayMayCopy(runtime, activeThreads);
    }

    @JRubyMethod
    public IRubyObject add_trace_func(ThreadContext context, IRubyObject trace_func, Block block) {
        return getContext().addThreadTraceFunction(trace_func, false);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject add_trace_func(ThreadContext context, IRubyObject recv, IRubyObject trace_func, Block block) {
        return context.addThreadTraceFunction(trace_func, false);
    }

    @JRubyMethod
    public IRubyObject set_trace_func(ThreadContext context, IRubyObject trace_func, Block block) {
        if (trace_func.isNil()) return getContext().clearThreadTraceFunctions();

        return getContext().setThreadTraceFunction(trace_func);
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

    private Map<IRubyObject, IRubyObject> getFiberLocals() {
        Map<IRubyObject, IRubyObject> locals = fiberLocalVariables;
        if (locals == null) {
            synchronized (this) {
                locals = fiberLocalVariables;
                if (locals == null) locals = fiberLocalVariables = new HashMap<>();
            }
        }
        return locals;
    }

    // NOTE: all callers are (expected to be) synchronized
    private Map<IRubyObject, IRubyObject> getThreadLocals() {
        return getFiberCurrentThread().getThreadLocals0();
    }

    private Map<IRubyObject, IRubyObject> getThreadLocals0() {
        Map<IRubyObject, IRubyObject> locals = threadLocalVariables;
        if (locals == null) {
            synchronized (this) {
                locals = threadLocalVariables;
                if (locals == null) locals = threadLocalVariables = new HashMap<>();
            }
        }
        return locals;
    }

    /**
     * Clear the fiber local variable storage for this thread.
     * Meant for Java consumers when reusing threads (e.g. during thread pooling).
     * @see #clearThreadLocals()
     */
    public void clearFiberLocals() {
        final Map<IRubyObject, IRubyObject> locals = getFiberLocals();
        synchronized (locals) {
            locals.clear();
        }
    }

    /**
     * Clear the thread local variable storage for this thread.
     * Meant for Java consumers when reusing threads (e.g. during thread pooling).
     * @see #clearFiberLocals()
     */
    public void clearThreadLocals() {
        final Map<IRubyObject, IRubyObject> locals = getThreadLocals();
        synchronized (locals) {
            locals.clear();
        }
    }

    @Override
    public final Map<Object, IRubyObject> getContextVariables() {
        return contextVariables;
    }

    public boolean isAlive(){
        return threadImpl.isAlive() && getStatus() != Status.DEAD;
    }

    public boolean isAdopted() {
        return adopted;
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
        IRubyObject value = op_aref(context, key);

        if (value.isNil()) {
            if (block.isGiven()) return block.yield(context, key);

            throw context.runtime.newKeyError("key not found: " + key.inspect(), this, key);
        }

        return value;
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject key, IRubyObject _default, Block block) {
        final boolean blockGiven = block.isGiven();

        if (blockGiven) {
            context.runtime.getWarnings().warn(ID.BLOCK_BEATS_DEFAULT_VALUE, "block supersedes default value argument");
        }

        IRubyObject value = op_aref(context, key);

        if (value == context.nil) {
            if (blockGiven) return block.yield(context, key);
            return _default;
        }

        return value;
    }

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getFiberLocals();
        synchronized (locals) {
            IRubyObject value;
            return (value = locals.get(key)) == null ? context.nil : value;
        }
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
        if (isFrozen()) throw frozenError(context, this, "can't modify frozen thread locals");

        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getFiberLocals();
        synchronized (locals) {
            locals.put(key, value);
        }
        return value;
    }

    @JRubyMethod(name = "key?")
    public RubyBoolean key_p(ThreadContext context, IRubyObject key) {
        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getFiberLocals();
        synchronized (locals) {
            return asBoolean(context, locals.containsKey(key));
        }
    }

    @JRubyMethod
    public RubyArray keys() {
        final Map<IRubyObject, IRubyObject> locals = getFiberLocals();
        IRubyObject[] ary;
        synchronized (locals) {
            ary = new IRubyObject[locals.size()];
            int i = 0;
            for (Map.Entry<IRubyObject, IRubyObject> entry : locals.entrySet()) {
                ary[i++] = entry.getKey();
            }
        }
        return RubyArray.newArrayMayCopy(getRuntime(), ary);
    }

    @JRubyMethod(name = "thread_variable?")
    public IRubyObject thread_variable_p(ThreadContext context, IRubyObject key) {
        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getThreadLocals();
        synchronized (locals) {
            return asBoolean(context, locals.containsKey(key));
        }
    }

    @JRubyMethod(name = "thread_variable_get")
    public IRubyObject thread_variable_get(ThreadContext context, IRubyObject key) {
        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getThreadLocals();
        synchronized (locals) {
            IRubyObject value;
            return (value = locals.get(key)) == null ? context.nil : value;
        }
    }

    @JRubyMethod(name = "thread_variable_set")
    public IRubyObject thread_variable_set(ThreadContext context, IRubyObject key, IRubyObject value) {
        checkFrozen();
        key = RubySymbol.idSymbolFromObject(context, key);
        final Map<IRubyObject, IRubyObject> locals = getThreadLocals();
        synchronized (locals) {
            locals.put(key, value);
        }
        return value;
    }

    @JRubyMethod(name = "thread_variables")
    public IRubyObject thread_variables(ThreadContext context) {
        final Map<IRubyObject, IRubyObject> locals = getThreadLocals();
        IRubyObject[] ary;
        synchronized (locals) {
            ary = new IRubyObject[locals.size()];
            int i = 0;
            for (Map.Entry<IRubyObject, IRubyObject> entry : locals.entrySet()) {
                ary[i++] = entry.getKey();
            }
        }
        return RubyArray.newArrayMayCopy(context.runtime, ary);
    }

    public boolean isAbortOnException() {
        return abortOnException;
    }

    public void setAbortOnException(final boolean abortOnException) {
        this.abortOnException = abortOnException;
    }

    @JRubyMethod
    public RubyBoolean abort_on_exception(ThreadContext context) {
        return isAbortOnException() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "abort_on_exception=")
    public IRubyObject abort_on_exception_set(IRubyObject val) {
        setAbortOnException(val.isTrue());
        return val;
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    @JRubyMethod(name = "abort_on_exception", meta = true)
    public static RubyBoolean abort_on_exception(ThreadContext context, IRubyObject recv) {
        return context.runtime.isAbortOnException() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "abort_on_exception=", meta = true)
    public static IRubyObject abort_on_exception_set(ThreadContext context, IRubyObject recv, IRubyObject value) {
        context.runtime.setAbortOnException(value.isTrue());
        return value;
    }

    @JRubyMethod(name = "alive?")
    public RubyBoolean alive_p(ThreadContext context) {
        return asBoolean(context, isAlive());
    }

    @JRubyMethod
    public IRubyObject join(ThreadContext context) {
        long timeoutMillis = Long.MAX_VALUE;

        return joinCommon(context, timeoutMillis);
    }

    @JRubyMethod
    public IRubyObject join(ThreadContext context, IRubyObject timeout) {
        long timeoutMillis = Long.MAX_VALUE;

        if (!timeout.isNil()) {
            // MRI behavior: value given in seconds; converted to Float; less
            // than or equal to zero returns immediately; returns nil
            timeoutMillis = (long) (1000 * RubyNumeric.num2dbl(timeout));
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

        return joinCommon(context, timeoutMillis);
    }

    private IRubyObject joinCommon(ThreadContext context, long timeoutMillis) {
        Ruby runtime = context.runtime;
        if (isCurrent()) {
            throw runtime.newThreadError("Target thread must not be current thread");
        }

        RubyThread currentThread = context.getThread();

        try {
            currentThread.enterSleep();

            final long timeToWait = Math.min(timeoutMillis, 200);

            // We need this loop in order to be able to "unblock" the
            // join call without actually calling interrupt.
            long start = System.currentTimeMillis();
            while (true) {
                currentThread.blockingThreadPoll(context);
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

        final Throwable exception = this.exitingException;
        if (exception != null) {
            if (exception instanceof RaiseException) {
                // Set $! in the current thread before exiting
                runtime.getGlobalVariables().set("$!", ((RaiseException) exception).getException());
            } else {
                runtime.getGlobalVariables().set("$!", JavaUtil.convertJavaToUsableRubyObject(runtime, exception));
            }
            Helpers.throwException(exception);
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
    public IRubyObject value(ThreadContext context) {
        join(context);
        synchronized (this) {
            return finalResult;
        }
    }

    @JRubyMethod
    public IRubyObject group() {
        final RubyThreadGroup group = this.threadGroup;
        return group == null ? getRuntime().getNil() : group;
    }

    void setThreadGroup(RubyThreadGroup rubyThreadGroup) {
        threadGroup = rubyThreadGroup;
    }

    @Override
    public IRubyObject inspect() {
        return inspect(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod(name = { "inspect", "to_s"})
    public RubyString inspect(ThreadContext context) {
        RubyString result = newString(context, "#<");

        result.cat(getMetaClass().getRealClass().toRubyString(context));
        result.cat(':');
        result.catString(identityString());
        synchronized (this) {
            String id = threadImpl.getRubyName(); // thread.name
            if (notEmpty(id)) {
                result.cat('@');
                result.cat(asSymbol(context, id).getBytes());
            }
            if (notEmpty(file) && line >= 0) {
                result.cat(' ');
                result.catString(file);
                result.cat(':');
                result.catString(Integer.toString(line + 1));
            }
            result.cat(' ');
            result.catString(getStatusName(context));
            result.cat('>');
            return result;
        }
    }

    private static boolean notEmpty(String str) {
        return str != null && str.length() > 0;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject stop(ThreadContext context, IRubyObject receiver) {
        RubyThread rubyThread = context.getThread();

        if (context.runtime.getThreadService().getActiveRubyThreads().length == 1) {
            throw context.runtime.newThreadError("stopping only thread\n\tnote: use sleep to stop forever");
        }

        synchronized (rubyThread) {
            rubyThread.blockingThreadPoll(context);
            Status oldStatus = rubyThread.getStatus();
            try {
                STATUS.set(rubyThread, Status.SLEEP);
                rubyThread.wait();
            } catch (InterruptedException ie) {
            } finally {
                rubyThread.pollThreadEvents(context);
                STATUS.set(rubyThread, oldStatus);
            }
        }

        return context.nil;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject kill(IRubyObject recv, IRubyObject rubyThread, Block block) {
        if (!(rubyThread instanceof RubyThread)) throw typeError(recv.getRuntime().getCurrentContext(), rubyThread, "Thread");
        return ((RubyThread)rubyThread).kill();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject exit(IRubyObject receiver, Block block) {
        RubyThread rubyThread = receiver.getRuntime().getThreadService().getCurrentContext().getThread();

        return rubyThread.kill();
    }

    @JRubyMethod(name = "stop?")
    public RubyBoolean stop_p(ThreadContext context) {
        // not valid for "dead" state
        return asBoolean(context, getStatus() == Status.SLEEP || getStatus() == Status.DEAD);
    }

    @Deprecated
    public RubyBoolean stop_p() {
        return stop_p(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public synchronized RubyThread wakeup() {
        if(!threadImpl.isAlive() && getStatus() == Status.DEAD) {
            throw getRuntime().newThreadError("killed thread");
        }

        STATUS.set(this, Status.RUN);
        interrupt();

        return this;
    }

    @JRubyMethod
    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuntime(), javaPriorityToRubyPriority(threadImpl.getPriority()));
    }

    @JRubyMethod(name = "priority=")
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
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return genericRaise(context, context.getThread(), exception);
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
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return genericRaise(context, context.getThread(), exception, message);
    }

    @JRubyMethod(optional = 3, checkArity = false)
    public IRubyObject raise(ThreadContext context, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 0, 3);

        return genericRaise(context, context.getThread(), args);
    }

    @JRubyMethod
    public IRubyObject native_thread_id(ThreadContext context) {
        if (!isAlive()) return context.nil;

        String encodedString = ManagementFactory.getRuntimeMXBean().getName();
        int atIndex = encodedString.indexOf('@');

        // Undocumented format: 1761769@localhost.localdomain
        if (atIndex != -1) {
            try {
                int id = Integer.parseInt(encodedString.substring(0, atIndex));

                return asFixnum(context, id);
            } catch (NumberFormatException e) {
                // if we fail to parse this we will just act like we don't support it
            }
        }

        return context.nil;  // Not supported or failed to extract id
    }

    private IRubyObject genericRaise(ThreadContext context, RubyThread currentThread, IRubyObject... args) {
        if (!isAlive()) return context.nil;

        pendingInterruptEnqueue(prepareRaiseException(context, args));
        interrupt();

        if (currentThread == this) {
            executeInterrupts(context, false);
        }

        return context.nil;
    }

    public static IRubyObject prepareRaiseException(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        IRubyObject errorInfo = context.getErrorInfo();

        if (args.length == 0) {
            if (errorInfo.isNil()) {
                // We force RaiseException here to populate backtrace
                return RaiseException.from(runtime, runtime.getRuntimeError(), "").getException();
            }
            return errorInfo;
        }

        final IRubyObject arg = args[0];

        IRubyObject tmp;
        final RubyException exception;
        if (args.length == 1) {
            if (arg instanceof RubyString) {
                tmp = runtime.getRuntimeError().newInstance(context, args, Block.NULL_BLOCK);
            } else if (arg instanceof ConcreteJavaProxy ) {
                return arg;
            } else {
                if (!arg.respondsTo("exception")) throw typeError(context, "exception class/object expected");
                tmp = arg.callMethod(context, "exception");
            }
        } else {
            if (!arg.respondsTo("exception")) throw typeError(context, "exception class/object expected");
            tmp = arg.callMethod(context, "exception", args[1]);
        }

        if (!runtime.getException().isInstance(tmp)) throw typeError(context, "exception object expected");

        exception = (RubyException) tmp;

        if (args.length == 3) {
            exception.set_backtrace(args[2]);
        }

        IRubyObject cause = errorInfo;
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
     * Sleep the current thread for milliseconds, waking up on any thread interrupts.
     *
     * @param milliseconds Number of milliseconds to sleep. Zero sleeps forever.
     */
    public boolean sleep(long milliseconds) throws InterruptedException {
        return sleep(milliseconds, 0);
    }

    /**
     * Sleep the current thread for milliseconds + nanoseconds, waking up on any thread interrupts.
     *
     * We can never be sure if a wait will finish because of a Java "spurious wakeup".  So if we
     * explicitly wakeup and we wait less than requested amount we will return false.  We will
     * return true if we sleep right amount or less than right amount via spurious wakeup.
     *
     * @param milliseconds Number of milliseconds to sleep. Combined with nanoseconds, zero sleeps forever.
     * @param nanoseconds Number of nanoseconds to sleep. Combined with milliseconds, zero sleeps forever.
     */
    public boolean sleep(long milliseconds, long nanoseconds) throws InterruptedException {
        assert this == getRuntime().getCurrentContext().getThread();
        sleepTask.nanoseconds = nanoseconds + TimeUnit.MILLISECONDS.toNanos(milliseconds);
        try {
            long timeSlept = executeTaskBlocking(getContext(), null, sleepTask);
            if (sleepTask.nanoseconds == 0 || timeSlept >= sleepTask.nanoseconds) {
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

    public IRubyObject status() { // not used
        return status(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject status(ThreadContext context) {
        if (status == Status.DEAD) {
            return exitingException != null ? context.nil : context.fals;
        }

        return Create.newString(context, getStatusName(context));
    }

    private String getStatusName(ThreadContext context) {
        Ruby runtime = context.runtime;
        final Status status = getStatus();

        switch (status) {
            case RUN:
                if (killed) {
                    return "aborting";
                }
                // fall through
            default:
                return status.name().toLowerCase();
        }
    }

    @JRubyMethod(meta = true, omit = true)
    public static IRubyObject each_caller_location(ThreadContext context, IRubyObject recv, Block block) {
        ThreadContext.WALKER.walk(stream -> {
            boolean[] skip = {true};
            context.eachCallerLocation(stream, (loc) -> {
                if (skip[0]) {
                    skip[0] = false;
                    return;
                }
                block.yieldSpecific(context, loc);
            });
            return null;
        });
        return context.nil;
    }

    /**
     * @return existing exception or null if terminated normally
     */
    public Throwable getExitingException() {
        return exitingException;
    }

    @Deprecated
    public interface BlockingTask {
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
        long nanoseconds;
        {semaphore.drainPermits();}

        @Override
        public Long run(ThreadContext context, Object data) throws InterruptedException {
            long start = System.nanoTime();

            try {
                if (nanoseconds == 0) {
                    semaphore.tryAcquire(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } else {
                    semaphore.tryAcquire(nanoseconds, TimeUnit.NANOSECONDS);
                }

                return System.nanoTime() - start;
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

    public <Data, Return> Return executeTaskBlocking(ThreadContext context, Data data, Task<Data, Return> task) throws InterruptedException {
        return executeTask(context, data, Status.SLEEP, task, true);
    }

    public <Data, Return> Return executeTask(ThreadContext context, Data data, Task<Data, Return> task) throws InterruptedException {
        return executeTask(context, data, Status.SLEEP, task, false);
    }

    public <Data, Return> Return executeTaskBlocking(ThreadContext context, Data data, Status status, Task<Data, Return> task) throws InterruptedException {
        return executeTask(context, data, status, task, true);
    }

    public <Data, Return> Return executeTask(ThreadContext context, Data data, Status status, Task<Data, Return> task) throws InterruptedException {
        return executeTask(context, data, status, task, false);
    }

    private <Data, Return> Return executeTask(ThreadContext context, Data data, Status status, Task<Data, Return> task, boolean blocking) throws InterruptedException {
        Status oldStatus = STATUS.get(this);
        try {
            this.unblockArg = data;
            this.unblockFunc = task;

            // check for interrupt before going into blocking call
            pollThreadEvents(context, blocking);

            STATUS.set(this, status);

            return task.run(context, data);
        } finally {
            STATUS.set(this, oldStatus);
            this.unblockFunc = null;
            this.unblockArg = null;
            pollThreadEvents(context, blocking);
        }
    }

    /**
     * Execute an interruptible read or write operation with the given byte range and data object.
     *
     * @param context the current context
     * @param data a data object
     * @param bytes the bytes to write
     * @param start start range of bytes to write
     * @param length length of bytes to write
     * @param task the write task
     * @param <Data> the type of the data object
     * @return the number of bytes written
     * @throws InterruptedException
     */
    public <Data> int executeReadWrite(
            ThreadContext context,
            Data data, byte[] bytes, int start, int length,
            ReadWrite<Data> task) throws InterruptedException {
        Status oldStatus = STATUS.get(this);
        try {
            preReadWrite(context, data, task);

            return task.run(context, data, bytes, start, length);
        } finally {
            postReadWrite(context, oldStatus);
        }
    }

    public <Data> int executeReadWrite(
            ThreadContext context,
            Data data, ByteBuffer bytes, int start, int length,
            ReadWrite<Data> task) throws InterruptedException {
        Status oldStatus = STATUS.get(this);
        try {
            preReadWrite(context, data, task);

            return task.run(context, data, bytes, start, length);
        } finally {
            postReadWrite(context, oldStatus);
        }
    }

    private void postReadWrite(ThreadContext context, Status oldStatus) {
        STATUS.set(this, oldStatus);
        this.unblockFunc = null;
        this.unblockArg = null;
        pollThreadEvents(context);
    }

    private <Data> void preReadWrite(ThreadContext context, Data data, ReadWrite<Data> task) {
        this.unblockArg = data;
        this.unblockFunc = task;

        // check for interrupt before going into blocking call
        blockingThreadPoll(context);

        STATUS.set(this, Status.SLEEP);
    }

    public interface ReadWrite<Data> extends Unblocker<Data> {
        /**
         * @deprecated Prefer version that receives ByteBuffer rather than recreating every time.
         */
        @Deprecated(since = "9.4-")
        public default int run(ThreadContext context, Data data, byte[] bytes, int start, int length) throws InterruptedException {
            return run(context, data, ByteBuffer.wrap(bytes), start, length);
        }
        public int run(ThreadContext context, Data data, ByteBuffer bytes, int start, int length) throws InterruptedException;
        public void wakeup(RubyThread thread, Data data);
    }

    /**
     * Execute an interruptible regexp operation with the given function and bytess.
     * @param context
     * @param matcher
     * @param start
     * @param range
     * @param option
     * @param task
     * @return ""
     * @param <Data>
     * @throws InterruptedException when interrupted
     */
    public <Data> int executeRegexp(
            ThreadContext context,
            Matcher matcher, int start, int range, int option,
            RegexMatch task) throws InterruptedException {
        Status oldStatus = STATUS.get(this);
        try {
            this.unblockArg = matcher;
            this.unblockFunc = task;

            // check for interrupt before going into blocking call
            blockingThreadPoll(context);

            STATUS.set(this, Status.SLEEP);

            return task.run(matcher, start, range, option);
        } finally {
            STATUS.set(this, oldStatus);
            this.unblockFunc = null;
            this.unblockArg = null;
            pollThreadEvents(context);
        }
    }

    public interface RegexMatch extends Unblocker<Matcher> {
        public int run(Matcher matcher, int start, int range, int option) throws InterruptedException;
        public default void wakeup(RubyThread thread, Matcher matcher) {
            thread.getNativeThread().interrupt();
        }
    }

    public void enterSleep() {
        STATUS.set(this, Status.SLEEP);
    }

    public void exitSleep() {
        STATUS.set(this, Status.RUN);
    }

    private Status getStatus() {
        Status status = STATUS.get(this);

        if (status != Status.NATIVE) return status;

        return nativeStatus();
    }

    private Status nativeStatus() {
        switch (getNativeThread().getState()) {
            case NEW:
            case RUNNABLE:
            default:
                return Status.RUN;
            case BLOCKED:
            case WAITING:
            case TIMED_WAITING:
                return Status.SLEEP;
            case TERMINATED:
                return Status.DEAD;
        }
    }

    @JRubyMethod(name = {"kill", "exit", "terminate"})
    public IRubyObject kill() {
        Ruby runtime = getRuntime();

        if (killed == true || status == Status.DEAD) {
            return this;
        }

        ThreadContext context = runtime.getCurrentContext();
        RubyThread currentThread = context.getThread();

        if (this == runtime.getThreadService().getMainThread()) {
            RubyKernel.exit(context, runtime.getKernel(), Helpers.arrayOf(RubyFixnum.zero(runtime)));
        }

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

        getRuntime().getCheckpointInvalidator().invalidate();
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

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context) {
        return backtrace(context, null, null);
    }

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context, IRubyObject level) {
        return backtrace(context, level, null);
    }

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context, IRubyObject level, IRubyObject length) {
        ThreadContext selfContext = getContext();
        Thread nativeThread = getNativeThread();

        // context can be nil if we have not started or GC has claimed our context
        // nativeThread can be null if the thread has terminated and GC has claimed it
        // nativeThread may have finished
        if (selfContext == null || nativeThread == null || !nativeThread.isAlive()) return context.nil;

        return RubyKernel.withLevelAndLength(
                selfContext, level, length, 0,
                (ctx, lev, len) -> WALKER.walk(getNativeThread().getStackTrace(), stream -> ctx.createCallerBacktrace(lev, len, stream)));
    }

    @JRubyMethod
    public IRubyObject backtrace_locations(ThreadContext context) {
        return backtrace_locations(context, null, null);
    }

    @JRubyMethod
    public IRubyObject backtrace_locations(ThreadContext context, IRubyObject level) {
        return backtrace_locations(context, level, null);
    }

    @JRubyMethod
    public IRubyObject backtrace_locations(ThreadContext context, IRubyObject level, IRubyObject length) {
        ThreadContext selfContext = getContext();
        Thread nativeThread = getNativeThread();

        // context can be nil if we have not started or GC has claimed our context
        // nativeThread can be null if the thread has terminated and GC has claimed it
        // nativeThread may have finished
        if (selfContext == null || nativeThread == null || !nativeThread.isAlive()) return context.nil;

        return RubyKernel.withLevelAndLength(
                selfContext, level, length, 0,
                (ctx, lev, len) -> WALKER.walk(getNativeThread().getStackTrace(), stream -> ctx.createCallerLocations(lev, len, stream)));
    }

    public boolean isReportOnException() {
        return reportOnException;
    }

    public void setReportOnException(final boolean reportOnException) {
        this.reportOnException = reportOnException;
    }

    @JRubyMethod(name = "report_on_exception=")
    public IRubyObject report_on_exception_set(ThreadContext context, IRubyObject state) {
        setReportOnException(state.isTrue());
        return state;
    }

    @JRubyMethod(name = "report_on_exception")
    public IRubyObject report_on_exception(ThreadContext context) {
        return isReportOnException() ? context.tru : context.fals;
    }

    @JRubyMethod(name = "report_on_exception=", meta = true)
    public static IRubyObject report_on_exception_set(ThreadContext context, IRubyObject self, IRubyObject state) {
        context.runtime.setReportOnException(state.isNil() ? context.nil : asBoolean(context, state.isTrue()));
        return state;
    }

    @JRubyMethod(name = "report_on_exception", meta = true)
    public static IRubyObject report_on_exception(ThreadContext context, IRubyObject self) {
        return asBoolean(context, context.runtime.isReportOnException());
    }

    public StackTraceElement[] javaBacktrace() {
        return threadImpl.getStackTrace();
    }

    private boolean isCurrent() {
        return threadImpl.isCurrent();
    }

    public void exceptionRaised(RaiseException exception) {
        exceptionRaised((Throwable) exception);
    }

    protected void printReportExceptionWarning() {
        Ruby runtime = getRuntime();
        String name = threadImpl.getReportName();
        String warning = "warning: thread \"" + name + "\" terminated with exception (report_on_exception is true):";

        runtime.printErrorString(warning);
    }

    /**
     * For handling all exceptions bubbling out of threads
     * @param throwable
     */
    public void exceptionRaised(Throwable throwable) {
        assert isCurrent();

        final Ruby runtime = getRuntime();

        if (throwable instanceof Error || throwable instanceof MainExitException) {
            exitingException = throwable;
            Helpers.throwException(throwable);
            return;
        }

        // if unrescuable (internal exceptions) just re-raise and let it be handled by thread handler
        if (throwable instanceof Unrescuable) {
            Helpers.throwException(throwable);
            return;
        }

        final IRubyObject rubyException;
        if (throwable instanceof RaiseException) {
            RaiseException exception = (RaiseException) throwable;
            rubyException = exception.getException();
        } else {
            rubyException = JavaUtil.convertJavaToUsableRubyObject(runtime, throwable);
        }

        boolean report;
        if (runtime.getSystemExit().isInstance(rubyException)) {
            runtime.getThreadService().getMainThread().raise(rubyException);
        } else if ((report = reportOnException) || abortOnException(runtime)) {
            if (report) {
                printReportExceptionWarning();
                runtime.printError(throwable);
            }
            if (abortOnException(runtime)) {
                runtime.getThreadService().getMainThread().raise(rubyException);
            }
        } else if (runtime.isDebug()) {
            runtime.printError(throwable);
        }

        exitingException = throwable;
    }

    private boolean abortOnException(Ruby runtime) {
        return (runtime.isAbortOnException() || abortOnException);
    }

    public static RubyThread mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }

    /**
     * Perform an interruptible select operation on the given channel and fptr,
     * waiting for the requested operations or the given timeout.
     *
     * @param io the RubyIO that contains the channel, for managing blocked threads list.
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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
     * @param ops the operations to wait for, from {@link java.nio.channels.SelectionKey}.
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

            // ensure we have fptr locked, but release it to avoid deadlock
            boolean locked = false;
            if (fptr != null) {
                locked = fptr.lock();
                fptr.unlock();
            }
            try {
                synchronized (selectable.blockingLock()) {
                    boolean oldBlocking = selectable.isBlocking();

                    SelectionKey key;
                    try {
                        selectable.configureBlocking(false);

                        if (fptr != null) fptr.addBlockingThread(this);
                        currentSelector = getRuntime().getSelectorPool().get(selectable.provider());

                        key = selectable.register(currentSelector, ops);

                        beforeBlockingCall(getContext());
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

                            if (keySet.contains(key) && key.isValid()) {
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
            } finally {
                if (fptr != null) {
                    fptr.lock();
                    if (locked) fptr.unlock();
                }
            }
        } else {
            // can't select, just have to do a blocking call
            return true;
        }
    }

    @SuppressWarnings("deprecation")
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

    /**
     * Set the pending interrupt flag.
     *
     * CRuby: RB_VM_SET_INTERRUPT/
     */
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
        if (!(channel instanceof SelectableChannel)) return true;

        try {
            io.addBlockingThread(this);
            blockingIO = BlockingIO.newCondition(channel, ops);
            boolean ready = blockingIO.await();

            // check for thread events, in case we've been woken up to die
            blockingThreadPoll(context);
            return ready;
        } catch (IOException ioe) {
            throw runtimeError(context, "Error with selector: " + ioe);
        } catch (InterruptedException ex) {
            throw runtimeError(context, "Interrupted"); // FIXME: not correct exception
        } finally {
            blockingIO = null;
            io.removeBlockingThread(this);
        }
    }
    public void beforeBlockingCall(ThreadContext context) {
        blockingThreadPoll(context);
        enterSleep();
    }

    @Deprecated
    public void beforeBlockingCall() {
        beforeBlockingCall(metaClass.runtime.getCurrentContext());
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
        return 97 * (3 + (this.threadImpl != ThreadLike.DUMMY ? this.threadImpl.hashCode() : 0));
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
        executeTaskBlocking(getContext(), lock, new RubyThread.Task<Lock, Object>() {
            @Override
            public Object run(ThreadContext context, Lock reentrantLock) throws InterruptedException {
                reentrantLock.lockInterruptibly();
                heldLocks.add(lock);
                return reentrantLock;
            }

            @Override
            public void wakeup(RubyThread thread, Lock reentrantLock) {
                thread.getNativeThread().interrupt();
            }
        });
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
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException imse) {
                // don't allow a bad lock to prevent others from unlocking
                getRuntime().getWarnings().warn("BUG: attempted to unlock a non-acquired lock " + lock + " in thread " + toString());
            }
        }
    }

    /**
     * Release lock and sleep.
     */
    public void sleep(Lock lock) throws InterruptedException {
        sleep(lock, 0);
    }

    /**
     * Release lock and sleep for the specified number of milliseconds.
     */
    public void sleep(Lock lock, long millis) throws InterruptedException {
        assert Thread.currentThread() == getNativeThread();
        executeTaskBlocking(getContext(), lock.newCondition(), Status.NATIVE, new Task<Condition, Object>() {
            @Override
            public Object run(ThreadContext context, Condition condition) throws InterruptedException {
                if (millis == 0) {
                    condition.await();
                } else {
                    condition.await(millis, TimeUnit.MILLISECONDS);
                }
                return null;
            }

            @Override
            public void wakeup(RubyThread thread, Condition condition) {
                thread.getNativeThread().interrupt();
            }
        });
    }

    private String identityString() {
        return "0x" + Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Customized for retrieving a Java thread from a Ruby one.
     *
     * @param target The target type to which the object should be converted (e.g. <code>java.lang.Thread.class</code>).
     * @since 9.4
     */
    @Override
    public <T> T toJava(final Class<T> target) {
        // since 9.4 due compatibility with JRuby <= 9.3, hopefully to be removed in 9.5
        if (target == Object.class) {
            // NOTE: a deprecation wasn't introduced in 9.4 simply due the need to cleanup libraries first and their
            // Thread.current.to_java.getNativeThread/getContext usage, in 9.5 a deprecation should be added here
            return super.toJava(target); // simply returns the (internal) org.jruby.RubyThread
        }

        if (target.isAssignableFrom(Thread.class)) { // Thread | Runnable | Object
            return target.cast(getNativeThread());
        }
        return super.toJava(target);
    }

    /**
     * Run the provided {@link BiFunction} without allowing for any cross-thread interrupts (equivalent to calling
     * {@link #handle_interrupt(ThreadContext, IRubyObject, IRubyObject, Block)} with Object =&gt; :never.
     *
     * MRI: rb_uninterruptible
     *
     * @param context the current context
     * @param f the bifunction to execute
     * @return return value of f.apply.
     */
    public static <StateType> IRubyObject uninterruptible(ThreadContext context, StateType state, BiFunction<ThreadContext, StateType, IRubyObject> f) {
        return context.getThread().handleInterrupt(
                context,
                RubyHash.newHash(context.runtime, objectClass(context), asSymbol(context, "never")),
                state,
                f);
    }

    /**
     * Set the scheduler for the current thread.
     *
     * MRI: rb_fiber_scheduler_set
     */
    public IRubyObject setFiberScheduler(ThreadContext context, IRubyObject scheduler) {
//        VM_ASSERT(ruby_thread_has_gvl_p());

        Objects.requireNonNull(scheduler);

        if (scheduler != null && !scheduler.isNil()) FiberScheduler.verifyInterface(context, scheduler);
        if (!this.scheduler.isNil()) FiberScheduler.close(getContext(), this.scheduler);

        this.scheduler = scheduler;

        return scheduler;
    }

    public IRubyObject getScheduler() {
        return scheduler;
    }

    // MRI: rb_fiber_scheduler_current_for_threadptr, rb_fiber_scheduler_current
    public IRubyObject getSchedulerCurrent() {
        if (!isBlocking()) {
            return scheduler;
        }

        return getRuntime().getNil();
    }

    public void incrementBlocking() {
        blockingCount++;
    }

    public void decrementBlocking() {
        blockingCount--;
    }

    public boolean isBlocking() {
        return blockingCount > 0;
    }

    @Deprecated
    public IRubyObject pending_interrupt_p(ThreadContext context, IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> pending_interrupt_p(context);
            case 1 -> pending_interrupt_p(context, args[0]);
            default -> throw argumentError(context, args.length, 0, 1);
        };
    }

    @Deprecated
    public static IRubyObject pending_interrupt_p(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return context.getThread().pending_interrupt_p(context, args);
    }

    @Deprecated
    public RubyBoolean alive_p() {
        return isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @Deprecated
    public IRubyObject value() {
        return value(getCurrentContext());
    }

    @Deprecated
    public IRubyObject join(ThreadContext context, IRubyObject[] args) {
        return switch (args.length) {
            case 0 -> join(context);
            case 1 -> join(context, args[0]);
            default -> throw argumentError(context, args.length, 0, 1);
        };
    }

    @Deprecated
    public IRubyObject op_aset(IRubyObject key, IRubyObject value) {
        return op_aset(getRuntime().getCurrentContext(), key, value);
    }
}
