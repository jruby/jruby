package org.jruby.ir.targets.indy;

import org.jruby.RubyRange;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class RangeObjectSite extends LazyObjectSite {
    protected final boolean exclusive;

    public RangeObjectSite(MethodType type, boolean exclusive) {
        super(type);

        this.exclusive = exclusive;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RangeObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int exclusive) {
        return new RangeObjectSite(type, exclusive == 1 ? true : false).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context, IRubyObject begin, IRubyObject end) throws Throwable {
        return RubyRange.newRange(context, begin, end, exclusive);
    }
}
