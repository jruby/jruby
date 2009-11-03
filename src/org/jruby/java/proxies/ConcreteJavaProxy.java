package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
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
        
        concreteJavaProxy.addMethod("initialize", new org.jruby.internal.runtime.methods.JavaMethod(concreteJavaProxy, Visibility.PUBLIC) {
            private final CallSite jcreateSite = MethodIndex.getFunctionalCallSite("__jcreate!");
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return jcreateSite.call(context, self, self, args, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
                return jcreateSite.call(context, self, self, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
                return jcreateSite.call(context, self, self, arg0, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
                return jcreateSite.call(context, self, self, arg0, arg1, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
                return jcreateSite.call(context, self, self, arg0, arg1, arg2, block);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
                return jcreateSite.call(context, self, self, args);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return jcreateSite.call(context, self, self);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
                return jcreateSite.call(context, self, self, arg0);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
                return jcreateSite.call(context, self, self, arg0, arg1);
            }
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
                return jcreateSite.call(context, self, self, arg0, arg1, arg2);
            }
        });
        
        return concreteJavaProxy;
    }
}
