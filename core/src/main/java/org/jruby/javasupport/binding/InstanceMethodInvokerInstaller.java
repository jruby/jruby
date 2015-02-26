package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.invokers.InstanceMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class InstanceMethodInvokerInstaller extends MethodInstaller {
    public InstanceMethodInvokerInstaller(String name) {
        super(name,INSTANCE_METHOD);
    }
    void install(RubyModule proxy) {
        if (hasLocalMethod()) {
            DynamicMethod method = new InstanceMethodInvoker(proxy, methods);
            proxy.addMethod(name, method);
            if (aliases != null && isPublic()) {
                proxy.defineAliases(aliases, this.name);
                aliases = null;
            }
        }
    }
}
