package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.invokers.SingletonMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class SingletonMethodInvokerInstaller extends StaticMethodInvokerInstaller {

    final Object singleton;

    public SingletonMethodInvokerInstaller(String name, Object singleton) {
        super(name);
        this.singleton = singleton;
    }

    @Override void install(final RubyModule proxy) {
        // we don't check haveLocalMethod() here because it's not local and we know
        // that we always want to go ahead and install it
        final RubyClass singletonClass = proxy.getSingletonClass();
        DynamicMethod method = new SingletonMethodInvoker(this.singleton, singletonClass, methods);
        singletonClass.addMethod(name, method);
        if ( aliases != null && isPublic() ) {
            singletonClass.defineAliases(aliases, name);
            //aliases = null;
        }
    }
}
