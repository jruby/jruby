package org.jruby.java.invokers;

import java.lang.reflect.Field;

import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceFieldSetter extends FieldMethodOne {

    public InstanceFieldSetter(String name, RubyModule host, Field field) {
        super(name, host, field);
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg) {
        try {
            Object value = arg.toJava(field.getType());
            field.set(retrieveTarget(self), value);
        }
        catch (IllegalAccessException ex) { return handleSetException(context.runtime, ex); }
        catch (IllegalArgumentException ex) { return handleSetException(context.runtime, ex); }
        return arg;
    }

}
