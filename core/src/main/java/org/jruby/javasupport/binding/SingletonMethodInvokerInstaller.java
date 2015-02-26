package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.invokers.SingletonMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class SingletonMethodInvokerInstaller extends StaticMethodInvokerInstaller {
    private Object singleton;

    public SingletonMethodInvokerInstaller(String name, Object singleton) {
        super(name);
        this.singleton = singleton;
    }

    void install(RubyModule proxy) {
        // we don't check haveLocalMethod() here because it's not local and we know
        // that we always want to go ahead and install it
        RubyClass rubySingleton = proxy.getSingletonClass();
        DynamicMethod method = new SingletonMethodInvoker(this.singleton, rubySingleton, methods);
        rubySingleton.addMethod(name, method);
        if (aliases != null && isPublic()) {
            rubySingleton.defineAliases(aliases, this.name);
            aliases = null;
        }
    }
}
