package org.jruby.ir.targets.indy;

import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class FloatObjectSite extends LazyObjectSite {
    private final double value;

    public FloatObjectSite(MethodType type, double value) {
        super(type);

        this.value = value;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(FloatObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, double.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, double value) {
        return new FloatObjectSite(type, value).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        return RubyFloat.newFloat(context.runtime, value);
    }
}
