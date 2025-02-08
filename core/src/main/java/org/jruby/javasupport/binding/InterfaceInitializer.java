package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;

/**
* Created by headius on 2/26/15.
*/
final class InterfaceInitializer extends Initializer {

    InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyModule initialize(ThreadContext context, RubyModule proxy) {
        final MethodGatherer state = new MethodGatherer(context.runtime, null);

        state.initialize(context, javaClass, proxy);

        proxy.getName(context); // trigger calculateName()

        return proxy;
    }

}
