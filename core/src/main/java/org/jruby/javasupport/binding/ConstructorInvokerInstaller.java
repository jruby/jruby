package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.java.invokers.ConstructorInvoker;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.jruby.runtime.Visibility.PUBLIC;

/**
* Created by headius on 2/26/15.
*/
public class ConstructorInvokerInstaller extends MethodInstaller {

    protected final List<Constructor> constructors = new ArrayList<Constructor>(4);
    private boolean localConstructor;

    public ConstructorInvokerInstaller(String name) { super(name, STATIC_METHOD); }

    // called only by initializing thread; no synchronization required
    void addConstructor(final Constructor ctor, final Class<?> clazz) {
        if ( ! Ruby.isSecurityRestricted() ) {
            try {
                ctor.setAccessible(true);
            } catch(SecurityException e) {}
        }
        this.constructors.add(ctor);
        localConstructor |= clazz == ctor.getDeclaringClass();
    }

    @Override void install(final RubyModule proxy) {
        if ( localConstructor ) {
            proxy.addMethod(name, new ConstructorInvoker(proxy, constructors));
        }
        else { // if there's no constructor, we must prevent construction
            proxy.addMethod(name, new org.jruby.internal.runtime.methods.JavaMethod(proxy, PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    throw context.runtime.newTypeError("no public constructors for " + clazz);
                }
            });
        }
    }
}
