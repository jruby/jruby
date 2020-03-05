package org.jruby.ir.targets.indy;

import org.jruby.RubyClass;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.CallType;
import org.jruby.util.JavaNameMangler;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.jruby.util.CodegenUtils.sig;

/**
* Created by headius on 10/23/14.
*/
public abstract class ResolvedSuperInvokeSite extends SelfInvokeSite {
    protected final String superName;
    protected final boolean[] splatMap;

    public ResolvedSuperInvokeSite(MethodType type, String superName, String splatmapString, String file, int line) {
        super(type, superName, CallType.SUPER, file, line);

        this.superName = superName;
        this.splatMap = IRRuntimeHelpers.decodeSplatmap(splatmapString);
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, String splatmapString, String file, int line) {
        String[] targetAndMethod = name.split(":");
        String superName = JavaNameMangler.demangleMethodName(targetAndMethod[1]);

        InvokeSite site;

        switch (targetAndMethod[0]) {
            case "invokeInstanceSuper":
                site = new InstanceSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            case "invokeClassSuper":
                site = new ClassSuperInvokeSite(type, superName, splatmapString, file, line);
                break;
            default:
                throw new RuntimeException("invalid super call: " + name);
        }

        return InvokeSite.bootstrap(site, lookup);
    }

    // FIXME: indy cached version was not doing splat mapping; revert to slow logic for now

//    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
//        // TODO: mostly copy of org.jruby.ir.targets.indy.InvokeSite because of different target class logic
//
//        RubyClass selfClass = pollAndGetClass(context, self);
//        RubyClass superClass = getSuperClass(definingModule);
//        SwitchPoint switchPoint = (SwitchPoint) superClass.getInvalidator().getData();
//        CacheEntry entry = superClass.searchWithCache(methodName);
//        DynamicMethod method = entry.method;
//
//        if (methodMissing(entry, caller)) {
//            return callMethodMissing(entry, callType, context, self, methodName, args, block);
//        }
//
//        MethodHandle mh = getHandle(superClass, this, method);
//
//        updateInvocationTarget(mh, self, selfClass, entry, switchPoint);
//
//        return method.call(context, self, superClass, methodName, args, block);
//    }
//
//    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
//        // TODO: get rid of caller
//
//        context.callThreadPoll();
//
//        RubyClass superClass = getSuperClass(definingModule);
//        String name = methodName;
//        CacheEntry entry = cache;
//
//        if (entry.typeOk(superClass)) {
//            return entry.method.call(context, self, superClass, name, splatArguments(args, splatMap), block);
//        }
//
//        entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
//
//        DynamicMethod method = entry.method;
//
//        if (method.isUndefined()) {
//            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, splatArguments(args, splatMap), block);
//        }
//
//        cache = entry;
//
//        return method.call(context, self, superClass, name, splatArguments(args, splatMap), block);
//    }

    protected abstract RubyClass getSuperClass(RubyClass definingModule);
}