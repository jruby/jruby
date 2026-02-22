package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Signature;
import org.jruby.runtime.CallType;
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

    private static Signature BOOTSTRAP_BASE_SIGNATURE =
            Signature.returning(InvokeSite.class)
                    .appendArg("lookup", MethodHandles.Lookup.class)
                    .appendArg("name", String.class)
                    .appendArg("methodType", MethodType.class);
    private static Signature BOOTSTRAP_SIGNATURE =
            BOOTSTRAP_BASE_SIGNATURE
                    .appendArg("literalClosure", int.class)
                    .appendArg("flags", int.class)
                    .appendArg("file", String.class)
                    .appendArg("line", int.class);
    private static Signature BOOTSTRAP_KWARGS_SIGNATURE =
            BOOTSTRAP_BASE_SIGNATURE
                    .changeReturn(CallSite.class)
                    .appendArg("kwargKeys", String.class)
                    .appendArg("literalClosure", int.class)
                    .appendArg("flags", int.class)
                    .appendArg("file", String.class)
                    .appendArg("line", int.class);

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SelfInvokeSite.class),
            "bootstrap",
            sig(BOOTSTRAP_SIGNATURE.type()),
            false);

    public static final Handle BOOTSTRAP_KWARGS = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SelfInvokeSite.class),
            "bootstrapKwargs",
            sig(BOOTSTRAP_KWARGS_SIGNATURE.type()),
            false);

    public static InvokeSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, int closureInt, int flags, String file, int line) {
        boolean literalClosure = closureInt != 0;
        List<String> nameComponents = StringSupport.split(name, ':');
        String methodName = JavaNameMangler.demangleMethodName(nameComponents.get(1));
        CallType callType = nameComponents.get(0).equals("callFunctional") ? CallType.FUNCTIONAL : CallType.VARIABLE;
        InvokeSite site = new SelfInvokeSite(type, methodName, callType, callType, literalClosure, flags, file, line);

        InvokeSite.bootstrap(site, lookup);

        return site;
    }

    public static CallSite bootstrapKwargs(MethodHandles.Lookup lookup, String name, MethodType type, String kwargKeys, int closureInt, int flags, String file, int line) {
        return wrappedKwargsInvokeSite(lookup, name, type, kwargKeys, closureInt, flags, file, line, false, SelfInvokeSite::bootstrap);
    }
}
