package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.java.invokers.StaticFieldSetter;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Field;

/**
* Created by headius on 2/26/15.
*/
public class StaticFieldSetterInstaller extends FieldInstaller {

    public StaticFieldSetterInstaller(String name, Field field) {
        super(name, STATIC_FIELD, field);
    }

    @Override void install(ThreadContext context, final RubyModule proxy) {
        if (isAccessible()) {
            proxy.singletonClass(context).addMethod(context, name, new StaticFieldSetter(name, proxy, field));
        }
    }
}
