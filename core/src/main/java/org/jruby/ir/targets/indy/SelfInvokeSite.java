package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StringSupport;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.List;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public class SelfInvokeSite extends InvokeSite {
    public SelfInvokeSite(MethodType type, String name, CallType callType, boolean literalClosure, int flags, String file, int line) {
        super(type, name, callType, literalClosure, flags, file, line);
    }

    public SelfInvokeSite(MethodType type, String name, CallType callType, int flags, String file, int line) {
        this(type, name, callType, false, flags, file, line);
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
        InvokeSite site = new SelfInvokeSite(type, methodName, callType, literalClosure, flags, file, line);

        InvokeSite.bootstrap(site, lookup);

        return site;
    }

    public static CallSite bootstrapKwargs(MethodHandles.Lookup lookup, String name, MethodType type, String kwargKeys, int closureInt, int flags, String file, int line) {
        String[] kwargKeysArray = kwargKeys.split(";");

        int argCount = type.parameterCount();
        boolean block = false;

        argCount--; // context
        argCount -= 1; // self
        if (type.lastParameterType() == Block.class) {
            block = true;
            argCount--; // block
        }

        int argIndex = 2;
        int normalArgCount = argCount - kwargKeysArray.length;
        int kwargsIndex = argIndex + normalArgCount;

        MethodType passthroughType = type
                .dropParameterTypes(kwargsIndex, kwargsIndex + kwargKeysArray.length)
                .insertParameterTypes(argIndex + normalArgCount, IRubyObject.class);

        // folder to construct kwargs from args
        MethodHandle foldKwargs;
        {
            Binder binder = Binder.from(type);

            // collect kwarg values
            binder = binder.collect(kwargsIndex, kwargKeysArray.length, IRubyObject[].class);

            // drop self and normal args
            binder = binder.drop(1, 1 + normalArgCount);

            // drop block if present
            if (block) binder = binder.dropLast();

            // insert kwarg constructor
            binder = binder.prepend(new Helpers.KwargConstructor(kwargKeysArray));

            foldKwargs = binder.invokeVirtualQuiet("constructKwargs");
        }

        InvokeSite invokeSite = bootstrap(lookup, name, passthroughType, closureInt, flags, file, line);
        InvokeSite.bootstrap(invokeSite, lookup);

        // fold, permute
        int[] permutes = new int[2 + normalArgCount + 1 + (block ? 1 : 0)];
        // slide context, self, normal args over
        int i;
        for (i = 0; i < 2 + normalArgCount; i++) {
            permutes[i] = i + 1;
        }
        // move kwargs
        permutes[i++] = 0;
        // drop rest except block
        if (block) permutes[i] = permutes.length - 1;

        MethodHandle wrappedSite = Binder.from(type)
                .fold(foldKwargs)
                .drop(1 + argIndex + normalArgCount, kwargKeysArray.length)
                .permute(permutes)
                .invoke(invokeSite.dynamicInvoker());

        return new ConstantCallSite(wrappedSite);
    }
}
