package org.jruby.ir.targets.indy;

import org.jruby.RubyEncoding;
import org.jruby.ir.runtime.IRRuntimeHelpers;
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

public class SymbolProcObjectSite extends LazyObjectSite {
    private final String value;
    private final String encoding;

    public SymbolProcObjectSite(MethodType type, String value, String encoding) {
        super(type);

        this.value = value;
        this.encoding = encoding;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SymbolProcObjectSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String value, String encoding) {
        return new SymbolProcObjectSite(type, value, encoding).bootstrap(lookup);
    }

    public IRubyObject construct(ThreadContext context) {
        var symbol = asSymbol(context, new ByteList(RubyEncoding.encodeISO(value),
                IRRuntimeHelpers.retrieveJCodingsEncoding(context, encoding), false));

        return IRRuntimeHelpers.newSymbolProc(context, symbol);
    }
}
