package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.InstanceMethodInvoker;

/**
* Created by headius on 2/26/15.
*/
public class InstanceMethodInvokerInstaller extends MethodInstaller {

    public InstanceMethodInvokerInstaller(String name) { super(name, INSTANCE_METHOD); }

    @Override void install(final RubyModule proxy) {
        if ( hasLocalMethod() ) {
            proxy.addMethod(name, new InstanceMethodInvoker(proxy, methods));
            if ( aliases != null && isPublic() ) {
                proxy.defineAliases(aliases, this.name);
                //aliases = null;
            }
        }
    }
}
