package org.jruby.ir.targets.indy;

import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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

public class ComplexObjectSite extends LazyObjectSite {
    private final Number value;

    public ComplexObjectSite(MethodType type, Number value) {
        super(type);
        this.value = value;
    }

    public static final Handle BOOTSTRAP_LONG = bootstrapHandle(long.class);
    public static final Handle BOOTSTRAP_DOUBLE = bootstrapHandle(double.class);
    public static final Handle BOOTSTRAP_BIGNUM = bootstrapHandle(String.class);

    private static Handle bootstrapHandle(Class<?> valueType) {
        return new Handle(Opcodes.H_INVOKESTATIC, p(ComplexObjectSite.class), "bootstrap",
                sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, valueType), false);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, long value) {
        return new ComplexObjectSite(type, value).bootstrap(lookup);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, double value) {
        return new ComplexObjectSite(type, value).bootstrap(lookup);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value) {
        return new ComplexObjectSite(type, new BigInteger(value)).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        IRubyObject imaginary;
        if (value instanceof Long l) {
            imaginary = RubyFixnum.newFixnum(context.runtime, l);
        } else if (value instanceof Double d) {
            imaginary = RubyFloat.newFloat(context.runtime, d);
        } else {
            imaginary = RubyBignum.newBignum(context.runtime, (BigInteger) value);
        }
        return IRRuntimeHelpers.newComplexRaw(context, imaginary);
    }
}
