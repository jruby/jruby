package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Method;

/**
* Created by headius on 2/26/15.
*/
public class StaticMethodInvokerInstaller extends MethodInstaller {

    public StaticMethodInvokerInstaller(String name) { super(name, STATIC_METHOD); }

    @Override void install(ThreadContext context, final RubyModule proxy) {
        if (hasLocalMethod()) {
            final RubyClass singletonClass = proxy.singletonClass(context);
            defineMethods(context, singletonClass,
                    new StaticMethodInvoker(singletonClass, () -> methods.toArray(new Method[methods.size()]), name), false);
        }
    }
}
