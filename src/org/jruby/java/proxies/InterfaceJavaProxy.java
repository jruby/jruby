/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtilities;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class InterfaceJavaProxy extends JavaProxy {
    public InterfaceJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public static RubyClass createInterfaceJavaProxy(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        RubyClass ifcJavaProxy = runtime.defineClass(
                "InterfaceJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new InterfaceJavaProxy(runtime, klazz);
            }
        });

        RubyClass singleton = ifcJavaProxy.getSingletonClass();

        singleton.addMethod("new", new JavaMethod(singleton, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                assert self instanceof RubyClass : "InterfaceJavaProxy.new defined on non-class ";
                RubyClass rubyClass = (RubyClass)self;

                IRubyObject proxy = rubyClass.allocate();
                JavaClass javaClass = (JavaClass)RuntimeHelpers.invoke(context, proxy.getMetaClass(), "java_class");
                IRubyObject proxyInstance = Java.new_proxy_instance2(
                        self,
                        self,
                        RubyArray.newArray(context.getRuntime(), javaClass),
                        block);
                JavaUtilities.set_java_object(proxy, proxy, proxyInstance);

                RuntimeHelpers.invoke(context, self, "initialize", args, block);

                return proxy;
            }
        });

        return ifcJavaProxy;
    }
}
