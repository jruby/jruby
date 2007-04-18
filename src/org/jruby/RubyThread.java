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

import java.util.HashMap;
import java.util.Map;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.internal.runtime.FutureThread;
import org.jruby.internal.runtime.NativeThread;
import org.jruby.internal.runtime.RubyNativeThread;
import org.jruby.internal.runtime.RubyRunnable;
import org.jruby.internal.runtime.ThreadLike;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.jruby.runtime.Arity;

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
 *
 * @author Jason Voegele (jason@jvoegele.com)
 */
public class RubyThread extends RubyObject {
    private ThreadLike threadImpl;
    private Map threadLocalVariables = new HashMap();
    private boolean abortOnException;
    private IRubyObject finalResult;
    private RaiseException exitingException;
    private IRubyObject receivedException;
    private RubyThreadGroup threadGroup;

    private ThreadService threadService;
    private boolean hasStarted = false;
    private volatile boolean isStopped = false;
    public Object stopLock = new Object();
    
    private volatile boolean killed = false;
    public Object killLock = new Object();
    private RubyThread joinedByCriticalThread;
      
     public final ReentrantLock lock = new ReentrantLock();
     private volatile boolean critical = false;
     
      private static boolean USE_POOLING;
      
     private static final boolean DEBUG = false;
    
   static {
       if (Ruby.isSecurityRestricted()) USE_POOLING = false;
       else USE_POOLING = Boolean.getBoolean("jruby.thread.pooling");
   }
   
    public static RubyClass createThreadClass(Ruby runtime) {
        // FIXME: In order for Thread to play well with the standard 'new' behavior,
        // it must provide an allocator that can create empty object instances which
        // initialize then fills with appropriate data.
        RubyClass threadClass = runtime.defineClass("Thread", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyThread.class);

        threadClass.defineFastMethod("[]", callbackFactory.getFastMethod("aref", RubyKernel.IRUBY_OBJECT));
        threadClass.defineFastMethod("[]=", callbackFactory.getFastMethod("aset", RubyKernel.IRUBY_OBJECT, RubyKernel.IRUBY_OBJECT));
        threadClass.defineFastMethod("abort_on_exception", callbackFactory.getFastMethod("abort_on_exception"));
        threadClass.defineFastMethod("abort_on_exception=", callbackFactory.getFastMethod("abort_on_exception_set", RubyKernel.IRUBY_OBJECT));
        threadClass.defineFastMethod("alive?", callbackFactory.getFastMethod("is_alive"));
        threadClass.defineFastMethod("group", callbackFactory.getFastMethod("group"));
        threadClass.defineFastMethod("join", callbackFactory.getFastOptMethod("join"));
        threadClass.defineFastMethod("value", callbackFactory.getFastMethod("value"));
        threadClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        threadClass.defineFastMethod("key?", callbackFactory.getFastMethod("has_key", RubyKernel.IRUBY_OBJECT));
        threadClass.defineFastMethod("keys", callbackFactory.getFastMethod("keys"));
        threadClass.defineFastMethod("priority", callbackFactory.getFastMethod("priority"));
        threadClass.defineFastMethod("priority=", callbackFactory.getFastMethod("priority_set", RubyKernel.IRUBY_OBJECT));
        threadClass.defineMethod("raise", callbackFactory.getOptMethod("raise"));
        //threadClass.defineFastMethod("raise", callbackFactory.getFastMethod("raise", RubyKernel.IRUBY_OBJECT));
        threadClass.defineFastMethod("run", callbackFactory.getFastMethod("run"));
        threadClass.defineFastMethod("status", callbackFactory.getFastMethod("status"));
        threadClass.defineFastMethod("stop?", callbackFactory.getFastMethod("isStopped"));
        threadClass.defineFastMethod("wakeup", callbackFactory.getFastMethod("wakeup"));
        //        threadClass.defineMethod("value", 
        //                callbackFactory.getMethod("value"));
        threadClass.defineFastMethod("kill", callbackFactory.getFastMethod("kill"));
        threadClass.defineFastMethod("exit", callbackFactory.getFastMethod("exit"));
        
        threadClass.getMetaClass().defineFastMethod("current", callbackFactory.getFastSingletonMethod("current"));
        threadClass.getMetaClass().defineMethod("fork", callbackFactory.getOptSingletonMethod("newInstance"));
        threadClass.getMetaClass().defineMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        threadClass.getMetaClass().defineFastMethod("list", callbackFactory.getFastSingletonMethod("list"));
        threadClass.getMetaClass().defineFastMethod("pass", callbackFactory.getFastSingletonMethod("pass"));
        threadClass.getMetaClass().defineMethod("start", callbackFactory.getOptSingletonMethod("start"));
        threadClass.getMetaClass().defineFastMethod("critical=", callbackFactory.getFastSingletonMethod("critical_set", RubyBoolean.class));
        threadClass.getMetaClass().defineFastMethod("critical", callbackFactory.getFastSingletonMethod("critical"));
        threadClass.getMetaClass().defineFastMethod("stop", callbackFactory.getFastSingletonMethod("stop"));
        threadClass.getMetaClass().defineMethod("kill", callbackFactory.getSingletonMethod("s_kill", RubyThread.class));
        threadClass.getMetaClass().defineMethod("exit", callbackFactory.getSingletonMethod("s_exit"));
        threadClass.getMetaClass().defineFastMethod("abort_on_exception", callbackFactory.getFastSingletonMethod("abort_on_exception"));
        threadClass.getMetaClass().defineFastMethod("abort_on_exception=", callbackFactory.getFastSingletonMethod("abort_on_exception_set", RubyKernel.IRUBY_OBJECT));

        RubyThread rubyThread = new RubyThread(runtime, threadClass);
        // set hasStarted to true, otherwise Thread.main.status freezes
        rubyThread.hasStarted = true;
        // TODO: need to isolate the "current" thread from class creation
        rubyThread.threadImpl = new NativeThread(rubyThread, Thread.currentThread());
        runtime.getThreadService().setMainThread(rubyThread);
        
        threadClass.getMetaClass().defineFastMethod("main", callbackFactory.getFastSingletonMethod("main"));
        
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
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        return startThread(recv, args, true, block);
    }

