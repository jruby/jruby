package org.jruby.ir.targets.indy;

import org.jruby.runtime.CallType;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.constant;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class CallSiteCacheBootstrap {
    public static final Handle CALLSITE = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(CallSiteCacheBootstrap.class),
            "callSite",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class),
            false);

    public static CallSite callSite(MethodHandles.Lookup lookup, String name, MethodType type, String id, int callType) {
        return new ConstantCallSite(constant(CachingCallSite.class, callSite(id, callType)));
    }

    private static CachingCallSite callSite(String id, int callType) {
        switch (CallType.fromOrdinal(callType)) {
            case NORMAL:
                return new MonomorphicCallSite(id);
            case FUNCTIONAL:
                return new FunctionalCachingCallSite(id);
            case VARIABLE:
                return new VariableCachingCallSite(id);
            default:
                throw new RuntimeException("BUG: Unexpected call type " + callType + " in JVM6 invoke logic");
        }
    }
}
