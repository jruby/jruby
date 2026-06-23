package org.jruby.ir.targets.simple;

import org.jruby.ir.targets.indy.InvokeSite;
import org.jruby.runtime.CallType;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StringSupport;
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
public class NormalInvokeSite extends InvokeSite {

    public NormalInvokeSite(MethodType type, String name, boolean literalClosure, int flags, String file, int line) {
        super(type, name, CallType.NORMAL, literalClosure, flags, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(NormalInvokeSite.class),
            "bootstrap",
            sig(InvokeSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, int.class, String.class, int.class),
            false);

    public static final Handle BOOTSTRAP_KWARGS = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(NormalInvokeSite.class),
            "bootstrapKwargs",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class, int.class, String.class, int.class),
            false);

    public static InvokeSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int closureInt, int flags, String file, int line) {
        boolean literalClosure = closureInt != 0;
        String methodName = JavaNameMangler.demangleMethodName(StringSupport.split(name, ':').get(1));

        return newSite(lookup, methodName, type, literalClosure, flags, file, line);
    }

    public static CallSite bootstrapKwargs(MethodHandles.Lookup lookup, String name, MethodType type, String kwargKeys, int closureInt, int flags, String file, int line) {
        return wrappedKwargsInvokeSite(lookup, name, type, kwargKeys, closureInt, flags, file, line, true, NormalInvokeSite::bootstrap);
    }

    public static NormalInvokeSite newSite(MethodHandles.Lookup lookup, String methodName, MethodType type, boolean literalClosure, int flags, String file, int line) {
        NormalInvokeSite site = new NormalInvokeSite(type, methodName, literalClosure, flags, file, line);

        InvokeSite.bootstrap(site, lookup);

        return site;
    }
}
