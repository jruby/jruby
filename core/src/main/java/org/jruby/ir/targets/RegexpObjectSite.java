package org.jruby.ir.targets;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class RegexpObjectSite extends LazyObjectSite {
    protected final RegexpOptions options;

    public RegexpObjectSite(MethodType type, int embeddedOptions) {
        super(type);

        this.options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RegexpObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int options) {
        return new RegexpObjectSite(type, options).bootstrap(lookup);
    }

    // normal regexp
    public IRubyObject construct(ThreadContext context, ByteList pattern) {
        RubyRegexp regexp = IRRuntimeHelpers.newLiteralRegexp(context, pattern, options);

        return regexp;
    }
}
