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

import java.util.*;

import org.jruby.runtime.*;
import org.jruby.exceptions.*;

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
    // The JVM thread mapped to this Ruby thread instance
    /**
     * Description of the Field
     */
    protected Thread jvmThread;
    /**
     * Description of the Field
     */
    protected Map locals;

    /**
     * Description of the Field
     */
    protected static boolean static_abort_on_exception;
    /**
     * Description of the Field
     */
    protected static boolean critical;
    /**
     * Description of the Field
     */
    protected boolean abort_on_exception;

    // maps Java threads to Ruby threads
    private static Map threads;
    static {
        threads = new HashMap();
        // Put the main thread in the map under the key "main"
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyClass createThreadClass(Ruby ruby) {
        RubyClass threadClass =
            ruby.defineClass("Thread", ruby.getClasses().getObjectClass());

        // class methods
        threadClass.defineSingletonMethod(
            "abort_on_exception",
            CallbackFactory.getSingletonMethod(
                RubyThread.class,
                "abort_on_exception",
                RubyString.class));
        threadClass.defineSingletonMethod(
            "abort_on_exception=",
            CallbackFactory.getSingletonMethod(
                RubyThread.class,
                "abort_on_exception_set",
                RubyBoolean.class));
        threadClass.defineSingletonMethod(
            "critical",
            CallbackFactory.getSingletonMethod(RubyThread.class, "critical"));
        threadClass.defineSingletonMethod(
            "critical=",
            CallbackFactory.getSingletonMethod(
                RubyThread.class,
                "critical_set",
                RubyBoolean.class));
        threadClass.defineSingletonMethod(
            "current",
            CallbackFactory.getSingletonMethod(RubyThread.class, "current"));
        threadClass.defineSingletonMethod(
            "exit",
            CallbackFactory.getSingletonMethod(RubyThread.class, "exit"));
        threadClass.defineSingletonMethod(
            "fork",
            CallbackFactory.getOptSingletonMethod(
                RubyThread.class,
                "newInstance"));
        threadClass.defineSingletonMethod(
            "kill",
            CallbackFactory.getSingletonMethod(
                RubyThread.class,
                "kill",
                RubyThread.class));
        threadClass.defineSingletonMethod(
            "list",
            CallbackFactory.getSingletonMethod(RubyThread.class, "list"));
        threadClass.defineSingletonMethod(
            "main",
            CallbackFactory.getSingletonMethod(RubyThread.class, "main"));
        threadClass.defineSingletonMethod(
            "new",
            CallbackFactory.getOptSingletonMethod(
                RubyThread.class,
                "newInstance"));
        threadClass.defineSingletonMethod(
            "pass",
            CallbackFactory.getSingletonMethod(RubyThread.class, "pass"));
        threadClass.defineSingletonMethod(
            "start",
            CallbackFactory.getOptSingletonMethod(RubyThread.class, "start"));
        threadClass.defineSingletonMethod(
            "stop",
            CallbackFactory.getSingletonMethod(RubyThread.class, "stop"));

        // instance methods
        threadClass.defineMethod(
            "[]",
            CallbackFactory.getMethod(
                RubyThread.class,
                "aref",
                RubyObject.class));
        threadClass.defineMethod(
            "[]=",
            CallbackFactory.getMethod(
                RubyThread.class,
                "aset",
                RubyObject.class,
                RubyObject.class));
        threadClass.defineMethod(
            "abort_on_exception",
            CallbackFactory.getMethod(RubyThread.class, "abort_on_exception"));
        threadClass.defineMethod(
            "abort_on_exception=",
            CallbackFactory.getMethod(
                RubyThread.class,
                "abort_on_exception_set",
                RubyBoolean.class));
        threadClass.defineMethod(
            "alive?",
            CallbackFactory.getMethod(RubyThread.class, "is_alive"));
        threadClass.defineMethod(
            "exit",
            CallbackFactory.getMethod(RubyThread.class, "exit"));
        threadClass.defineMethod(
            "join",
            CallbackFactory.getMethod(RubyThread.class, "join"));
        threadClass.defineMethod(
            "key?",
            CallbackFactory.getMethod(
                RubyThread.class,
                "has_key",
                RubyObject.class));
        threadClass.defineMethod(
            "kill",
            CallbackFactory.getMethod(RubyThread.class, "exit"));
        threadClass.defineMethod(
            "priority",
            CallbackFactory.getMethod(RubyThread.class, "priority"));
        threadClass.defineMethod(
            "priority=",
            CallbackFactory.getMethod(
                RubyThread.class,
                "priority_set",
                RubyFixnum.class));
        threadClass.defineMethod(
            "raise",
            CallbackFactory.getMethod(
                RubyThread.class,
                "raise",
                RubyException.class));
        threadClass.defineMethod(
            "run",
            CallbackFactory.getMethod(RubyThread.class, "run"));
        threadClass.defineMethod(
            "safe_level",
            CallbackFactory.getMethod(RubyThread.class, "safe_level"));
        threadClass.defineMethod(
            "status",
            CallbackFactory.getMethod(RubyThread.class, "status"));
        threadClass.defineMethod(
            "stop?",
            CallbackFactory.getMethod(RubyThread.class, "is_stopped"));
        threadClass.defineMethod(
            "value",
            CallbackFactory.getMethod(RubyThread.class, "value"));
        threadClass.defineMethod(
            "wakeup",
            CallbackFactory.getMethod(RubyThread.class, "wakeup"));

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
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @param args Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyObject newInstance(
        Ruby ruby,
        RubyObject recv,
        RubyObject[] args) {
        return startThread(ruby, recv, args, true);
    }

    /**
     * Basically the same as Thread.new . However, if class Thread is
     * subclassed, then calling start in that subclass will not invoke the
     * subclass's initialize method.
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @param args Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyThread start(
        Ruby ruby,
        RubyObject recv,
        RubyObject[] args) {
        return startThread(ruby, recv, args, false);
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @param args Description of the Parameter
     * @param callInit Description of the Parameter
     * @return Description of the Return Value
     */
    protected static RubyThread startThread(
        final Ruby ruby,
        RubyObject recv,
        final RubyObject[] args,
        boolean callInit) {
        if (!ruby.isBlockGiven()) {
            System.out.println("No block given to thread!");
        }
        RubyThread result = new RubyThread(ruby, (RubyClass) recv);
        if (callInit) {
            result.callInit(args);
        }
        
        final RubyProc proc = RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
        
        final Frame currentFrame = ruby.getCurrentFrame();
        //final Iter currentIter = ruby.getCurrentIter();
        final Block currentBlock = ruby.getBlockStack().getCurrent();

        //result.jvmThread = new Thread(result.new RubyThreadRunner(ruby, args));
        result.jvmThread = new Thread(new Runnable() {
            public void run() {
                ruby.getFrameStack().push(currentFrame);
                //ruby.getIterStack().push(currentIter);
                ruby.getBlockStack().setCurrent(currentBlock);

                proc.call(args);
            }
        });
        result.threads.put(result.jvmThread, result);
        result.locals = new HashMap();

        result.jvmThread.start();
        return result;
    }

    /**
     *Constructor for the RubyThread object
     *
     * @param ruby Description of the Parameter
     */
    protected RubyThread(Ruby ruby) {
        this(ruby, ruby.getClasses().getThreadClass());
    }

    /**
     *Constructor for the RubyThread object
     *
     * @param ruby Description of the Parameter
     * @param type Description of the Parameter
     */
    protected RubyThread(Ruby ruby, RubyClass type) {
        super(ruby, type);
    }

    /**
     * Returns the status of the global ``abort on exception'' condition. The
     * default is false. When set to true, will cause all threads to abort (the
     * process will exit(0)) if an exception is raised in any thread. See also
     * Thread.abort_on_exception= .
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyBoolean abort_on_exception(Ruby ruby, RubyObject recv) {
        return static_abort_on_exception ? ruby.getTrue() : ruby.getFalse();
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @param val Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyBoolean abort_on_exception_set(
        Ruby ruby,
        RubyObject recv,
        RubyBoolean val) {
        static_abort_on_exception = val.isTrue();
        return val;
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyBoolean critical(Ruby ruby, RubyObject recv) {
        // TODO: Probably should just throw NotImplementedError
        return critical ? ruby.getTrue() : ruby.getFalse();
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @param val Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyBoolean critical_set(
        Ruby ruby,
        RubyObject recv,
        RubyBoolean val) {
        // TODO: Probably should just throw NotImplementedError
        critical = val.isTrue();
        return val;
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyThread current(Ruby ruby, RubyObject recv) {
        return (RubyThread) threads.get(Thread.currentThread());
    }

    /**
     * Description of the Method
     *
     * @param ruby Description of the Parameter
     * @param recv Description of the Parameter
     * @return Description of the Return Value
     */
    public static RubyArray list(Ruby ruby, RubyObject recv) {
        return new RubyArray(ruby, new ArrayList(threads.values()));
    }

    /**
     * Description of the Method
     *
     * @param key Description of the Parameter
     * @return Description of the Return Value
     */
    public RubyObject aref(RubyObject key) {
        if (!(key instanceof RubySymbol) || !(key instanceof RubyString)) {
            throw new ArgumentError(
                getRuby(),
                key.inspect() + " is not a symbol");
        }

        RubyObject result = (RubyObject) locals.get(key);
        if (result == null) {
            result = getRuby().getNil();
        }
        return result;
    }

    /**
     * Description of the Method
     *
     * @param key Description of the Parameter
     * @param val Description of the Parameter
     * @return Description of the Return Value
     */
    public RubyObject aset(RubyObject key, RubyObject val) {
        if (!(key instanceof RubySymbol) || !(key instanceof RubyString)) {
            throw new ArgumentError(
                getRuby(),
                key.inspect() + " is not a symbol");
        }

        locals.put(key, val);
        return val;
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public RubyBoolean abort_on_exception() {
        return abort_on_exception ? getRuby().getTrue() : getRuby().getFalse();
    }

    /**
     * Description of the Method
     *
     * @param val Description of the Parameter
     * @return Description of the Return Value
     */
    public RubyBoolean abort_on_exception_set(RubyBoolean val) {
        abort_on_exception = val.isTrue();
        return val;
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public RubyBoolean is_alive() {
        return jvmThread.isAlive() ? getRuby().getTrue() : getRuby().getFalse();
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public RubyThread join() {
        try {
            jvmThread.join();
        } catch (InterruptedException e) {
        }
        return this;
    }

    /**
     * Description of the Method
     *
     * @param key Description of the Parameter
     * @return Description of the Return Value
     */
    public RubyBoolean has_key(RubyObject key) {
        return locals.containsKey(key)
            ? getRuby().getTrue()
            : getRuby().getFalse();
    }

    /**
     * Description of the Method
     *
     * @return Description of the Return Value
     */
    public RubyFixnum priority() {
        return RubyFixnum.newFixnum(getRuby(), jvmThread.getPriority());
    }

    /**
     * Description of the Method
     *
     * @param priority Description of the Parameter
     * @return Description of the Return Value
     */
    public RubyFixnum priority_set(RubyFixnum priority) {
        jvmThread.setPriority((int) priority.getLongValue());
        return priority;
    }

    /**
     * Description of the Method
     *
     * @param exc Description of the Parameter
     */
    public void raise(RubyException exc) {
        // TODO: How do we raise the exception from the target thread
        // (as opposed to the calling thread)?
        //throw exc;
    }

    /**
     * Description of the Class
     *
     * @author Jason Voegele (jason@jvoegele.com)
     */
    protected class RubyThreadRunner implements Runnable {
        private RubyObject[] args;
        private Ruby ruby;

        /**
         *Constructor for the RubyThreadRunner object
         *
         * @param ruby Description of the Parameter
         * @param args Description of the Parameter
         */
        public RubyThreadRunner(Ruby ruby, RubyObject[] args) {
            this.ruby = ruby;
            this.args = args;
        }

        /**
         * Main processing method for the RubyThreadRunner object
         */
        public void run() {
            try {
                if (ruby == null) {
                    throw new RuntimeException("ruby is null!");
                }
                if (ruby.isBlockGiven()) {
                    System.out.println("THE BLOCK HAS PROPOGATED!!!");
                    ruby.yield(
                        RubyArray.newArray(
                            ruby,
                            new ArrayList(Arrays.asList(args))));
                } else {
                    System.out.println("THE BLOCK HAS NOT PROPOGATED :(");
                }
            } catch (Throwable t) {
                // TODO: Should abort_on_exception behavior be implemented here
                // or in the interpreter itself?
                /*
                 *  if (RubyThread.abort_on_exception(ruby, RubyThread.this).isTrue()) {
                 *  }
                 *  else if (RubyThread.this.abort_on_exception().isTrue()) {
                 *  }
                 *  else {
                 *  }
                 */
                // TODO: Temporary (hack) solution
                throw new RuntimeException(t.toString());
            }
        }
    }
}

