package org.jruby.javasupport.binding;

import org.jruby.RubyModule;
import org.jruby.javasupport.JavaUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
* Created by headius on 2/26/15.
*/
public class ConstantField {

    final Field field;

    public ConstantField(Field field) { this.field = field; }

    void install(final RubyModule proxy) {
        final String name = field.getName();
        if ( proxy.getConstantAt(name) == null ) {
            try {
                final Object value = field.get(null);
                proxy.setConstant(name, JavaUtil.convertJavaToUsableRubyObject(proxy.getRuntime(), value));
            }
            catch (IllegalAccessException iae) {
                // if we can't read it, we don't set it
            }
        }
    }

    private static final int CONSTANT = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;

    static boolean isConstant(final Field field) {
        return (field.getModifiers() & CONSTANT) == CONSTANT &&
            Character.isUpperCase( field.getName().charAt(0) );
    }

}
