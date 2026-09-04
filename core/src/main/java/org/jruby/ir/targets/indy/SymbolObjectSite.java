package org.jruby.ir.targets.indy;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class SymbolObjectSite extends LazyObjectSite {
    protected final Symbol symbol;

    public SymbolObjectSite(MethodType type, ByteList bytes) {
        super(type);

        this.symbol = new Symbol(bytes);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SymbolObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encoding) {
        return new SymbolObjectSite(type, StringBootstrap.bytelist(value, encoding)).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        return context.runtime.cacheImmutableLiteral(symbol,
                (s) -> asSymbol(context, s.bytes));
    }

    protected record Symbol(ByteList bytes) {}
}
