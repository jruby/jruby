/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
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
        Ruby runtime = context.runtime;
        
        RubyClass ifcJavaProxy = runtime.defineClass(
                "InterfaceJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new InterfaceJavaProxy(runtime, klazz);
            }
        });

        RubyClass javaIfcExtender = runtime.defineClass(
                "JavaInterfaceExtender", runtime.getObject(), runtime.getObject().getAllocator());
        javaIfcExtender.defineAnnotatedMethods(JavaInterfaceExtender.class);

        return ifcJavaProxy;
    }

    public static class JavaInterfaceExtender {
        @JRubyMethod
        public static IRubyObject initialize(ThreadContext context, IRubyObject self, IRubyObject javaClassName, Block block) {
            Ruby runtime = context.runtime;
            
            self.getInstanceVariables().setInstanceVariable("@java_class", JavaClass.forNameVerbose(runtime, javaClassName.asJavaString()));
            self.getInstanceVariables().setInstanceVariable("@block", RubyProc.newProc(runtime, block, block.type));

            return runtime.getNil();
        }

        @JRubyMethod
        public static IRubyObject extend_proxy(ThreadContext context, IRubyObject self, IRubyObject proxyClass) {
            return proxyClass.callMethod(context, "class_eval", IRubyObject.NULL_ARRAY,
                    ((RubyProc)self.getInstanceVariables().getInstanceVariable("@block")).getBlock());
        }
    }
}
