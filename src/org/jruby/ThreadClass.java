/*
 *  Copyright (C) 2002 Jason Voegele
 *  Copyright (C) 2002 Anders Bengtsson
 *  Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * 
 * Thomas E Enebo <enebo@acm.org>
 * Charles O Nutter <headius@headius.com>
 *
 *  JRuby - http://jruby.sourceforge.net
 *
 *  This file is part of JRuby
 *
 *  JRuby is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  JRuby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with JRuby; if not, write to
 *  the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307 USA
 */
package org.jruby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadError;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

/**
 * Implementation of Ruby's <code>Thread</code> class.  Each Ruby thread is
 * mapped to an underlying Java Virtual Machine thread.
 * <p>
 * Thread encapsulates the behavior of a thread of execution, including the main
 * thread of the Ruby script.  In the descriptions that follow, the parameter
 * <code>aSymbol</code> refers to a symbol, which is either a quoted string or a
 * <code>Symbol</code> (such as <code>:name</code>).
 *
 * @author Jason Voegele (jason@jvoegele.com)
 * @version $Revision$
 */
public class ThreadClass extends RubyObject {
    private static boolean globalAbortOnException; // move to runtime object

    private Thread jvmThread;
    private Map threadLocalVariables = new HashMap();
    private boolean abortOnException;
    private RaiseException exitingException;
    private IRubyObject receivedException;
    private RubyThreadGroup threadGroup;

    private ThreadService threadService;
    private Object hasStartedLock = new Object();
    private boolean hasStarted = false;
    private boolean isStopped = false;
    
    public static RubyClass createThreadClass(Ruby ruby) {
        RubyClass threadClass = ruby.defineClass("Thread", 
                ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();

        threadClass.defineMethod("[]", 
                callbackFactory.getMethod(ThreadClass.class, "aref", IRubyObject.class));
        threadClass.defineMethod("[]=", 
                callbackFactory.getMethod(ThreadClass.class, "aset", IRubyObject.class, IRubyObject.class));
        threadClass.defineMethod("abort_on_exception", 
                callbackFactory.getMethod(ThreadClass.class, "abort_on_exception", IRubyObject.class));
        threadClass.defineMethod("abort_on_exception=", 
                callbackFactory.getMethod(ThreadClass.class, "abort_on_exception_set", IRubyObject.class, IRubyObject.class));
        threadClass.defineMethod("alive?", 
                callbackFactory.getMethod(ThreadClass.class, "is_alive"));
        threadClass.defineMethod("group", 
                callbackFactory.getMethod(ThreadClass.class, "group"));
        threadClass.defineMethod("join", 
                callbackFactory.getMethod(ThreadClass.class, "join"));
        threadClass.defineMethod("key?", 
                callbackFactory.getMethod(ThreadClass.class, "has_key", IRubyObject.class));
        threadClass.defineMethod("priority", 
                callbackFactory.getMethod(ThreadClass.class, "priority"));
        threadClass.defineMethod("priority=", 
                callbackFactory.getMethod(ThreadClass.class, "priority_set", IRubyObject.class));
        threadClass.defineMethod("raise", 
                callbackFactory.getMethod(ThreadClass.class, "raise", IRubyObject.class));
        threadClass.defineMethod("run",
        		callbackFactory.getMethod(ThreadClass.class, "run"));
        threadClass.defineMethod("status", 
                callbackFactory.getMethod(ThreadClass.class, "status"));
        threadClass.defineMethod("stop?", 
                callbackFactory.getMethod(ThreadClass.class, "isStopped"));
        threadClass.defineMethod("wakeup", 
                callbackFactory.getMethod(ThreadClass.class, "wakeup"));
        threadClass.defineMethod("kill", 
                callbackFactory.getMethod(ThreadClass.class, "kill"));
        
        threadClass.defineSingletonMethod("current",
                callbackFactory.getSingletonMethod(ThreadClass.class, "current"));
        threadClass.defineSingletonMethod("fork",
                callbackFactory.getOptSingletonMethod(ThreadClass.class, "newInstance"));
        threadClass.defineSingletonMethod("new",
                callbackFactory.getOptSingletonMethod(ThreadClass.class, "newInstance"));
        threadClass.defineSingletonMethod("list",
                callbackFactory.getSingletonMethod(ThreadClass.class, "list"));
        threadClass.defineSingletonMethod("pass",
                callbackFactory.getSingletonMethod(ThreadClass.class, "pass"));
        threadClass.defineSingletonMethod("start",
                callbackFactory.getOptSingletonMethod(ThreadClass.class, "start"));
        threadClass.defineSingletonMethod("critical=", 
                callbackFactory.getSingletonMethod(ThreadClass.class, "critical_set", RubyBoolean.class));
        threadClass.defineSingletonMethod("critical", 
                callbackFactory.getSingletonMethod(ThreadClass.class, "critical"));
        threadClass.defineSingletonMethod("stop", 
                callbackFactory.getSingletonMethod(ThreadClass.class, "stop"));
        threadClass.defineSingletonMethod("kill", 
                callbackFactory.getMethod(ThreadClass.class, "s_kill", ThreadClass.class));

        ThreadClass currentThread = new ThreadClass(ruby, threadClass);
        // set hasStarted to true, otherwise Thread.main.status freezes
        currentThread.hasStarted = true;
        currentThread.jvmThread = Thread.currentThread();
        ruby.getThreadService().setMainThread(currentThread);
        
        threadClass.defineSingletonMethod("main",
        		callbackFactory.getSingletonMethod(ThreadClass.class, "main"));
        
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
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        return startThread(recv, args, true);
    }

    /**
     * Basically the same as Thread.new . However, if class Thread is
     * subclassed, then calling start in that subclass will not invoke the
     * subclass's initialize method.
     */
    public static ThreadClass start(IRubyObject recv, IRubyObject[] args) {
        return startThread(recv, args, false);
    }

    private static ThreadClass startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit) {
        final Ruby runtime = recv.getRuntime();
        if (!runtime.isBlockGiven()) {
            throw new ThreadError(runtime, "must be called with a block");
        }
        final ThreadClass thread = new ThreadClass(runtime, (RubyClass) recv);
        if (callInit) {
            thread.callInit(args);
        }

        final RubyProc proc = RubyProc.newProc(runtime);

        final Frame currentFrame = runtime.getCurrentFrame();
        final Block currentBlock = runtime.getBlockStack().getCurrent();

        thread.jvmThread = new Thread(new Runnable() {
            public void run() {
                thread.notifyStarted();

                runtime.getThreadService().registerNewThread(thread);
                ThreadContext context = runtime.getCurrentContext();
                context.getFrameStack().push(currentFrame);
                context.getBlockStack().setCurrent(currentBlock);

                // Call the thread's code
                try {
                    proc.call(args);
                } catch (RaiseException e) {
                    thread.exceptionRaised(e);
                }
            }
        });

        thread.jvmThread.start();
        return thread;
    }

