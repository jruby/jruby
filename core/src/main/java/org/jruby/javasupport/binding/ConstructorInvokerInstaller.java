package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
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
    private boolean haveLocalConstructor;
    protected List<Constructor> constructors;

    public ConstructorInvokerInstaller(String name) {
        super(name,STATIC_METHOD);
    }

    // called only by initializing thread; no synchronization required
    void addConstructor(Constructor ctor, Class<?> javaClass) {
        if (constructors == null) {
            constructors = new ArrayList<Constructor>(4);
        }
        if (!Ruby.isSecurityRestricted()) {
            try {
                ctor.setAccessible(true);
            } catch(SecurityException e) {}
        }
        constructors.add(ctor);
        haveLocalConstructor |= javaClass == ctor.getDeclaringClass();
    }

    void install(final RubyModule proxy) {
        if (haveLocalConstructor) {
            DynamicMethod method = new ConstructorInvoker(proxy, constructors);
            proxy.addMethod(name, method);
        } else {
            // if there's no constructor, we must prevent construction
            proxy.addMethod(name, new org.jruby.internal.runtime.methods.JavaMethod(proxy, PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    throw context.runtime.newTypeError("no public constructors for " + clazz);
                }
            });
        }
    }
}
