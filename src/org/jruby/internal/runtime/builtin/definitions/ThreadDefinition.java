/* Generated code - do not edit! */

package org.jruby.internal.runtime.builtin.definitions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.util.Asserts;

public class ThreadDefinition extends ClassDefinition {
    private static final int THREAD = 0xf000;
    private static final int STATIC = THREAD | 0x100;
    public static final int AREF = THREAD | 1;
    public static final int ASET = THREAD | 2;
    public static final int ABORT_ON_EXCEPTION = THREAD | 3;
    public static final int ABORT_ON_EXCEPTION_SET = THREAD | 4;
    public static final int IS_ALIVE = THREAD | 5;
    public static final int EXIT = THREAD | 6;
    public static final int JOIN = THREAD | 7;
    public static final int IS_KEY = THREAD | 8;
    public static final int PRIORITY = THREAD | 9;
    public static final int PRIORITY_SET = THREAD | 10;
    public static final int RAISE = THREAD | 11;
    public static final int STATUS = THREAD | 12;
    public static final int CURRENT = STATIC | 1;
    public static final int NEWINSTANCE = STATIC | 2;
    public static final int LIST = STATIC | 3;
    public static final int PASS = STATIC | 4;
    public static final int START = STATIC | 5;

    public ThreadDefinition(Ruby runtime) {
        super(runtime);
    }

    protected RubyClass createType(Ruby runtime) {
        RubyClass result =
            runtime.defineClass(
                "Thread",
                (RubyClass) runtime.getClasses().getClass("Object"));
        return result;
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
        context.create("priority", PRIORITY, 0);
        context.create("priority=", PRIORITY_SET, 1);
        context.createOptional("raise", RAISE, 1);
        context.create("status", STATUS, 0);
    }

    protected void defineSingletonMethods(SingletonMethodContext context) {
        context.create("current", CURRENT, 0);
        context.createOptional("fork", NEWINSTANCE, 0);
        context.create("list", LIST, 0);
        context.createOptional("new", NEWINSTANCE, 0);
        context.create("pass", PASS, 0);
        context.createOptional("start", START, 0);
    }
    public IRubyObject callIndexed(
        int index,
        IRubyObject receiver,
        IRubyObject[] args) {
        switch (index) {
            case CURRENT :
                return org.jruby.ThreadClass.current(receiver);
            case NEWINSTANCE :
                return org.jruby.ThreadClass.newInstance(receiver, args);
            case LIST :
                return org.jruby.ThreadClass.list(receiver);
            case PASS :
                return org.jruby.ThreadClass.pass(receiver);
            case START :
                return org.jruby.ThreadClass.start(receiver, args);
            default :
                Asserts.notReached();
                return null;
        }
    }
}
