package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.invokers.StaticMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class StaticMethodInvokerInstaller extends MethodInstaller {
    public StaticMethodInvokerInstaller(String name) {
        super(name,STATIC_METHOD);
    }

    void install(RubyModule proxy) {
        if (hasLocalMethod()) {
            RubyClass singleton = proxy.getSingletonClass();
            DynamicMethod method = new StaticMethodInvoker(singleton, methods);
            singleton.addMethod(name, method);
            if (aliases != null && isPublic() ) {
                singleton.defineAliases(aliases, this.name);
                aliases = null;
            }
        }
    }
}
