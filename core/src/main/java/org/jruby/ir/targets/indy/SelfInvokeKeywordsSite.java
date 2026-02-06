package org.jruby.ir.targets.indy;

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
public class SelfInvokeKeywordsSite extends InvokeSite {
    private final String[] keywords;

    public SelfInvokeKeywordsSite(MethodType type, String name, CallType callType, String[] keywords, boolean literalClosure, int flags, String file, int line) {
        super(type, name, callType, literalClosure, flags, file, line);
        this.keywords = keywords;
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SelfInvokeKeywordsSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, int.class, int.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String keywords, int closureInt, int flags, String file, int line) {
        boolean literalClosure = closureInt != 0;
        List<String> nameComponents = StringSupport.split(name, ':');
        String methodName = JavaNameMangler.demangleMethodName(nameComponents.get(1));
        CallType callType = nameComponents.get(0).equals("callFunctional") ? CallType.FUNCTIONAL : CallType.VARIABLE;
        InvokeSite site = new SelfInvokeKeywordsSite(type, methodName, callType, keywords.split(","), literalClosure, flags, file, line);

        return InvokeSite.bootstrap(site, lookup);
    }
}
