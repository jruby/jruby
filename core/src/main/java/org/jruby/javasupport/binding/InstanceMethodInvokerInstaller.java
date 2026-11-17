package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Method;

/**
* Created by headius on 2/26/15.
*/
public class InstanceMethodInvokerInstaller extends MethodInstaller {

    public InstanceMethodInvokerInstaller(String name) { super(name, INSTANCE_METHOD); }

    @Override void install(ThreadContext context, final RubyModule proxy) {
        if (hasLocalMethod()) {
            defineMethods(context, proxy, new InstanceMethodInvoker(proxy, () -> methods.toArray(new Method[methods.size()]), name), true);
        }
    }
}
