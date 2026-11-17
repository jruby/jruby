package org.jruby.ext.monitor;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.thread.Mutex;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.runtimeError;

@JRubyClass(name = "Monitor")
public class Monitor extends RubyObject {
    private final Mutex mutex;
    private volatile RubyThread owner;
    private volatile long count;

    public Monitor(Ruby runtime, RubyClass klass) {
        super(runtime, klass);

        mutex = new Mutex(runtime, runtime.getMutex());
    }

    public static void createMonitorClass(ThreadContext context) {
        defineClass(context, "Monitor", objectClass(context), Monitor::new).defineMethods(context, Monitor.class);
    }

    @JRubyMethod
    public RubyBoolean try_enter(ThreadContext context) {
        if (!ownedByCurrentThread(context)) {
            if (!mutex.tryLock(context)) {
                return context.fals;
            }
            owner = context.getThread();
            count = 0;
        }

        count++;

        return context.tru;
    }

    @JRubyMethod
    public IRubyObject enter(ThreadContext context) {
        if (!ownedByCurrentThread(context)) {
            mutex.lock(context);
            owner = context.getThread();
            count = 0;
        }

        count++;

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject exit(ThreadContext context) {
        mon_check_owner(context);

        if (count <= 0) throw runtimeError(context, "monitor_exit: count:" + count + "\n");

        count--;

        if (count == 0) {
            owner = null;
            mutex.unlock(context);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject synchronize(ThreadContext context, Block block) {
        enter(context);
        try {
            return block.yieldSpecific(context);
        } finally {
            exit(context);
        }
    }

    @JRubyMethod(name = "mon_locked?")
    public IRubyObject mon_locked_p(ThreadContext context) {
        return mutex.locked_p(context);
    }

    @JRubyMethod
    public IRubyObject mon_check_owner(ThreadContext context) {
        checkOwner(context);

        return context.nil;
    }

    @JRubyMethod(name = "mon_owned?")
    public RubyBoolean mon_owned_p(ThreadContext context) {
        return asBoolean(context, ownedByCurrentThread(context));
    }

    @JRubyMethod
    public IRubyObject wait_for_cond(ThreadContext context, IRubyObject cond, IRubyObject timeout) {
        long count = exitForCondition();

        try {
            sites(context).wait.call(context, cond, cond, mutex, timeout);

            return context.tru;
        } finally {
            this.owner = context.getThread();
            this.count = count;
        }
    }

    private boolean ownedByCurrentThread(ThreadContext context) {
        return owner == context.getThread();
    }

    private void checkOwner(ThreadContext context) {
        if (!ownedByCurrentThread(context)) {
            throw context.runtime.newThreadError("current thread not owner");
        }
    }

    private long exitForCondition() {
        long cnt = count;

        owner = null;
        count = 0;

        return cnt;
    }

    private static JavaSites.MonitorSites sites(ThreadContext context) {
        return context.sites.Monitor;
    }
}
