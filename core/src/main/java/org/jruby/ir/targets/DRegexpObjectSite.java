package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RegexpOptions;
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
public class DRegexpObjectSite extends ConstructObjectSite {
    protected final RegexpOptions options;

    public DRegexpObjectSite(MethodType type, int embeddedOptions) {
        super(type);

        options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(DRegexpObjectSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class));

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
    public RubyRegexp construct(ThreadContext context, RubyString[] pieces) {
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }
}
