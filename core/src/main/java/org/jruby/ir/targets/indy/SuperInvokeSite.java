package org.jruby.ir.targets.indy;

import org.jruby.RubyClass;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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
public abstract class SuperInvokeSite extends SelfInvokeSite {
    protected final String superName;
    protected final boolean[] splatMap;

    public SuperInvokeSite(MethodType type, String superName, String splatmapString, String file, int line) {
        super(type, superName, CallType.SUPER, file, line);

        this.superName = superName;
        this.splatMap = IRRuntimeHelpers.decodeSplatmap(splatmapString);
    }

    public static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(SuperInvokeSite.class),
            "bootstrap",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class, int.class),
            false);

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String splatmapString, String file, int line) {
        List<String> targetAndMethod = StringSupport.split(name, ':');
        String superName = JavaNameMangler.demangleMethodName(targetAndMethod.get(1));

        InvokeSite site;

        switch (targetAndMethod.get(0)) {
            case "invokeInstanceSuper":
                site = new InstanceSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeInstanceSuperIter":
                site = new InstanceSuperIterInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeClassSuper":
                site = new ClassSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeClassSuperIter":
                site = new ClassSuperIterInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeModuleSuper":
                site = new ModuleSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeUnresolvedSuper":
                site = new UnresolvedSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeUnresolvedSuperIter":
                site = new UnresolvedSuperIterInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeZSuper":
                site = new ZSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            default:
                throw new RuntimeException("invalid super call: " + name);
        }

        return InvokeSite.bootstrap(site, lookup);
    }

    public abstract IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable;

    public abstract IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable;
}