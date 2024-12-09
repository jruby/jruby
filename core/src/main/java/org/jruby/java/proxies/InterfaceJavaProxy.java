/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Define.defineClass;

/**
 *
 * @author headius
 */
public class InterfaceJavaProxy extends JavaProxy {

    public InterfaceJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    public static RubyClass createInterfaceJavaProxy(ThreadContext context, RubyClass Object, RubyClass JavaProxy) {
        defineClass(context, "JavaInterfaceExtender", Object, Object.getAllocator()).
                defineMethods(context, JavaInterfaceExtender.class);

        return defineClass(context, "InterfaceJavaProxy", JavaProxy, InterfaceJavaProxy::new);
    }

    public static class JavaInterfaceExtender {
        @JRubyMethod(visibility = Visibility.PRIVATE)
        public static IRubyObject initialize(ThreadContext context, IRubyObject self, IRubyObject javaClassName, Block block) {
            Ruby runtime = context.runtime;

            JavaProxy.setJavaClass(self, Java.getJavaClass(runtime, javaClassName.asJavaString()));
            self.getInstanceVariables().setInstanceVariable("@block", RubyProc.newProc(runtime, block, block.type));

            self.getInternalVariables().getInternalVariable("@block");

            return context.nil;
        }

        @JRubyMethod
        public static IRubyObject extend_proxy(ThreadContext context, IRubyObject self, IRubyObject proxyClass) {
            return proxyClass.callMethod(context, "class_eval", IRubyObject.NULL_ARRAY,
                    ((RubyProc)self.getInstanceVariables().getInstanceVariable("@block")).getBlock());
        }
    }
}
