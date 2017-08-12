package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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
    protected final int embeddedOptions;
    private volatile RubyRegexp cache;
    private static final AtomicReferenceFieldUpdater UPDATER = AtomicReferenceFieldUpdater.newUpdater(DRegexpObjectSite.class, RubyRegexp.class, "cache");

    public DRegexpObjectSite(MethodType type, int embeddedOptions) {
        super(type);

        this.embeddedOptions = embeddedOptions;
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(DRegexpObjectSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int options) {
        return new DRegexpObjectSite(type, options).bootstrap(lookup);
    }

    @Override
    public Binder prepareBinder() {
        if (type().parameterCount() - 1 <= MAX_ARITY) { // 5 arity max minus ThreadContext
            return Binder
                    .from(type());
        }

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

    public RubyRegexp construct(ThreadContext context, RubyString[] pieces) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, pieces, options);

        return constructAndCache(re, options);
    }

    private static final int MAX_ARITY = 5;

    public RubyRegexp construct(ThreadContext context, RubyString piece) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, piece, options);

        return constructAndCache(re, options);
    }

    public RubyRegexp construct(ThreadContext context, RubyString piece0, RubyString piece1) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, piece0, piece1, options);

        return constructAndCache(re, options);
    }

    public RubyRegexp construct(ThreadContext context, RubyString piece0, RubyString piece1, RubyString piece2) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, piece0, piece1, piece2, options);

        return constructAndCache(re, options);
    }

    public RubyRegexp construct(ThreadContext context, RubyString piece0, RubyString piece1, RubyString piece2, RubyString piece3) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, piece0, piece1, piece2, piece3, options);

        return constructAndCache(re, options);
    }

    public RubyRegexp construct(ThreadContext context, RubyString piece0, RubyString piece1, RubyString piece2, RubyString piece3, RubyString piece4) throws Throwable {
        RubyRegexp cache = this.cache;
        if (cache != null) return cache;

        int options = this.embeddedOptions;

        RubyRegexp re = IRRuntimeHelpers.newDynamicRegexp(context, piece0, piece1, piece2, piece3, piece4, options);

        return constructAndCache(re, options);
    }

    private RubyRegexp constructAndCache(RubyRegexp re, int options) {
        if (RegexpOptions.isOnce(options)) {
            // we don't care if this succeeds, just that it only gets set once
            UPDATER.compareAndSet(this, null, re);

            // use the set-once cache and constantize the site
            setTarget(Binder.from(type()).dropAll().constant(cache));
        }

        return re;
    }
}
