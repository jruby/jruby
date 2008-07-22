package org.jruby.javasupport.methods;

import org.jruby.javasupport.*;
import java.lang.reflect.Field;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceFieldGetter extends FieldMethodZero {

    public InstanceFieldGetter(String name, RubyModule host, Field field) {
        super(name, host, field);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        try {
            return JavaUtil.convertJavaToUsableRubyObject(self.getRuntime(), field.get(((JavaObject) self.dataGetStruct()).getValue()));
        } catch (IllegalAccessException iae) {
            throw context.getRuntime().newTypeError("illegal access getting variable: " + iae.getMessage());
        }
    }
}
