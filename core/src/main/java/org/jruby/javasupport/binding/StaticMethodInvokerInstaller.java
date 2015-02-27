package org.jruby.javasupport.binding;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.invokers.StaticMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class StaticMethodInvokerInstaller extends MethodInstaller {

    public StaticMethodInvokerInstaller(String name) { super(name, STATIC_METHOD); }

    @Override void install(final RubyModule proxy) {
        if ( hasLocalMethod() ) {
            final RubyClass singletonClass = proxy.getSingletonClass();
            DynamicMethod method = new StaticMethodInvoker(singletonClass, methods);
            singletonClass.addMethod(name, method);
            if ( aliases != null && isPublic() ) {
                singletonClass.defineAliases(aliases, name);
                //aliases = null;
            }
        }
    }
}
