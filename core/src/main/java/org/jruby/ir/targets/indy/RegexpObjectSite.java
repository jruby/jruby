package org.jruby.ir.targets.indy;

import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
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
public class RegexpObjectSite extends LazyObjectSite {
    protected final Regexp regexp;

    public RegexpObjectSite(MethodType type, ByteList pattern, RegexpOptions options) {
        super(type);

        this.regexp = new Regexp(pattern, options);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(RegexpObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encodingName, int options) {
        return new RegexpObjectSite(type, StringBootstrap.bytelist(value, encodingName), RegexpOptions.fromEmbeddedOptions(options)).bootstrap(lookup);
    }

    // normal regexp
    public IRubyObject construct(ThreadContext context) {
        return context.runtime.cacheImmutableLiteral(regexp,
                (r) -> IRRuntimeHelpers.newLiteralRegexp(context, r.pattern, r.options));
    }

    record Regexp(ByteList pattern, RegexpOptions options) {}
}
