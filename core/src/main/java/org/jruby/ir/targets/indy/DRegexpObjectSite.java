package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class DRegexpObjectSite extends ConstructObjectSite {
    protected final RegexpOptions options;
    private volatile RubyRegexp cache;
    private static final AtomicReferenceFieldUpdater CACHE_UPDATER = AtomicReferenceFieldUpdater.newUpdater(DRegexpObjectSite.class, RubyRegexp.class, "cache");

    public DRegexpObjectSite(MethodType type, int embeddedOptions) {
        super(type);

        options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(DRegexpObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int options) {
        return new DRegexpObjectSite(type, options).bootstrap(lookup);
    }

    @Override
    public Binder prepareBinder() {
        // collect dregexp args into an array

        String[] argNames = new String[type().parameterCount()];
        Class[] argTypes = new Class[argNames.length];

        argNames[0] = "context";
        argTypes[0] = ThreadContext.class;

        for (int i = 1; i < argNames.length; i++) {
            argNames[i] = "part" + i;
            argTypes[i] = RubyString.class;
        }

        // "once" deregexp must be handled on the call side
        return SmartBinder
                .from(RubyRegexp.class, argNames, argTypes)
                .collect("parts", "part.*", Helpers.constructRubyStringArrayHandle(argNames.length - 1))
                .binder();
    }

    @Override
    public String initialTarget() {
        if (options.isOnce()) return "constructOnce";

        return super.initialTarget();
    }

    // dynamic regexp
    public RubyRegexp construct(ThreadContext context, RubyString[] pieces) {
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, pieces);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    // dynamic regexp cached once
    public RubyRegexp constructOnce(ThreadContext context, RubyString[] pieces) {
        RubyRegexp re = construct(context, pieces);

        // permanently set target to new regexp iff we are the first to assign it
        if (CACHE_UPDATER.compareAndSet(this, null, re)) {
            setTarget(Binder.from(type()).dropAll().constant(re));

            return re;
        }

        // cache was assigned on another thread, re-get cache and return
        return this.cache;
    }
}
