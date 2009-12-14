/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
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
        Ruby runtime = context.getRuntime();
        
        RubyClass ifcJavaProxy = runtime.defineClass(
                "InterfaceJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new InterfaceJavaProxy(runtime, klazz);
            }
        });

        return ifcJavaProxy;
    }
}
