package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
* Created by headius on 2/26/15.
*/
public class ConstantField {

    final Field field;

    public ConstantField(Field field) { this.field = field; }

    void install(ThreadContext context, final RubyModule proxy) {
        final String name = field.getName();
        if (proxy.getConstantAt(context, name) == null) {
            try {
                final Object value = field.get(null);
                proxy.setConstant(context, name, JavaUtil.convertJavaToUsableRubyObject(context.runtime, value));
            }
            catch (IllegalAccessException iae) {
                // if we can't read it, we don't set it
            }
        }
    }
}
