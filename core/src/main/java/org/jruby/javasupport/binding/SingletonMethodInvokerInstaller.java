package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.java.invokers.SingletonMethodInvoker;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Method;

/**
* Created by headius on 2/26/15.
*/
public class SingletonMethodInvokerInstaller extends StaticMethodInvokerInstaller {

    final Object singleton;

    public SingletonMethodInvokerInstaller(String name, Object singleton) {
        super(name);
        this.singleton = singleton;
    }

    @Override void install(ThreadContext context, final RubyModule proxy) {
        // we don't check haveLocalMethod() here because it's not local and we know
        // that we always want to go ahead and install it
        final RubyClass singletonClass = proxy.singletonClass(context);
        defineMethods(context, singletonClass,
                new SingletonMethodInvoker(this.singleton, singletonClass, () -> methods.toArray(new Method[methods.size()]), name), false);
    }
}
