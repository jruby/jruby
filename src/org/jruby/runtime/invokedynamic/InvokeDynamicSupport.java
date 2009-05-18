package org.jruby.runtime.invokedynamic;

import java.dyn.CallSite;
//import java.dyn.Linkage;
//import java.dyn.MethodHandle;
//import java.dyn.MethodHandles;
//import java.dyn.MethodType;
import java.dyn.Linkage;
import java.dyn.MethodHandle;
import java.dyn.JavaMethodHandle;
import java.dyn.MethodHandles;
import java.dyn.MethodType;
//import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.RubyClass;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
//import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Type;

public class InvokeDynamicSupport {
    public static CallSite bootstrap(Class caller, String name, MethodType type) {
        CallSite site = new CallSite(caller, name, type);
        MethodHandle target = new GuardedRubyMethodHandle(CacheEntry.NULL_CACHE, site);
        site.setTarget(target);
        return site;
    }
    
    public static void registerBootstrap(Class cls) {
          MethodType bootstrapType = MethodType.make(CallSite.class, Class.class, String.class, MethodType.class);
      MethodHandle bootstrap
        = MethodHandles.lookup().findStatic(InvokeDynamicSupport.class, "bootstrap", bootstrapType);
      Linkage.registerBootstrapMethod(cls, bootstrap);
    }

    
    public static void installBytecode(MethodVisitor method, String classname) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(method);
        mv.ldc(c(classname));
        mv.invokestatic(p(Class.class), "forName", sig(Class.class, params(String.class)));
        mv.invokestatic(p(InvokeDynamicSupport.class), "registerBootstrap", sig(void.class, Class.class));
    }

    public static class GuardedRubyMethodHandle extends JavaMethodHandle {
        final CacheEntry entry;
        final CallSite site;

        public GuardedRubyMethodHandle(CacheEntry entry, CallSite site) {
            super(DEFAULT);
            this.entry = entry;
            this.site = site;
        }

        public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) {
            RubyClass selfClass = pollAndGetClass(context, self);
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0);
            } else {
                CacheEntry newEntry = selfClass.searchWithCache(name);
                site.setTarget(new GuardedRubyMethodHandle(newEntry, site));
                return newEntry.method.call(context, self, selfClass, name, arg0);
            }
        }

        public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
            context.pollThreadEvents();
            return self.getMetaClass();
        }

        private static final MethodHandle DEFAULT =
                MethodHandles.lookup().findVirtual(GuardedRubyMethodHandle.class, "invoke",
                    MethodType.make(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    }
}
