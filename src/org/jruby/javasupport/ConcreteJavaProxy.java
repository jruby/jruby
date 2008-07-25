package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ConcreteJavaProxy extends JavaProxy {
    public ConcreteJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    public static RubyClass createConcreteJavaProxy(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        RubyClass concreteJavaProxy = runtime.defineClass("ConcreteJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(),
                new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new ConcreteJavaProxy(runtime, klazz);
            }
        });
        
        RubyClass singleton = concreteJavaProxy.getSingletonClass();
        
        final DynamicMethod oldNew = singleton.searchMethod("new");
        
        singleton.addMethod("new", new org.jruby.internal.runtime.methods.JavaMethod(singleton, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", args, block);
                if (proxy.dataGetStruct() instanceof JavaObject) {
                    // already initialized
                } else {
                    // not initialized yet, call __jcreate!
                    RuntimeHelpers.invoke(context, proxy, "__jcreate!", args);
                }
                
                return proxy;
            }
        });
        
        concreteJavaProxy.addMethod("initialize", new org.jruby.internal.runtime.methods.JavaMethod(concreteJavaProxy, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", args);
            }
        });
        
        return concreteJavaProxy;
    }
}
