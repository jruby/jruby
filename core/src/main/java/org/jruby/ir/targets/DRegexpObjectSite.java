package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
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
    private static final AtomicReferenceFieldUpdater UPDATER = AtomicReferenceFieldUpdater.newUpdater(DRegexpObjectSite.class, RubyRegexp.class, "cache");

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
                .collect("parts", "part.*")
                .binder();
    }

    // dynamic regexp
    public RubyRegexp construct(ThreadContext context, RubyString[] pieces) throws Throwable {
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        if (options.isOnce()) {
            if (cache != null) {
                // we cached a value, so re-call this site's target handle to get it
                return cache;
            }

            // we don't care if this succeeds, just that it only gets set once
            UPDATER.compareAndSet(this, null, cache);

            setTarget(Binder.from(type()).dropAll().constant(cache));
        }

        return re;
    }
}
