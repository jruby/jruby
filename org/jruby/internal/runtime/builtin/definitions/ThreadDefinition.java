package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.ThreadClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.util.Asserts;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ThreadDefinition extends ClassDefinition {
    private static final int THREAD = 0x1200;
    private static final int STATIC = THREAD | 0x100;

    private static final int S_ABORT_ON_EXCEPTION = STATIC | 0x01;
    private static final int S_ABORT_ON_EXCEPTION_SET = STATIC | 0x02;
    private static final int CURRENT = STATIC | 0x03;
    private static final int S_EXIT = STATIC | 0x04;
    private static final int LIST = STATIC | 0x05;
    private static final int NEW = STATIC | 0x06;
    private static final int PASS = STATIC | 0x07;
    private static final int START = STATIC | 0x08;
    
    public static final int AREF = THREAD | 0x01;
    public static final int ASET = THREAD | 0x02;
    public static final int ABORT_ON_EXCEPTION = THREAD | 0x03;
    public static final int ABORT_ON_EXCEPTION_SET = THREAD | 0x04;
    public static final int IS_ALIVE = THREAD | 0x05;
    public static final int EXIT = THREAD | 0x06;
    public static final int JOIN = THREAD | 0x07;
    public static final int IS_KEY = THREAD | 0x08;
    public static final int PRIORITY = THREAD | 0x09;
    public static final int PRIORITY_SET = THREAD | 0x10;
    public static final int RAISE = THREAD | 0x11;
    public static final int RUN = THREAD | 0x12;
    public static final int SAFE_LEVEL = THREAD | 0x13;
    public static final int STATUS = THREAD | 0x14;
    public static final int IS_STOP = THREAD | 0x15;
    public static final int VALUE = THREAD | 0x16;
    public static final int WAKEUP = THREAD | 0x17;

    /**
     * Constructor for Thread.
     * @param runtime
     */
    public ThreadDefinition(Ruby runtime) {
        super(runtime);
    }

    protected RubyClass createType(Ruby runtime) {
        return runtime.defineClass("Thread", runtime.getClasses().getObjectClass());
    }

    protected void defineSingletonMethods(SingletonMethodContext context) {
        context.create("abort_on_exception", S_ABORT_ON_EXCEPTION, 0);
        context.create("abort_on_exception=", S_ABORT_ON_EXCEPTION_SET, 1);
        context.create("current", CURRENT, 0);
        context.create("exit", S_EXIT, 0);
        context.createOptional("fork", NEW);
        context.create("list", LIST, 0);
        context.createOptional("new", NEW);
        context.create("pass", PASS, 0);
        context.createOptional("start", START);
        // context.create("main", MAIN, 0);
        // context.create("stop", STOP, 0);
        // context.create("critical", CRITICAL, 0);
        // context.create("critical=", CRITICAL_SET, 1);
        // context.create("kill", KILL, 1);
    }

    protected void defineMethods(MethodContext context) {
        context.create("[]", AREF, 1);
        context.create("[]=", ASET, 2);
        context.create("abort_on_exception", ABORT_ON_EXCEPTION, 0);
        context.create("abort_on_exception=", ABORT_ON_EXCEPTION_SET, 1);
        context.create("alive?", IS_ALIVE, 0);
        context.create("exit", EXIT, 0);
        context.create("join", JOIN, 0);
        context.create("key?", IS_KEY, 1);
        context.create("kill", EXIT, 0);
        context.create("priority", PRIORITY, 0);
        context.create("priority=", PRIORITY_SET, 1);
        context.createOptional("raise", RAISE, 1);
        context.create("run", RUN, 0);
        context.create("safe_level", SAFE_LEVEL, 0);
        context.create("status", STATUS, 0);
        context.create("stop?", IS_STOP, 0);
        context.create("value", VALUE, 0);
        context.create("wakeup", WAKEUP, 0);
    }

    public IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args) {
        switch (index) {
            case ABORT_ON_EXCEPTION :
                return ThreadClass.abort_on_exception(receiver);
            case ABORT_ON_EXCEPTION_SET :
                return ThreadClass.abort_on_exception_set(receiver, args[0]);
            case CURRENT :
                return ThreadClass.current(receiver);
            case S_EXIT :
                //return RubyThread.exit(receiver);
            case LIST :
                return ThreadClass.list(receiver);
            case NEW :
                return ThreadClass.newInstance(receiver, args);
            case PASS :
                return ThreadClass.pass(receiver);
            case START :
                return ThreadClass.start(receiver, args);
        }
        Asserts.notReached();
        return null;
    }
}