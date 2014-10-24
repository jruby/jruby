package org.jruby.ir.targets;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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
public class SymbolObjectSite extends LazyObjectSite {
    private final String value;

    public SymbolObjectSite(MethodType type, String value) {
        super(type);

        this.value = value;
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(SymbolObjectSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value) {
        return new SymbolObjectSite(type, value).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        return context.runtime.newSymbol(value);
    }
}
