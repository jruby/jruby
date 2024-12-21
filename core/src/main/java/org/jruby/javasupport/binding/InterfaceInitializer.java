package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;

/**
* Created by headius on 2/26/15.
*/
final class InterfaceInitializer extends Initializer {

    InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyModule initialize(RubyModule proxy) {
        final MethodGatherer state = new MethodGatherer(runtime, null);

        state.initialize(javaClass, proxy);

        proxy.getName(runtime.getCurrentContext()); // trigger calculateName()

        return proxy;
    }

}
