package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Field;

/**
* Created by headius on 2/26/15.
*/
public class InstanceFieldSetterInstaller extends FieldInstaller {

    public InstanceFieldSetterInstaller(String name, Field field) {
        super(name, INSTANCE_FIELD, field);
    }

    @Override void install(ThreadContext context, final RubyModule proxy) {
        if (isAccessible()) {
            proxy.addMethod(name, new InstanceFieldSetter(name, proxy, field));
        }
    }
}
