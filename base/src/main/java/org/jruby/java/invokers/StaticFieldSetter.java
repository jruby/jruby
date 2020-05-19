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
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
        try {
            field.set(null, arg.toJava(field.getType()));
        }
        catch (IllegalAccessException ex) { return handleSetException(context.runtime, ex); }
        catch (IllegalArgumentException ex) { return handleSetException(context.runtime, ex); }
        return arg;
    }

}
