/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaField extends JavaAccessibleObject {
    private Field field;

    public static RubyClass createJavaFieldClass(Ruby runtime, RubyModule javaModule) {
        RubyClass result = javaModule.defineClassUnder("JavaField", 
            runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory();

        JavaAccessibleObject.registerRubyMethods(runtime, result);
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
        return getRuntime().newString(field.getType().getName());
    }

    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(field.getModifiers()));
    }

    public RubyBoolean static_p() {
        return getRuntime().newBoolean(Modifier.isStatic(field.getModifiers()));
    }

    public JavaObject value(IRubyObject object) {
        if (! (object instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object");
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            return JavaObject.wrap(getRuntime(), field.get(javaObject));
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access");
        }
    }

    public JavaObject set_value(IRubyObject object, IRubyObject value) {
         if (! (object instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object: " + object);
        }
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            Object convertedValue = JavaUtil.convertArgument(((JavaObject) value).getValue(),
                                                             field.getType());

            field.set(javaObject, convertedValue);
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError(
                                "illegal access on setting variable: " + iae.getMessage());
        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError(
                                "wrong type for " + field.getType().getName() + ": " +
                                ((JavaObject) value).getValue().getClass().getName());
        }
        return (JavaObject) value;
    }

    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(field.getModifiers()));
    }

    public JavaObject static_value() {
        try {
	    // TODO: Only setAccessible to account for pattern found by
	    // accessing constants included from a non-public interface.
	    // (aka java.util.zip.ZipConstants being implemented by many
	    // classes)
	    field.setAccessible(true);
            return JavaObject.wrap(getRuntime(), field.get(null));
        } catch (IllegalAccessException iae) {
	    throw new TypeError(getRuntime(),
				"illegal static value access: " + iae.getMessage());
        }
    }

    public RubyString name() {
        return getRuntime().newString(field.getName());
    }
    
    protected AccessibleObject accesibleObject() {
        return field;
    }
}
