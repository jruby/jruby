package org.jruby.javasupport.methods;

import org.jruby.javasupport.*;
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
            Object newValue = JavaUtil.convertArgumentToType(context, arg, field.getType());
            field.set(null, newValue);
        } catch (IllegalAccessException iae) {
            throw context.getRuntime().newTypeError("illegal access setting variable: " + iae.getMessage());
        }
        return arg;
    }
}
