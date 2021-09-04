package org.jruby.ir.targets.simple;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.targets.indy.InvokeSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
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

    public NormalInvokeSite(MethodType type, String name, boolean literalClosure, String file, int line) {
        super(type, name, CallType.NORMAL, literalClosure, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(NormalInvokeSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int closureInt, String file, int line) {
        boolean literalClosure = closureInt != 0;
        String methodName = JavaNameMangler.demangleMethodName(StringSupport.split(name, ':').get(1));

        return newSite(lookup, methodName, type, literalClosure, file, line);
    }

    public static NormalInvokeSite newSite(MethodHandles.Lookup lookup, String methodName, MethodType type, boolean literalClosure, String file, int line) {
        NormalInvokeSite site = new NormalInvokeSite(type, methodName, literalClosure, file, line);

        InvokeSite.bootstrap(site, lookup);

        return site;
    }

    @Override
    public boolean methodMissing(CacheEntry entry, IRubyObject caller) {
        DynamicMethod method = entry.method;

        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
