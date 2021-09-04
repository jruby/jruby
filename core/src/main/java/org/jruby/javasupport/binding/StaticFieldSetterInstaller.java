package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.StaticFieldSetter;

import java.lang.reflect.Field;

/**
* Created by headius on 2/26/15.
*/
public class StaticFieldSetterInstaller extends FieldInstaller {

    public StaticFieldSetterInstaller(String name, Field field) {
        super(name, STATIC_FIELD, field);
    }

    @Override void install(final RubyModule proxy) {
        if (isAccessible()) {
            proxy.getSingletonClass().addMethod(name, new StaticFieldSetter(name, proxy, field));
        }
    }
}
