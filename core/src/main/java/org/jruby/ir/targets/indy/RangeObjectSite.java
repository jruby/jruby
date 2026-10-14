package org.jruby.ir.targets.indy;

import org.jruby.RubyRange;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class RangeObjectSite extends LazyObjectSite {
    public static final String RANGE_BEGINLESS = "rangeBeginless";
    public static final String RANGE_ENDLESS = "rangeEndless";
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
        return new RangeObjectSite(type, exclusive == 1).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context, IRubyObject begin, IRubyObject end) throws Throwable {
        return RubyRange.newRange(context, begin, end, exclusive);
    }

    public static final Handle BOOTSTRAP_LONG_LONG = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RangeObjectSite.class),
            "bootstrapFixnums",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, long.class, long.class, int.class),
            false);

    public static final Handle BOOTSTRAP_LONG = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RangeObjectSite.class),
            "bootstrapFixnums",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, long.class, int.class),
            false);

    public static CallSite bootstrapFixnums(MethodHandles.Lookup lookup, String name, MethodType type, long begin, long end, int exclusive) {
        return new FixnumRangeObjectSite(type, begin, end, false, false, exclusive != 0).bootstrap(lookup);
    }

    public static CallSite bootstrapFixnums(MethodHandles.Lookup lookup, String name, MethodType type, long beginOrEnd, int exclusive) {
        return new FixnumRangeObjectSite(type, beginOrEnd, 0, name.equals(RANGE_BEGINLESS), name.equals(RANGE_ENDLESS), exclusive != 0).bootstrap(lookup);
    }

    public static class FixnumRangeObjectSite extends RangeObjectSite {
        protected final FixnumRange range;

        public FixnumRangeObjectSite(MethodType type, long beginOrOnly, long end, boolean beginless, boolean endless, boolean exclusive) {
            super(type, exclusive);

            this.range = new FixnumRange(beginOrOnly, end, beginless, endless, exclusive);
        }

        public IRubyObject construct(ThreadContext context) throws Throwable {
            return context.runtime.cacheImmutableLiteral(range, (r) -> {
                if (r.beginless) {
                    return RubyRange.newBeginlessRange(context, r.beginOrOnly, r.exclusive);
                } else if (r.endless) {
                    return RubyRange.newEndlessRange(context, r.beginOrOnly, r.exclusive);
                }
                return RubyRange.newRange(context, r.beginOrOnly, r.end, r.exclusive);
            });
        }

        record FixnumRange(long beginOrOnly, long end, boolean beginless, boolean endless, boolean exclusive) {}
    }

    public static final Handle BOOTSTRAP_STRING_STRING = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RangeObjectSite.class),
            "bootstrapStrings",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class, String.class, String.class, int.class, int.class),
            false);

    public static CallSite bootstrapStrings(MethodHandles.Lookup lookup, String name, MethodType type, String begin, String beginEnc, int beginCR, String end, String endEnc, int endCR, int exclusive) {
        return new StringRangeObjectSite(type, StringBootstrap.bytelist(begin, beginEnc), beginCR, StringBootstrap.bytelist(end, endEnc), endCR, exclusive != 0).bootstrap(lookup);
    }

    public static class StringRangeObjectSite extends RangeObjectSite {
        protected final StringRange range;

        public StringRangeObjectSite(MethodType type, ByteList begin, int beginCR, ByteList end, int endCR, boolean exclusive) {
            super(type, exclusive);

            this.range = new StringRange(begin, beginCR, end, endCR, exclusive);
        }

        public IRubyObject construct(ThreadContext context) throws Throwable {
            return context.runtime.cacheImmutableLiteral(range, (r) -> RubyRange.newRange(
                    context,
                    IRRuntimeHelpers.newFrozenString(context, r.begin, r.beginCR),
                    IRRuntimeHelpers.newFrozenString(context, r.end, r.endCR),
                    r.exclusive));
        }

        record StringRange(ByteList begin, int beginCR, ByteList end, int endCR, boolean exclusive) {}
    }
}
