/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.posix.util;

import java.lang.reflect.Field;

/**
 *
 * @author nicksieger
 */
public class FieldAccess {
    public static Field getProtectedField(Class klass, String fieldName) {
        Field field = null;
        try {
            field = klass.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (Exception e) {
        }
        return field;
    }
    public static Object getProtectedFieldValue(Class klass, String fieldName, Object instance) {
        try {
            Field f = getProtectedField(klass, fieldName);
            return f.get(instance);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