    /**
     * Basically the same as Thread.new . However, if class Thread is
     * subclassed, then calling start in that subclass will not invoke the
     * subclass's initialize method.
     */
    public static RubyThread start(IRubyObject recv, IRubyObject[] args, Block block) {
        return startThread(recv, args, false, block);
    }
    
    public static RubyThread adopt(IRubyObject recv, Thread t) {
        return adoptThread(recv, t, Block.NULL_BLOCK);
    }

    private static RubyThread adoptThread(final IRubyObject recv, Thread t, Block block) {
        final Ruby runtime = recv.getRuntime();
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) recv);
        
        rubyThread.threadImpl = new NativeThread(rubyThread, t);
        runtime.getThreadService().registerNewThread(rubyThread);
        
        runtime.getCurrentContext().preAdoptThread();
        
        rubyThread.callInit(IRubyObject.NULL_ARRAY, block);
        rubyThread.hasStarted = true;
        
        return rubyThread;
    }

    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit, Block block) {
        if (!block.isGiven()) throw recv.getRuntime().newThreadError("must be called with a block");

        RubyThread rubyThread = new RubyThread(recv.getRuntime(), (RubyClass) recv);
        
        if (callInit) rubyThread.callInit(IRubyObject.NULL_ARRAY, block);

        if (USE_POOLING) {
            rubyThread.threadImpl = new FutureThread(rubyThread, new RubyRunnable(rubyThread, args, block));
        } else {
            rubyThread.threadImpl = new NativeThread(rubyThread, new RubyNativeThread(rubyThread, args, block));
        }
        rubyThread.threadImpl.start();
        rubyThread.hasStarted = true;
        
        return rubyThread;
    }
    
    private void ensureCurrent() {
        if (this != getRuntime().getCurrentContext().getThread()) {
            throw new RuntimeException("internal thread method called from another thread");
        }
    }
    
    private void ensureNotCurrent() {
        if (this == getRuntime().getCurrentContext().getThread()) {
            throw new RuntimeException("internal thread method called from another thread");
        }
    }
    
    public void cleanTerminate(IRubyObject result) {
        finalResult = result;
        isStopped = true;
    }

    public void pollThreadEvents() {
        try {
            // check for criticalization *before* locking ourselves
            threadService.waitForCritical();
            
            while (!this.lock.tryLock());
            
            ensureCurrent();
            
            if (DEBUG) System.out.println("thread " + Thread.currentThread() + " before");
            if (killed) throw new ThreadKill();
            
            if (DEBUG) System.out.println("thread " + Thread.currentThread() + " after");
            if (receivedException != null) {
                // clear this so we don't keep re-throwing
                IRubyObject raiseException = receivedException;
                receivedException = null;
                RubyModule kernelModule = getRuntime().getModule("Kernel");
                if (DEBUG) System.out.println("thread " + Thread.currentThread() + " before propagating exception: " + killed);
                kernelModule.callMethod(getRuntime().getCurrentContext(), "raise", raiseException);
            }
            
        } finally {
            if (this.lock.isHeldByCurrentThread()) this.lock.unlock();
        }
    }

    private RubyThread(Ruby runtime, RubyClass type) {
        super(runtime, type);
        this.threadService = runtime.getThreadService();
        // set to default thread group
        RubyThreadGroup defaultThreadGroup = (RubyThreadGroup)runtime.getClass("ThreadGroup").getConstant("Default");
        defaultThreadGroup.add(this, Block.NULL_BLOCK);
        finalResult = runtime.getNil();
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    public static RubyBoolean abort_on_exception(IRubyObject recv) {
    	Ruby runtime = recv.getRuntime();
        return runtime.isGlobalAbortOnExceptionEnabled() ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static IRubyObject abort_on_exception_set(IRubyObject recv, IRubyObject value) {
        recv.getRuntime().setGlobalAbortOnExceptionEnabled(value.isTrue());
        return value;
    }

    public static RubyThread current(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getThread();
    }

    public static RubyThread main(IRubyObject recv) {
        return recv.getRuntime().getThreadService().getMainThread();
    }

    public static IRubyObject pass(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        ThreadService ts = runtime.getThreadService();
        boolean critical = ts.getCritical();
        RubyThread currentThread = ts.getCurrentContext().getThread();
        
        ts.setCritical(false);
        
        Thread.yield();
        
        ts.setCritical(critical);
        
        return recv.getRuntime().getNil();
    }

    public static RubyArray list(IRubyObject recv) {
    	RubyThread[] activeThreads = recv.getRuntime().getThreadService().getActiveRubyThreads();
        
        return recv.getRuntime().newArrayNoCopy(activeThreads);
    }
    
    private IRubyObject getSymbolKey(IRubyObject originalKey) {
        if (originalKey instanceof RubySymbol) {
            return originalKey;
        } else if (originalKey instanceof RubyString) {
            return RubySymbol.newSymbol(getRuntime(), originalKey.asSymbol());
        } else if (originalKey instanceof RubyFixnum) {
            getRuntime().getWarnings().warn("Do not use Fixnums as Symbols");
            throw getRuntime().newArgumentError(originalKey + " is not a symbol");
        } else {
            throw getRuntime().newArgumentError(originalKey + " is not a symbol");
        }
    }

    public IRubyObject aref(IRubyObject key) {
        key = getSymbolKey(key);
        
        if (!threadLocalVariables.containsKey(key)) {
            return getRuntime().getNil();
        }
        return (IRubyObject) threadLocalVariables.get(key);
    }

    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        key = getSymbolKey(key);
        
        threadLocalVariables.put(key, value);
        return value;
    }

    public RubyBoolean abort_on_exception() {
        return abortOnException ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject abort_on_exception_set(IRubyObject val) {
        abortOnException = val.isTrue();
        return val;
    }

    public RubyBoolean is_alive() {
        return threadImpl.isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyThread join(IRubyObject[] args) {
        long timeoutMillis = 0;
        if (args.length > 0) {
            if (args.length > 1) {
                throw getRuntime().newArgumentError(args.length,1);
            }
            // MRI behavior: value given in seconds; converted to Float; less
            // than or equal to zero returns immediately; returns nil
            timeoutMillis = (long)(1000.0D * args[0].convertToFloat().getValue());
            if (timeoutMillis <= 0) {
                return null;
            }
        }
        if (isCurrent()) {
            throw getRuntime().newThreadError("thread tried to join itself");
        }
        try {
            if (threadService.getCritical()) {
                // set the target thread's joinedBy, so it knows it can execute during a critical section
                joinedByCriticalThread = this;
                threadImpl.interrupt(); // break target thread out of critical
            }
            threadImpl.join(timeoutMillis);
        } catch (InterruptedException iExcptn) {
            assert false : iExcptn;
        } catch (TimeoutException iExcptn) {
            assert false : iExcptn;
        } catch (ExecutionException iExcptn) {
            assert false : iExcptn;
        }
        if (exitingException != null) {
            throw exitingException;
        }
        return null;
    }

    public IRubyObject value() {
        join(new IRubyObject[0]);
        synchronized (this) {
            return finalResult;
        }
    }

    public IRubyObject group() {
        if (threadGroup == null) {
        	return getRuntime().getNil();
        }
        
        return threadGroup;
    }
    
    void setThreadGroup(RubyThreadGroup rubyThreadGroup) {
    	threadGroup = rubyThreadGroup;
    }
    
    public IRubyObject inspect() {
        // FIXME: There's some code duplication here with RubyObject#inspect
        StringBuffer part = new StringBuffer();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(":0x");
        part.append(Integer.toHexString(System.identityHashCode(this)));
        
        if (threadImpl.isAlive()) {
            if (isStopped) {
                part.append(getRuntime().newString(" sleep"));
            } else if (killed) {
                part.append(getRuntime().newString(" aborting"));
            } else {
                part.append(getRuntime().newString(" run"));
            }
        } else {
            part.append(" dead");
        }
        
        part.append(">");
        return getRuntime().newString(part.toString());
    }

    public RubyBoolean has_key(IRubyObject key) {
        key = getSymbolKey(key);
        
        return getRuntime().newBoolean(threadLocalVariables.containsKey(key));
    }

    public RubyArray keys() {
        IRubyObject[] keys = new IRubyObject[threadLocalVariables.size()];
        
        return RubyArray.newArrayNoCopy(getRuntime(), (IRubyObject[])threadLocalVariables.keySet().toArray(keys));
    }
    
    public static IRubyObject critical_set(IRubyObject receiver, RubyBoolean value) {
    	receiver.getRuntime().getThreadService().setCritical(value.isTrue());
    	
    	return value;
    }

    public static IRubyObject critical(IRubyObject receiver) {
    	return receiver.getRuntime().newBoolean(receiver.getRuntime().getThreadService().getCritical());
    }
    
    public static IRubyObject stop(IRubyObject receiver) {
        RubyThread rubyThread = receiver.getRuntime().getThreadService().getCurrentContext().getThread();
        Object stopLock = rubyThread.stopLock;
        
        synchronized (stopLock) {
            try {
                rubyThread.isStopped = true;
                // attempt to decriticalize all if we're the critical thread
                receiver.getRuntime().getThreadService().setCritical(false);
                
                stopLock.wait();
            } catch (InterruptedException ie) {
                // ignore, continue;
            }
            rubyThread.isStopped = false;
        }
        
        return receiver.getRuntime().getNil();
    }
    
    public static IRubyObject s_kill(IRubyObject receiver, RubyThread rubyThread, Block block) {
        return rubyThread.kill();
    }
    
    public static IRubyObject s_exit(IRubyObject receiver, Block block) {
        RubyThread rubyThread = receiver.getRuntime().getThreadService().getCurrentContext().getThread();
        
        rubyThread.killed = true;
        // attempt to decriticalize all if we're the critical thread
        receiver.getRuntime().getThreadService().setCritical(false);
        
        throw new ThreadKill();
    }

    public RubyBoolean isStopped() {
    	// not valid for "dead" state
    	return getRuntime().newBoolean(isStopped);
    }
    
    public RubyThread wakeup() {
    	synchronized (stopLock) {
    		stopLock.notifyAll();
    	}
    	
    	return this;
    }
    
    public RubyFixnum priority() {
        return getRuntime().newFixnum(threadImpl.getPriority());
    }

    public IRubyObject priority_set(IRubyObject priority) {
        // FIXME: This should probably do some translation from Ruby priority levels to Java priority levels (until we have green threads)
        int iPriority = RubyNumeric.fix2int(priority);
        
        if (iPriority < Thread.MIN_PRIORITY) {
            iPriority = Thread.MIN_PRIORITY;
        } else if (iPriority > Thread.MAX_PRIORITY) {
            iPriority = Thread.MAX_PRIORITY;
        }
        
        threadImpl.setPriority(iPriority);
        return priority;
    }

    public IRubyObject raise(IRubyObject[] args, Block block) {
        ensureNotCurrent();
        Ruby runtime = getRuntime();
        
        if (DEBUG) System.out.println("thread " + Thread.currentThread() + " before raising");
        RubyThread currentThread = getRuntime().getCurrentContext().getThread();
        try {
            while (!(currentThread.lock.tryLock() && this.lock.tryLock())) {
                if (currentThread.lock.isHeldByCurrentThread()) currentThread.lock.unlock();
            }

            currentThread.pollThreadEvents();
            if (DEBUG) System.out.println("thread " + Thread.currentThread() + " raising");
            receivedException = prepareRaiseException(runtime, args, block);

            // interrupt the target thread in case it's blocking or waiting
            threadImpl.interrupt();
        } finally {
            if (currentThread.lock.isHeldByCurrentThread()) currentThread.lock.unlock();
            if (this.lock.isHeldByCurrentThread()) this.lock.unlock();
        }

        return this;
    }

    private IRubyObject prepareRaiseException(Ruby runtime, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 3); 

        if(args.length == 0) {
            IRubyObject lastException = runtime.getGlobalVariables().get("$!");
            if(lastException.isNil()) {
                return new RaiseException(runtime, runtime.getClass("RuntimeError"), "", false).getException();
            } 
            return lastException;
        }

        IRubyObject exception;
        ThreadContext context = getRuntime().getCurrentContext();
        
        if(args.length == 1) {
            if(args[0] instanceof RubyString) {
                return runtime.getClass("RuntimeError").newInstance(args, block);
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
        
        if (!exception.isKindOf(runtime.getClass("Exception"))) {
            return runtime.newTypeError("exception object expected").getException();
        }
        
        if (args.length == 3) {
            ((RubyException) exception).set_backtrace(args[2]);
        }
        
        return exception;
    }
    
    public IRubyObject run() {
        // if stopped, unstop
        synchronized (stopLock) {
            if (isStopped) {
                isStopped = false;
                stopLock.notifyAll();
            }
        }
    	
    	return this;
    }
    
    public void sleep(long millis) throws InterruptedException {
        ensureCurrent();
        synchronized (stopLock) {
            try {
                isStopped = true;
                stopLock.wait(millis);
            } finally {
                isStopped = false;
                pollThreadEvents();
            }
        }
    }

    public IRubyObject status() {
        if (threadImpl.isAlive()) {
        	if (isStopped) {
            	return getRuntime().newString("sleep");
            } else if (killed) {
                return getRuntime().newString("aborting");
            }
        	
            return getRuntime().newString("run");
        } else if (exitingException != null) {
            return getRuntime().getNil();
        } else {
            return getRuntime().newBoolean(false);
        }
    }

    public IRubyObject kill() {
    	// need to reexamine this
        RubyThread currentThread = getRuntime().getCurrentContext().getThread();
        
        try {
            if (DEBUG) System.out.println("thread " + Thread.currentThread() + " trying to kill");
            while (!(currentThread.lock.tryLock() && this.lock.tryLock())) {
                if (currentThread.lock.isHeldByCurrentThread()) currentThread.lock.unlock();
            }

            currentThread.pollThreadEvents();

            if (DEBUG) System.out.println("thread " + Thread.currentThread() + " succeeded with kill");
            killed = true;

            threadImpl.interrupt(); // break out of wait states and blocking IO
        } finally {
            if (currentThread.lock.isHeldByCurrentThread()) currentThread.lock.unlock();
            if (this.lock.isHeldByCurrentThread()) this.lock.unlock();
        }
        
        try {
            threadImpl.join();
        } catch (InterruptedException ie) {
            // we were interrupted, check thread events again
            currentThread.pollThreadEvents();
        } catch (ExecutionException ie) {
            // we were interrupted, check thread events again
            currentThread.pollThreadEvents();
        }
        
        return this;
    }
    
    public IRubyObject exit() {
    	return kill();
    }

    private boolean isCurrent() {
        return threadImpl.isCurrent();
    }

    public void exceptionRaised(RaiseException exception) {
        assert isCurrent();

        Ruby runtime = exception.getException().getRuntime();
        if (abortOnException(runtime)) {
            // FIXME: printError explodes on some nullpointer
            //getRuntime().getRuntime().printError(exception.getException());
        	// TODO: Doesn't SystemExit have its own method to make this less wordy..
            RubyException re = RubyException.newException(getRuntime(), getRuntime().getClass("SystemExit"), exception.getMessage());
            re.setInstanceVariable("status", getRuntime().newFixnum(1));
            threadService.getMainThread().raise(new IRubyObject[]{re}, Block.NULL_BLOCK);
        } else {
            exitingException = exception;
        }
    }

    private boolean abortOnException(Ruby runtime) {
        return (runtime.isGlobalAbortOnExceptionEnabled() || abortOnException);
    }

    public static RubyThread mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }
}
