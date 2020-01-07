package org.jruby.java.invokers;

import java.lang.reflect.Field;

import org.jruby.RubyModule;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StaticFieldGetter extends FieldMethodZero {
    private boolean warnConstant;

    public StaticFieldGetter(String name, RubyModule host, Field field, boolean warnConstant) {
        super(name, host, field);

        this.warnConstant = warnConstant;
    }

    @Override
    public final IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        if (warnConstant) {
            // Warn once about accessing what should be a constant via an accessor.
            // See jruby/jruby#5730.
            warnConstant = false;
            context.runtime.getWarnings().warning("DEPRECATED: Accessing Java interface constant via accessor method. This behavior will go away in JRuby 9.3, please use constant-style A::B syntax for accessing '" + field.toString() + "'");
        }

        try {
            return JavaUtil.convertJavaToUsableRubyObject(context.runtime, field.get(null));
        }
        catch (IllegalAccessException ex) { return handleGetException(context.runtime, ex); }
    }

}
