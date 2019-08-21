package org.jruby.java.invokers;

import java.lang.reflect.Field;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Java field getter (reader) base implementation e.g. `self.myField`.
 */
public abstract class FieldMethodZero extends JavaMethod.JavaMethodZero {
    
    protected final Field field;

    FieldMethodZero(String name, RubyModule host, Field field) {
        this(host, field, name);
    }

    public Field getField() {
        return field;
    }

    protected FieldMethodZero(RubyModule host, Field field, String name) {
        super(host, Visibility.PUBLIC, name);
        Java.trySetAccessible(field);
        this.field = field;
    }

    protected Object retrieveTarget(final IRubyObject self) {
        return FieldMethodZero.retrieveTargetImpl(self);
    }

    static Object retrieveTargetImpl(final IRubyObject self) {
        // NOTE: we re-use these with Class#become_java!'s generated field accessors
        return self instanceof JavaProxy ? ((JavaProxy) self).getObject() : self;
    }

    protected final IRubyObject handleGetException(final Ruby runtime, final IllegalAccessException ex) {
        throw runtime.newSecurityError("illegal access getting field '" + field + "' : " + ex.getMessage());
    }

}
