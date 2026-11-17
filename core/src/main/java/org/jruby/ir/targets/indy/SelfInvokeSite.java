package org.jruby.ir.targets.indy;

import org.jruby.internal.runtime.methods.DynamicMethod;
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
import java.util.List;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class SelfInvokeSite extends InvokeSite {
    public SelfInvokeSite(MethodType type, String name, CallType structuralCallType, CallType visibilityCallType, boolean literalClosure, int flags, String file, int line) {
        super(type, name, structuralCallType, visibilityCallType, literalClosure, flags, file, line);
    }

    public SelfInvokeSite(MethodType type, String name, CallType callType, int flags, String file, int line) {
        this(type, name, callType, callType, false, flags, file, line);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SelfInvokeSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class, int.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int closureInt, int flags, String file, int line) {
        boolean literalClosure = closureInt != 0;
        List<String> nameComponents = StringSupport.split(name, ':');
        String methodName = JavaNameMangler.demangleMethodName(nameComponents.get(1));
        CallType callType = nameComponents.get(0).equals("callFunctional") ? CallType.FUNCTIONAL : CallType.VARIABLE;
        InvokeSite site = new SelfInvokeSite(type, methodName, callType, callType, literalClosure, flags, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }
}
