package org.jruby.java.invokers;

import java.lang.reflect.Field;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StaticFieldSetter extends FieldMethodOne {

    public StaticFieldSetter(String name, RubyModule host, Field field) {
        super(name, host, field);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
        try {
            field.set(null, arg.toJava(field.getType()));
        } catch (IllegalAccessException iae) {
            throw context.getRuntime().newTypeError("illegal access setting variable: " + iae.getMessage());
        }
        return arg;
    }
}
