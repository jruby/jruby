package org.jruby.ir.targets;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.callMethodMissing;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.methodMissing;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class SelfInvokeSite extends InvokeSite {
    public SelfInvokeSite(MethodType type, String name, CallType callType) {
        super(type, name, callType);
    }

    public static Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(SelfInvokeSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
        String[] nameComponents = name.split(":");
        String methodName = JavaNameMangler.demangleMethodName(nameComponents[1]);
        CallType callType = nameComponents[0].equals("callFunctional") ? CallType.FUNCTIONAL : CallType.VARIABLE;
        InvokeSite site = new SelfInvokeSite(type, methodName, callType);

        return InvokeSite.bootstrap(site, lookup);
    }

    @Override
    public boolean methodMissing(CacheEntry entry, IRubyObject caller) {
        DynamicMethod method = entry.method;

        return method.isUndefined();
    }
}
