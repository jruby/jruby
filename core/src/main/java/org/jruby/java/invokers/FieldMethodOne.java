package org.jruby.java.invokers;

import java.lang.reflect.Field;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Java field setter (writer) base implementation e.g. `self.myField = value`.
 */
public abstract class FieldMethodOne extends JavaMethod.JavaMethodOne {

    final Field field;

    FieldMethodOne(String name, RubyModule host, Field field) {
        this(host, field, name);
    }

    public Field getField() {
        return field;
    }

    protected FieldMethodOne(RubyModule host, Field field, String name) {
        super(host, Visibility.PUBLIC, name);
        if ( ! Ruby.isSecurityRestricted() ) field.setAccessible(true);
        this.field = field;
    }

    protected Object retrieveTarget(final IRubyObject self) {
        return FieldMethodZero.retrieveTargetImpl(self);
    }

    protected final IRubyObject handleSetException(final Ruby runtime, final IllegalAccessException ex) {
        throw runtime.newSecurityError("illegal access setting field '" + field + "' : " + ex.getMessage());
    }

    protected final IRubyObject handleSetException(final Ruby runtime, final IllegalArgumentException ex) {
        throw runtime.newTypeError(ex.getMessage());
    }

}
