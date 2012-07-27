package org.jruby.ext.fiber;

import org.jruby.CompatVersion;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class FiberMeta {
    @JRubyMethod(compat = CompatVersion.RUBY1_9, meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv) {
        Fiber fiber = context.getFiber();
        if (fiber.isRoot()) {
            throw context.runtime.newFiberError("can't yield from root fiber");
        }
        return fiber.yield(context, context.nil);
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9, meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Fiber fiber = context.getFiber();
        if (fiber.isRoot()) {
            throw context.runtime.newFiberError("can't yield from root fiber");
        }
        return fiber.yield(context, arg);
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9, rest = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Fiber fiber = context.getFiber();
        if (fiber.isRoot()) {
            throw context.runtime.newFiberError("can't yield from root fiber");
        }
        return fiber.yield(context, context.runtime.newArrayNoCopyLight(args));
    }
    
}
