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

import org.jruby.RubyObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyBoolean;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class JavaField extends RubyObject implements IndexCallable {
    private final Field field;

    private static final int VALUE_TYPE = 1;
    private static final int PUBLIC_P = 2;
    private static final int STATIC_P = 3;
    private static final int VALUE = 4;
    private static final int SET_VALUE = 5;
    private static final int FINAL_P = 6;
    private static final int STATIC_VALUE = 7;
    private static final int NAME = 8;

    public static RubyClass createJavaFieldClass(Ruby runtime, RubyModule javaModule) {
        RubyClass javaFieldClass =
                javaModule.defineClassUnder("JavaField", runtime.getClasses().getObjectClass());

        javaFieldClass.defineMethod("value_type", IndexedCallback.create(VALUE_TYPE, 0));
        javaFieldClass.defineMethod("public?", IndexedCallback.create(PUBLIC_P, 0));
        javaFieldClass.defineMethod("static?", IndexedCallback.create(STATIC_P, 0));
        javaFieldClass.defineMethod("value", IndexedCallback.create(VALUE, 1));
        javaFieldClass.defineMethod("set_value", IndexedCallback.create(SET_VALUE, 2));
        javaFieldClass.defineMethod("final?", IndexedCallback.create(FINAL_P, 0));
        javaFieldClass.defineMethod("static_value", IndexedCallback.create(STATIC_VALUE, 0));
        javaFieldClass.defineMethod("name", IndexedCallback.create(NAME, 0));

        return javaFieldClass;
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

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case VALUE_TYPE :
                return value_type();
            case PUBLIC_P :
                return public_p();
            case STATIC_P :
                return static_p();
            case VALUE :
                return value(args[0]);
            case SET_VALUE :
                return set_value(args[0], args[1]);
            case FINAL_P :
                return final_p();
            case STATIC_VALUE :
                return static_value();
            case NAME :
                return name();
            default :
                return super.callIndexed(index, args);
        }
    }
}
