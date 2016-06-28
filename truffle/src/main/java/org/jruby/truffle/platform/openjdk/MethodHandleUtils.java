/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.openjdk;

import org.jruby.truffle.language.control.JavaException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public abstract class MethodHandleUtils {

    public static MethodHandle getPrivateGetter(Class<?> klass, String fieldName) {
        final Field field = getPrivateField(klass, fieldName);
        try {
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException e) {
            throw new JavaException(e);
        }
    }

    private static Field getPrivateField(final Class<?> klass, final String fieldName) {
        try {
            final Field field = klass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new JavaException(e);
        }
    }

}
