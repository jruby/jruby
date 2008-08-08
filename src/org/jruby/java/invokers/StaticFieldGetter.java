package org.jruby.java.invokers;

import org.jruby.javasupport.*;
import java.lang.reflect.Field;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StaticFieldGetter extends FieldMethodZero {

    public StaticFieldGetter(String name, RubyModule host, Field field) {
        super(name, host, field);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        try {
            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), field.get(null));
        } catch (IllegalAccessException iae) {
            throw context.getRuntime().newTypeError("illegal access getting variable: " + iae.getMessage());
        }
    }
}
