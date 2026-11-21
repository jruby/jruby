package org.jruby.ir.targets.indy;

import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/** Caches rational literals already reduced by the Rational operand, with a positive denominator. */
public class RationalObjectSite extends LazyObjectSite {
    private final String numerator;
    private final String denominator;

    public RationalObjectSite(MethodType type, String numerator, String denominator) {
        super(type);
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static final Handle BOOTSTRAP = bootstrapHandle("bootstrap");
    public static final Handle BOOTSTRAP_COMPLEX = bootstrapHandle("bootstrapComplex");

    private static Handle bootstrapHandle(String name) {
        return new Handle(Opcodes.H_INVOKESTATIC, p(RationalObjectSite.class), name,
                sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class), false);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String numerator, String denominator) {
        return new RationalObjectSite(type, numerator, denominator).bootstrap(lookup);
    }

    public static CallSite bootstrapComplex(MethodHandles.Lookup lookup, String name, MethodType type, String numerator, String denominator) {
        return new RationalComplexObjectSite(type, numerator, denominator).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        // Parse only during initialization; LazyObjectSite replaces this target with the cached value.
        return IRRuntimeHelpers.newRationalRaw(context, numerator, denominator);
    }

    public static class RationalComplexObjectSite extends RationalObjectSite {
        public RationalComplexObjectSite(MethodType type, String numerator, String denominator) {
            super(type, numerator, denominator);
        }

        @Override
        public IRubyObject construct(ThreadContext context) {
            return IRRuntimeHelpers.newComplexRaw(context, super.construct(context));
        }
    }
}
