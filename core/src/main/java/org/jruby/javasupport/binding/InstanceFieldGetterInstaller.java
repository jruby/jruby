package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.InstanceFieldGetter;

import java.lang.reflect.Field;

/**
* Created by headius on 2/26/15.
*/
public class InstanceFieldGetterInstaller extends FieldInstaller {

    public InstanceFieldGetterInstaller(String name, Field field) {
        super(name, INSTANCE_FIELD, field);
    }

    @Override void install(final RubyModule proxy) {
        if (isAccessible()) {
            proxy.addMethod(name, new InstanceFieldGetter(name, proxy, field));
        }
    }
}
