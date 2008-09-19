package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaObject;
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
        
        singleton.addMethod("new", new ConcreteNewMethod(singleton, Visibility.PUBLIC, oldNew));
        
        concreteJavaProxy.addMethod("initialize", new org.jruby.internal.runtime.methods.JavaMethod(concreteJavaProxy, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", args, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0, arg1, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0, arg1, arg2, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", args);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!");
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0, arg1);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                return RuntimeHelpers.invoke(context, self, "__jcreate!", arg0, arg1, arg2);
            }
        });
        
        return concreteJavaProxy;
    }
    
    public static class ConcreteNewMethod extends org.jruby.internal.runtime.methods.JavaMethod {
        private DynamicMethod oldNew;
            
        public ConcreteNewMethod(RubyModule implClass, Visibility visibility, DynamicMethod oldNew) {
            super(implClass, visibility);
            this.oldNew = oldNew;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", args, block);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", args, block);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", block);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", block);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0, block);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0, block);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0, arg1, block);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0, arg1, block);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0, arg1, arg2, block);
            
            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0, arg1, arg2, block);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", args);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", args);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy");

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!");
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0, arg1);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0, arg1);
            }

            return proxy;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy", arg0, arg1, arg2);

            if (!(proxy.dataGetStruct() instanceof JavaObject)) { // Need to initialize
                RuntimeHelpers.invoke(context, proxy, "__jcreate!", arg0, arg1, arg2);
            }

            return proxy;
        }
    }
}
