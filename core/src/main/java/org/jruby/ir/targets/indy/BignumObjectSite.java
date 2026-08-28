package org.jruby.ir.targets.indy;

import org.jruby.RubyBignum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class BignumObjectSite extends LazyObjectSite {
    private final BigInteger value;

    public BignumObjectSite(MethodType type, String value) {
        super(type);

        this.value = new BigInteger(value);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(BignumObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value) {
        return new BignumObjectSite(type, value).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        return RubyBignum.newBignum(context.runtime, value);
    }
}
