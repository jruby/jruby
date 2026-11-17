package org.jruby.ir.targets.indy;

import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.util.CodegenUtils.p;

public class CallInfoBootstrap {
    public static final Handle CALL_INFO_BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(CallInfoBootstrap.class),
            "callInfoBootstrap",
            Bootstrap.BOOTSTRAP_INT_SIG,
            false);

    public static CallSite callInfoBootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int callInfo) throws Throwable {
        MethodHandle handle;
        if (callInfo == 0) {
            handle = lookup.findStatic(ThreadContext.class, "clearCallInfo", methodType(void.class, ThreadContext.class));
        } else {
            handle = lookup.findStatic(IRRuntimeHelpers.class, "setCallInfo", methodType(void.class, ThreadContext.class, int.class));
            handle = insertArguments(handle, 1, callInfo);
        }

        return new ConstantCallSite(handle);
    }
}
