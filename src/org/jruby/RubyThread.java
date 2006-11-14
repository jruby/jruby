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
import org.jruby.internal.runtime.NativeThread;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

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
    private NativeThread threadImpl;
    private Map threadLocalVariables = new HashMap();
    private boolean abortOnException;
    private RaiseException exitingException;
    private IRubyObject receivedException;
    private RubyThreadGroup threadGroup;

    private ThreadService threadService;
    private Object hasStartedLock = new Object();
    private boolean hasStarted = false;
    private volatile boolean isStopped = false;
    public Object stopLock = new Object();
    
    private volatile boolean killed = false;
    public Object killLock = new Object();
    private RubyThread joinedByCriticalThread;
    
    public static RubyClass createThreadClass(IRuby runtime) {
        RubyClass threadClass = runtime.defineClass("Thread", runtime.getObject());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyThread.class);

        threadClass.defineMethod("[]", 
                callbackFactory.getMethod("aref", IRubyObject.class));
        threadClass.defineMethod("[]=", 
                callbackFactory.getMethod("aset", IRubyObject.class, IRubyObject.class));
        threadClass.defineMethod("abort_on_exception", 
                callbackFactory.getMethod("abort_on_exception"));
        threadClass.defineMethod("abort_on_exception=", 
                callbackFactory.getMethod("abort_on_exception_set", IRubyObject.class));
        threadClass.defineMethod("alive?", 
                callbackFactory.getMethod("is_alive"));
        threadClass.defineMethod("group", 
                callbackFactory.getMethod("group"));
        threadClass.defineMethod("join", 
                callbackFactory.getMethod("join"));
        threadClass.defineMethod("key?", 
                callbackFactory.getMethod("has_key", IRubyObject.class));
        threadClass.defineMethod("priority", 
                callbackFactory.getMethod("priority"));
        threadClass.defineMethod("priority=", 
                callbackFactory.getMethod("priority_set", IRubyObject.class));
        threadClass.defineMethod("raise", 
                callbackFactory.getMethod("raise", IRubyObject.class));
        threadClass.defineMethod("run",
        		callbackFactory.getMethod("run"));
        threadClass.defineMethod("status", 
                callbackFactory.getMethod("status"));
        threadClass.defineMethod("stop?", 
                callbackFactory.getMethod("isStopped"));
        threadClass.defineMethod("wakeup", 
                callbackFactory.getMethod("wakeup"));
        threadClass.defineMethod("kill", 
                callbackFactory.getMethod("kill"));
        threadClass.defineMethod("exit",
        		callbackFactory.getMethod("exit"));
        
        threadClass.defineSingletonMethod("current",
                callbackFactory.getSingletonMethod("current"));
        threadClass.defineSingletonMethod("fork",
                callbackFactory.getOptSingletonMethod("newInstance"));
        threadClass.defineSingletonMethod("new",
                callbackFactory.getOptSingletonMethod("newInstance"));
        threadClass.defineSingletonMethod("list",
                callbackFactory.getSingletonMethod("list"));
        threadClass.defineSingletonMethod("pass",
                callbackFactory.getSingletonMethod("pass"));
        threadClass.defineSingletonMethod("start",
                callbackFactory.getOptSingletonMethod("start"));
        threadClass.defineSingletonMethod("critical=", 
                callbackFactory.getSingletonMethod("critical_set", RubyBoolean.class));
        threadClass.defineSingletonMethod("critical", 
                callbackFactory.getSingletonMethod("critical"));
        threadClass.defineSingletonMethod("stop", 
                callbackFactory.getSingletonMethod("stop"));
        threadClass.defineSingletonMethod("kill", 
                callbackFactory.getSingletonMethod("s_kill", RubyThread.class));
        threadClass.defineSingletonMethod("exit", 
                callbackFactory.getSingletonMethod("s_exit"));
        threadClass.defineSingletonMethod("abort_on_exception",
                callbackFactory.getSingletonMethod("abort_on_exception"));
        threadClass.defineSingletonMethod("abort_on_exception=",
                callbackFactory.getSingletonMethod("abort_on_exception_set", IRubyObject.class));

        RubyThread rubyThread = new RubyThread(runtime, threadClass);
        // set hasStarted to true, otherwise Thread.main.status freezes
        rubyThread.hasStarted = true;
        // TODO: need to isolate the "current" thread from class creation
        rubyThread.threadImpl = new NativeThread(rubyThread, Thread.currentThread());
        runtime.getThreadService().setMainThread(rubyThread);
        
        threadClass.defineSingletonMethod("main",
        		callbackFactory.getSingletonMethod("main"));
        
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
    public static RubyThread start(IRubyObject recv, IRubyObject[] args) {
        return startThread(recv, args, false);
    }
    
    public static RubyThread adopt(IRubyObject recv, Thread t) {
        return adoptThread(recv, t);
    }

    private static RubyThread adoptThread(final IRubyObject recv, Thread t) {
        final IRuby runtime = recv.getRuntime();
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) recv, false);
        
        rubyThread.threadImpl = new NativeThread(rubyThread, t);
        runtime.getThreadService().registerNewThread(rubyThread);
        
        runtime.getCurrentContext().preAdoptThread();
        
        rubyThread.callInit(new IRubyObject[0]);
        
        rubyThread.notifyStarted();
        
        return rubyThread;
    }

    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit) {
        final IRuby runtime = recv.getRuntime();
        if (!runtime.getCurrentContext().isBlockGiven()) {
            throw runtime.newThreadError("must be called with a block");
        }
        final RubyThread rubyThread = new RubyThread(runtime, (RubyClass) recv);
        if (callInit) {
            rubyThread.callInit(args);
        }

        rubyThread.threadImpl = new NativeThread(rubyThread, args);
        rubyThread.threadImpl.start();
        
        // make sure the thread has started before continuing, so it will appear "runnable" to the rest of Ruby
        rubyThread.ensureStarted();
        
        return rubyThread;
    }
    
    public void cleanTerminate() {
    	try {
    		isStopped = true;
    		waitIfCriticalized();
    	} catch (InterruptedException ie) {
    		// ignore
    	}
    }
	
	public void waitIfCriticalized() throws InterruptedException {
        RubyThread criticalThread = getRuntime().getThreadService().getCriticalThread();
		if (criticalThread != null && criticalThread != this && criticalThread != joinedByCriticalThread) {
			synchronized (criticalThread) {
                criticalThread.wait();
			}
		}
	}
    
    public void notifyStarted() {
        assert isCurrent();
        synchronized (hasStartedLock) {
            hasStarted = true;
            hasStartedLock.notifyAll();
        }
    }

    public void pollThreadEvents() {
        // Asserts.isTrue(isCurrent());
        pollReceivedExceptions();
        
        // TODO: should exceptions trump thread control, or vice versa?
        criticalizeOrDieIfKilled();
    }

    private void pollReceivedExceptions() {
        if (receivedException != null) {
            // clear this so we don't keep re-throwing
            IRubyObject raiseException = receivedException;
            receivedException = null;
            RubyModule kernelModule = getRuntime().getModule("Kernel");
            kernelModule.callMethod(getRuntime().getCurrentContext(), "raise", raiseException);
        }
    }

    public void criticalizeOrDieIfKilled() {
    	try {
    		waitIfCriticalized();
    	} catch (InterruptedException ie) {
    		throw new ThreadKill();
    	}
    	dieIfKilled();
    }

    private RubyThread(IRuby runtime, RubyClass type) {
        super(runtime, type);
        this.threadService = runtime.getThreadService();
        // set to default thread group
        RubyThreadGroup defaultThreadGroup = (RubyThreadGroup)runtime.getClass("ThreadGroup").getConstant("Default");
        defaultThreadGroup.add(this);
        
    }

    private RubyThread(IRuby runtime, RubyClass type, boolean narf) {
        super(runtime, type);
        this.threadService = runtime.getThreadService();
        
        // set to default thread group
        RubyThreadGroup defaultThreadGroup = (RubyThreadGroup)runtime.getClass("ThreadGroup").getConstant("Default");
        defaultThreadGroup.add(this);
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    public static RubyBoolean abort_on_exception(IRubyObject recv) {
    	IRuby runtime = recv.getRuntime();
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
        IRuby runtime = recv.getRuntime();
        ThreadService ts = runtime.getThreadService();
        RubyThread criticalThread = ts.getCriticalThread();
        RubyThread currentThread = ts.getCurrentContext().getThread();
        
        if (criticalThread == currentThread) {
            // we're currently the critical thread; decriticalize for pass
            ts.setCritical(false);
        }
        
        Thread.yield();
        
        if (criticalThread != null) {
            // recriticalize
            ts.setCritical(true);
        }
        
        return recv.getRuntime().getNil();
    }

    public static RubyArray list(IRubyObject recv) {
    	RubyThread[] activeThreads = recv.getRuntime().getThreadService().getActiveRubyThreads();
        
        return recv.getRuntime().newArray(activeThreads);
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
            name = ((RubyString) key).toString();
        } else {
            throw getRuntime().newArgumentError(key.inspect() + " is not a symbol");
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
        return threadImpl.isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyThread join() {
        if (isCurrent()) {
            throw getRuntime().newThreadError("thread tried to join itself");
        }
        ensureStarted();
        try {
            RubyThread criticalThread = getRuntime().getThreadService().getCriticalThread();
            if (criticalThread != null) {
                // set the target thread's joinedBy, so it knows it can execute during a critical section
                joinedByCriticalThread = criticalThread;
                threadImpl.interrupt(); // break target thread out of critical
            }
            threadImpl.join();
        } catch (InterruptedException iExcptn) {
            assert false : iExcptn;
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
        return getRuntime().newBoolean(threadLocalVariables.containsKey(name));
    }
    
    public static IRubyObject critical_set(IRubyObject receiver, RubyBoolean value) {
    	receiver.getRuntime().getThreadService().setCritical(value.isTrue());
    	
    	return value;
    }

    public static IRubyObject critical(IRubyObject receiver) {
    	return receiver.getRuntime().newBoolean(receiver.getRuntime().getThreadService().getCriticalThread() != null);
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
    
    public static IRubyObject s_kill(IRubyObject receiver, RubyThread rubyThread) {
    	return rubyThread.kill();
    }

    public static IRubyObject s_exit(IRubyObject receiver) {
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

    public IRubyObject raise(IRubyObject exc) {
        receivedException = exc;

        // FIXME: correct raise call

        // FIXME: call the IRaiseListener#exceptionRaised method

        return this;
    }
    
    public IRubyObject run() {
    	// if stopped, unstop
    	if (isStopped) {
    		synchronized (stopLock) {
    			isStopped = false;
    			stopLock.notifyAll();
    		}
    	}
    	
    	// Abort any sleep()s
    	// CON: Sleep now waits on the same stoplock, so it will have been woken up by the notify above
    	//threadImpl.interrupt();
    	
    	return this;
    }
    
    public void sleep(long millis) throws InterruptedException {
    	try {
	    	synchronized (stopLock) {
	    		isStopped = true;
	    		stopLock.wait(millis);
	    	}
    	} finally {
    		isStopped = false;
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
    	synchronized (this) {
    		if (killed) return this;
    		
    		killed = true;
            
    		threadImpl.interrupt(); // break out of wait states and blocking IO
    		try {
    			if (!threadImpl.isInterrupted()) {
                    // we did not interrupt the thread, so wait for it to complete
                    // TODO: test that this is correct...should killer wait for killee to die?
                    threadImpl.join();
    			}
    		} catch (InterruptedException ie) {
    			throw new ThreadKill();
    		}
    	}

    	return this;
    }
    
    public IRubyObject exit() {
    	return kill();
    }
    
    public void dieIfKilled() {
    	if (killed) throw new ThreadKill();
    }

    private boolean isCurrent() {
        return threadImpl.isCurrent();
    }

    private void ensureStarted() {
        // The JVM's join() method may return immediately
        // and isAlive() give the wrong result if the thread
        // hasn't started yet. We give it a chance to start
        // before we try to do anything.


        // Yes, I know double-check locking is broken.
        if (!hasStarted) {
            synchronized (hasStartedLock) {
                if (!hasStarted) {
                    try {
                        hasStartedLock.wait();
                    } catch (InterruptedException iExcptn) {
                        assert false : iExcptn;
                    }
                }
            }
        }
    }

    public void exceptionRaised(RaiseException exception) {
        assert isCurrent();

        IRuby runtime = exception.getException().getRuntime();
        if (abortOnException(runtime)) {
            // FIXME: printError explodes on some nullpointer
            //getRuntime().getRuntime().printError(exception.getException());
        	// TODO: Doesn't SystemExit have its own method to make this less wordy..
            RubyException re = RubyException.newException(getRuntime(), getRuntime().getClass("SystemExit"), exception.getMessage());
            re.setInstanceVariable("status", getRuntime().newFixnum(1));
            threadService.getMainThread().raise(re);
        } else {
            exitingException = exception;
        }
    }

    private boolean abortOnException(IRuby runtime) {
        return (runtime.isGlobalAbortOnExceptionEnabled() || abortOnException);
    }

    public static RubyThread mainThread(IRubyObject receiver) {
        return receiver.getRuntime().getThreadService().getMainThread();
    }
}
