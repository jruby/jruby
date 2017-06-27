package org.jruby.ir.targets;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class NormalInvokeSite extends InvokeSite {
    CacheEntry cache;

    public NormalInvokeSite(MethodType type, String name, boolean literalClosure, String file, int line) {
        super(type, name, CallType.NORMAL, literalClosure, file, line);
    }

    public static Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(NormalInvokeSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, String.class, int.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int closureInt, String file, int line) {
        boolean literalClosure = closureInt != 0;
        String methodName = StringSupport.split(name, ':').get(1);
        InvokeSite site = new NormalInvokeSite(type, JavaNameMangler.demangleMethodName(methodName), literalClosure, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }

    @Override
    public boolean methodMissing(CacheEntry entry, IRubyObject caller) {
        DynamicMethod method = entry.method;

        return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }
}