    private void notifyStarted() {
        Asserts.isTrue(isCurrent());
        synchronized (hasStartedLock) {
            hasStarted = true;
            hasStartedLock.notifyAll();
        }
    }

    public void pollThreadEvents() {
        // Asserts.isTrue(isCurrent());
        pollReceivedExceptions();
    }

    private void pollReceivedExceptions() {
        if (receivedException != null) {
            RubyModule kernelModule = getRuntime().getClasses().getKernelModule();
            kernelModule.callMethod("raise", receivedException);
        }
    }

    private ThreadClass(Ruby ruby, RubyClass type) {
        super(ruby, type);
        this.threadService = ruby.getThreadService();
        // set to default thread group
        RubyThreadGroup defaultThreadGroup = (RubyThreadGroup)ruby.getClass("ThreadGroup").getConstant("Default");
        defaultThreadGroup.add(this);
        
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    public static RubyBoolean abort_on_exception(IRubyObject recv) {
        return globalAbortOnException ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static IRubyObject abort_on_exception_set(IRubyObject recv, IRubyObject value) {
        globalAbortOnException = value.isTrue();
        return value;
    }

    public static ThreadClass current(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getThread();
    }

    public static ThreadClass main(IRubyObject recv) {
        return recv.getRuntime().getThreadService().getMainThread();
    }

    public static IRubyObject pass(IRubyObject recv) {
        Thread.yield();
        return recv.getRuntime().getNil();
    }

    public static RubyArray list(IRubyObject recv) {
    	Ruby ruby = recv.getRuntime();
        List list = new ArrayList();
        
        Iterator iter = ruby.objectSpace.iterator(recv.getRuntime().getClasses().getThreadClass());
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        
        return RubyArray.newArray(recv.getRuntime(), list);
    }

    public IRubyObject aref(IRubyObject key) {
        String name = keyName(key);
        if (!threadLocalVariables.containsKey(name)) {
            return getRuntime().getNil();
        }
        return (IRubyObject) threadLocalVariables.get(name);
    }

    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        String name = keyName(key);
        threadLocalVariables.put(name, value);
        return value;
    }

    private String keyName(IRubyObject key) {
        String name;
        if (key instanceof RubySymbol) {
            name = key.asSymbol();
        } else if (key instanceof RubyString) {
            name = ((RubyString) key).getValue();
        } else {
            throw new ArgumentError(getRuntime(), key.inspect() + " is not a symbol");
        }
        return name;
    }

    public RubyBoolean abort_on_exception() {
        return abortOnException ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject abort_on_exception_set(IRubyObject val) {
        abortOnException = val.isTrue();
        return val;
    }

    public RubyBoolean is_alive() {
        return jvmThread.isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public ThreadClass join() {
        if (isCurrent()) {
            throw new ThreadError(getRuntime(), "thread tried to join itself");
        }
        ensureStarted();
        try {
            jvmThread.join();
        } catch (InterruptedException iExcptn) {
            Asserts.notReached();
        }
        if (exitingException != null) {
            throw exitingException;
        }
        return this;
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

    public RubyBoolean has_key(IRubyObject key) {
        String name = keyName(key);
        return RubyBoolean.newBoolean(getRuntime(), threadLocalVariables.containsKey(name));
    }
    
    // TODO: Determine overhead in implementing this
    public static IRubyObject critical_set(IRubyObject receiver, RubyBoolean value) {
    	// Useless implementation to satisfy tempfile.rb
    	return value;
    }

    public static IRubyObject critical(IRubyObject receiver) {
    	// TODO: Useless implementation to satisfy rubicon, always returns false
    	// real implementation, like kill, will require method call hooks
    	return RubyBoolean.newBoolean(receiver.getRuntime(), false);
    }

    public static IRubyObject stop(IRubyObject receiver) {
    	ThreadClass stopLock = receiver.getRuntime().getThreadService().getCurrentContext().getThread();
    	
    	synchronized (stopLock) {
    		try {
    			stopLock.isStopped = true;
    			stopLock.wait();
    		} catch (InterruptedException ie) {
    			// ignore, continue;
    		}
    		stopLock.isStopped = false;
    	}
    	
    	return receiver.getRuntime().getNil();
    }
    
    public static IRubyObject s_kill(ThreadClass rubyThread) {
    	// TODO: stubbed for now, will need to implement kill hooks into call stack
    	return rubyThread.kill();
    }

    public RubyBoolean isStopped() {
    	// not valid for "dead" state
    	return RubyBoolean.newBoolean(getRuntime(), isStopped);
    }
    
    public ThreadClass wakeup() {
    	synchronized (this) {
    		this.notifyAll();
    	}
    	
    	return this;
    }
    
    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuntime(), jvmThread.getPriority());
    }

    public IRubyObject priority_set(IRubyObject priority) {
        jvmThread.setPriority(RubyNumeric.fix2int(priority));
        return priority;
    }

    public IRubyObject raise(IRubyObject exc) {
        receivedException = exc;

        // FIXME: correct raise call

        // FIXME: call the IRaiseListener#exceptionRaised method

        return this;
    }
    
    public IRubyObject run() {
    	// if stopped, unstop
    	if (isStopped) {
    		synchronized (this) {
    			this.notifyAll();
    			isStopped = false;
    		}
    	}
    	
    	// Abort any sleep()s
    	jvmThread.interrupt();
    	
    	return this;
    }

    public IRubyObject status() {
        ensureStarted();
        if (jvmThread.isAlive()) {
        	if (isStopped) {
            	return RubyString.newString(getRuntime(), "sleep");
            }
        	
            return RubyString.newString(getRuntime(), "run");
        } else if (exitingException != null) {
            return getRuntime().getNil();
        } else {
            return RubyBoolean.newBoolean(getRuntime(), false);
        }
    }

    public IRubyObject kill() {
    	// TODO: stubbed for now, will need to implement kill hooks into call stack
    	jvmThread.interrupt();
    	return this;
    }

    private boolean isCurrent() {
        return Thread.currentThread() == jvmThread;
    }

    private void ensureStarted() {
        // The JVM's join() method may return immediately
        // and isAlive() give the wrong result if the thread
        // hasn't started yet. We give it a chance to start
        // before we try to do anything.

        if (hasStarted) {
            return;
        }
        synchronized (hasStartedLock) {
            if (!hasStarted) {
                try {
                    hasStartedLock.wait();
                } catch (InterruptedException iExcptn) {
                    Asserts.notReached();
                }
            }
        }

    }

    private void exceptionRaised(RaiseException exception) {
        Asserts.isTrue(Thread.currentThread() == jvmThread);

        if (abortOnException()) {
            // FIXME: printError explodes on some nullpointer
            //getRuntime().getRuntime().printError(exception.getException());
            threadService.getMainThread().raise(RubyException.newException(getRuntime(),
                                                                           getRuntime().getExceptions().getSystemExit(),
                                                                           ""));
        } else {
            exitingException = exception;
        }
    }

    private boolean abortOnException() {
        return (globalAbortOnException || abortOnException);
    }

    public static ThreadClass mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }
}
