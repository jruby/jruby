/*
 *  Copyright (C) 2002 Jason Voegele
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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.ThreadError;
import org.jruby.exceptions.RaiseException;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
public class RubyThread extends RubyObject {

    protected static boolean static_abort_on_exception;

    private Thread jvmThread;
    private Map threadLocalVariables = new HashMap();
    private boolean abortOnException;
    private RaiseException exitingException = null;

    public static RubyClass createThreadClass(Ruby ruby) {
        RubyClass threadClass = ruby.defineClass("Thread", ruby.getClasses().getObjectClass());

        // class methods
        threadClass.defineSingletonMethod(
            "abort_on_exception",
            CallbackFactory.getSingletonMethod(RubyThread.class, "abort_on_exception", RubyString.class));
        threadClass.defineSingletonMethod(
            "abort_on_exception=",
            CallbackFactory.getSingletonMethod(RubyThread.class, "abort_on_exception_set", RubyBoolean.class));
        threadClass.defineSingletonMethod("critical", CallbackFactory.getSingletonMethod(RubyThread.class, "critical"));
        threadClass.defineSingletonMethod(
            "critical=",
            CallbackFactory.getSingletonMethod(RubyThread.class, "critical_set", RubyBoolean.class));
        threadClass.defineSingletonMethod("current", CallbackFactory.getSingletonMethod(RubyThread.class, "current"));
        threadClass.defineSingletonMethod("exit", CallbackFactory.getSingletonMethod(RubyThread.class, "exit"));
        threadClass.defineSingletonMethod(
            "fork",
            CallbackFactory.getOptSingletonMethod(RubyThread.class, "newInstance"));
//        threadClass.defineSingletonMethod(
//            "kill",
//            CallbackFactory.getSingletonMethod(RubyThread.class, "kill", RubyThread.class));
        threadClass.defineSingletonMethod("list", CallbackFactory.getSingletonMethod(RubyThread.class, "list"));
//        threadClass.defineSingletonMethod("main", CallbackFactory.getSingletonMethod(RubyThread.class, "main"));
        threadClass.defineSingletonMethod(
            "new",
            CallbackFactory.getOptSingletonMethod(RubyThread.class, "newInstance"));
        threadClass.defineSingletonMethod("pass", CallbackFactory.getSingletonMethod(RubyThread.class, "pass"));
        threadClass.defineSingletonMethod("start", CallbackFactory.getOptSingletonMethod(RubyThread.class, "start"));
    //    threadClass.defineSingletonMethod("stop", CallbackFactory.getSingletonMethod(RubyThread.class, "stop"));

        // instance methods
        threadClass.defineMethod("[]", CallbackFactory.getMethod(RubyThread.class, "aref", IRubyObject.class));
        threadClass.defineMethod(
            "[]=",
            CallbackFactory.getMethod(RubyThread.class, "aset", IRubyObject.class, IRubyObject.class));
        threadClass.defineMethod(
            "abort_on_exception",
            CallbackFactory.getMethod(RubyThread.class, "abort_on_exception"));
        threadClass.defineMethod(
            "abort_on_exception=",
            CallbackFactory.getMethod(RubyThread.class, "abort_on_exception_set", RubyBoolean.class));
        threadClass.defineMethod("alive?", CallbackFactory.getMethod(RubyThread.class, "is_alive"));
        threadClass.defineMethod("exit", CallbackFactory.getMethod(RubyThread.class, "exit"));
        threadClass.defineMethod("join", CallbackFactory.getMethod(RubyThread.class, "join"));
        threadClass.defineMethod("key?", CallbackFactory.getMethod(RubyThread.class, "has_key", IRubyObject.class));
        threadClass.defineMethod("kill", CallbackFactory.getMethod(RubyThread.class, "exit"));
        threadClass.defineMethod("priority", CallbackFactory.getMethod(RubyThread.class, "priority"));
        threadClass.defineMethod(
            "priority=",
            CallbackFactory.getMethod(RubyThread.class, "priority_set", RubyFixnum.class));
        threadClass.defineMethod("raise", CallbackFactory.getMethod(RubyThread.class, "raise", RubyException.class));
        threadClass.defineMethod("run", CallbackFactory.getMethod(RubyThread.class, "run"));
        threadClass.defineMethod("safe_level", CallbackFactory.getMethod(RubyThread.class, "safe_level"));
        threadClass.defineMethod("status", CallbackFactory.getMethod(RubyThread.class, "status"));
        threadClass.defineMethod("stop?", CallbackFactory.getMethod(RubyThread.class, "is_stopped"));
        threadClass.defineMethod("value", CallbackFactory.getMethod(RubyThread.class, "value"));
        threadClass.defineMethod("wakeup", CallbackFactory.getMethod(RubyThread.class, "wakeup"));

        Ruby runtime = threadClass.getRuntime();
        RubyThread currentThread = new RubyThread(runtime, threadClass);
        currentThread.jvmThread = Thread.currentThread();
        runtime.getCurrentContext().setCurrentThread(currentThread);

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

    private static RubyThread startThread(final IRubyObject recv, final IRubyObject[] args, boolean callInit) {
        final Ruby runtime = recv.getRuntime();
        if (! runtime.isBlockGiven()) {
            throw new ThreadError(runtime, "must be called with a block");
        }
        final RubyThread thread = new RubyThread(runtime, (RubyClass) recv);
        if (callInit) {
            thread.callInit(args);
        }

        final RubyProc proc = RubyProc.newProc(runtime, runtime.getClasses().getProcClass());

        final Frame currentFrame = runtime.getCurrentFrame();
        final Block currentBlock = runtime.getBlockStack().getCurrent();

        thread.jvmThread = new Thread(new Runnable() {
            public void run() {
                runtime.registerNewContext(thread);
                ThreadContext context = runtime.getCurrentContext();
                context.getFrameStack().push(currentFrame);
                context.getBlockStack().setCurrent(currentBlock);

                // Call the thread's code
                try {
                    proc.call(args);
                } catch (RaiseException e) {
                    thread.exitingException = e;
                }
            }
        });

        thread.jvmThread.start();
        return thread;
    }

    protected RubyThread(Ruby ruby) {
        this(ruby, ruby.getClasses().getThreadClass());
    }

    protected RubyThread(Ruby ruby, RubyClass type) {
        super(ruby, type);
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     */
    public static RubyBoolean abort_on_exception(IRubyObject recv) {
        return static_abort_on_exception ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static RubyBoolean abort_on_exception_set(IRubyObject recv, RubyBoolean val) {
        static_abort_on_exception = val.isTrue();
        return val;
    }

    public static RubyBoolean critical(IRubyObject recv) {
        throw new NotImplementedError();
    }

    public static RubyBoolean critical_set(IRubyObject recv, RubyBoolean val) {
        throw new NotImplementedError();
    }

    public static RubyThread current(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().getCurrentThread();
    }

    public static IRubyObject pass(IRubyObject recv) {
        Thread.yield();
        return recv.getRuntime().getNil();
    }

    public static RubyArray list(IRubyObject recv) {
        ArrayList list = new ArrayList();
        Iterator iter = recv.getRuntime().objectSpace.iterator(recv.getRuntime().getClasses().getThreadClass());
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return RubyArray.newArray(recv.getRuntime(), list);
    }

    public IRubyObject aref(IRubyObject key) {
        String name = keyName(key);
        if (! threadLocalVariables.containsKey(name)) {
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
            name = ((RubySymbol) key).toId();
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

    public RubyBoolean abort_on_exception_set(RubyBoolean val) {
        abortOnException = val.isTrue();
        return val;
    }

    public RubyBoolean is_alive() {
        return jvmThread.isAlive() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyThread join() {
        if (jvmThread == Thread.currentThread()) {
            throw new ThreadError(getRuntime(), "thread tried to join itself");
        }
        try {
            jvmThread.join();
        } catch (InterruptedException e) {
            // FIXME: output warning
        }
        if (exitingException != null) {
            throw exitingException;
        }
        return this;
    }

    public RubyBoolean has_key(IRubyObject key) {
        String name = keyName(key);
        return RubyBoolean.newBoolean(getRuntime(), threadLocalVariables.containsKey(name));
    }

    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuntime(), jvmThread.getPriority());
    }

    public RubyFixnum priority_set(RubyFixnum priority) {
        jvmThread.setPriority((int) priority.getLongValue());
        return priority;
    }

    public void raise(RubyException exc) {
        throw new NotImplementedError();
    }

    public IRubyObject status() {
        if (jvmThread.isAlive()) {
            return RubyString.newString(getRuntime(), "run");
        } else if (exitingException != null) {
            return getRuntime().getNil();
        } else {
            return RubyBoolean.newBoolean(getRuntime(), false);
        }
    }
}

