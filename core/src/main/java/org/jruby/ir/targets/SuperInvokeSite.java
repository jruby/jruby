package org.jruby.ir.targets;

import com.headius.invokebinder.SmartBinder;
import org.jruby.RubyClass;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public abstract class SuperInvokeSite extends InvokeSite {
    public SuperInvokeSite(MethodType type, String name) {
        super(type, name, CallType.SUPER);
    }

    public static final Handle BOOTSTRAP = new Handle(Opcodes.H_INVOKESTATIC, p(SuperInvokeSite.class), "bootstrap", sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class));

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String splatmapString) {
        String[] targetAndMethod = name.split(":");
        String superName = JavaNameMangler.demangleMethodName(targetAndMethod[1]);

        InvokeSite site;

        switch (targetAndMethod[0]) {
            case "invokeInstanceSuper":
                site = new InstanceSuperInvokeSite(type, name);
                break;
            case "invokeClassSuper":
                site = new ClassSuperInvokeSite(type, name);
                break;
            case "invokeUnresolvedSuper":
                site = new UnresolvedSuperInvokeSite(type, name);
                break;
            case "invokeZSuper":
                site = new ZSuperInvokeSite(type, name);
                break;
            default:
                throw new RuntimeException("invalid super call: " + name);
        }

        MethodHandle handle;

        boolean[] splatMap = IRRuntimeHelpers.decodeSplatmap(splatmapString);

        SmartBinder binder = SmartBinder.from(site.signature)
                .insert(
                        0,
                        arrayOf("site", "name", "splatMap"),
                        arrayOf(SuperInvokeSite.class, String.class, boolean[].class),
                        site, superName, splatMap);

        if (site.arity > 0) {
            binder = binder
                    .collect("args", "arg[0-9]+");
        }

        handle = binder.invokeVirtualQuiet(lookup, "invoke").handle();

        site.setTarget(handle);

        return site;
    }

    public abstract IRubyObject invoke(String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable;
}