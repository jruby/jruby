/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class JavaField extends RubyObject {
    private final Field field;

    public static RubyClass createJavaFieldClass(Ruby ruby, RubyModule javaModule) {
        RubyClass result = javaModule.defineClassUnder("JavaField", 
            ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();

        result.defineMethod("value_type", 
            callbackFactory.getMethod(JavaField.class, "value_type"));
        result.defineMethod("public?", 
            callbackFactory.getMethod(JavaField.class, "public_p"));
        result.defineMethod("static?", 
            callbackFactory.getMethod(JavaField.class, "static_p"));
        result.defineMethod("value", 
            callbackFactory.getMethod(JavaField.class, "value", IRubyObject.class));
        result.defineMethod("set_value", 
            callbackFactory.getMethod(JavaField.class, "set_value", IRubyObject.class, IRubyObject.class));
        result.defineMethod("final?", 
            callbackFactory.getMethod(JavaField.class, "final_p"));
        result.defineMethod("static_value", 
            callbackFactory.getMethod(JavaField.class, "static_value"));
        result.defineMethod("name", 
            callbackFactory.getMethod(JavaField.class, "name"));

        return result;
    }

    public JavaField(Ruby runtime, Field field) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaField"));
        this.field = field;
    }

    public RubyString value_type() {
        return RubyString.newString(getRuntime(), field.getType().getName());
    }

    public RubyBoolean public_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isPublic(field.getModifiers()));
    }

    public RubyBoolean static_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isStatic(field.getModifiers()));
    }

    public JavaObject value(IRubyObject object) {
        if (! (object instanceof JavaObject)) {
            throw new TypeError(getRuntime(), "not a java object");
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            return JavaObject.wrap(getRuntime(), field.get(javaObject));
        } catch (IllegalAccessException iae) {
            throw new TypeError(getRuntime(), "illegal access");
        }
    }

    public JavaObject set_value(IRubyObject object, IRubyObject value) {
         if (! (object instanceof JavaObject)) {
            throw new TypeError(getRuntime(), "not a java object: " + object);
        }
        if (! (value instanceof JavaObject)) {
            throw new TypeError(getRuntime(), "not a java object:" + value);
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            Object convertedValue = JavaUtil.convertArgument(((JavaObject) value).getValue(),
                                                             field.getType());

            field.set(javaObject, convertedValue);
        } catch (IllegalAccessException iae) {
            throw new TypeError(getRuntime(),
                                "illegal access on setting variable: " + iae.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new TypeError(getRuntime(),
                                "wrong type for " + field.getType().getName() + ": " +
                                ((JavaObject) value).getValue().getClass().getName());
        }
        return (JavaObject) value;
    }

    public RubyBoolean final_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isFinal(field.getModifiers()));
    }

    public JavaObject static_value() {
        try {
            return JavaObject.wrap(getRuntime(), field.get(null));
        } catch (IllegalAccessException iae) {
            throw new TypeError(getRuntime(),
                                "illegal static value access: " + iae.getMessage());
        }
    }

    public RubyString name() {
        return RubyString.newString(getRuntime(), field.getName());
    }
}
