/*
 * JavaUtil.java - No description
 * Created on 22. September 2001, 16:23
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Jan Arne Petersen, Alan Moore
 * @version $Revision$
 */
public class JavaUtil {
    public static boolean isCompatible(IRubyObject arg, Class javaClass) {
        if (arg.isNil()) {
            return true;
        }

        if (javaClass.isInstance(arg)) {
            // arg is already of the required jruby class (or subclass)
            return true;
        }
        if (javaClass == Object.class || javaClass == null) {
            return true;
        } else if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return true;
            }
            return arg instanceof RubyNumeric || arg instanceof RubyString;
        } else if (javaClass.isArray()) {
            if (!(arg instanceof RubyArray)) {
                return false;
            }
            Class arrayClass = javaClass.getComponentType();
            for (int i = 0; i < ((RubyArray) arg).getLength(); i++) {
                if (!isCompatible(((RubyArray) arg).entry(i), arrayClass)) {
                    return false;
                }
            }
            return true;
        } else {
            return javaClass.isAssignableFrom(arg.getJavaClass());
        }
    }

    public static Object convertRubyToJava(Ruby ruby, IRubyObject rubyObject) {
        return convertRubyToJava(ruby, rubyObject, null);
    }

    public static Object convertRubyToJava(Ruby ruby, IRubyObject rubyObject, Class javaClass) {
        if (rubyObject == null || rubyObject == ruby.getNil()) {
            return null;
        }

        if (rubyObject instanceof JavaObject) {
            return ((JavaObject) rubyObject).getValue();
        } else if (javaClass == Object.class || javaClass == null) {
            /* The Java method doesn't care what class it is, but we need to
               know what to convert it to, so we use the object's own class.
               If that doesn't help, we use String to force a call to the
               object's "to_s" method. */
            javaClass = rubyObject.getJavaClass();
            if (javaClass == IRubyObject.class) {
                javaClass = String.class;
            }
        }

        if (javaClass.isInstance(rubyObject)) {
            // rubyObject is already of the required jruby class (or subclass)
            return rubyObject;
        }

        if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return new Boolean(rubyObject.isTrue());
            } else if (cName == "float") {
                if (rubyObject.respondsTo("to_f")) {
                    return new Float(((RubyNumeric) rubyObject.callMethod("to_f")).getDoubleValue());
                } else {
                    return new Float(0.0);
                }
            } else if (cName == "double") {
                if (rubyObject.respondsTo("to_f")) {
                    return new Double(((RubyNumeric) rubyObject.callMethod("to_f")).getDoubleValue());
                } else {
                    return new Double(0.0);
                }
            } else if (cName == "long") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Long(((RubyNumeric) rubyObject.callMethod("to_i")).getLongValue());
                } else {
                    return new Long(0);
                }
            } else if (cName == "int") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Integer((int) ((RubyNumeric) rubyObject.callMethod("to_i")).getLongValue());
                } else {
                    return new Integer(0);
                }
            } else if (cName == "short") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Short((short) ((RubyNumeric) rubyObject.callMethod("to_i")).getLongValue());
                } else {
                    return new Short((short) 0);
                }
            } else if (cName == "byte") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Byte((byte) ((RubyNumeric) rubyObject.callMethod("to_i")).getLongValue());
                } else {
                    return new Byte((byte) 0);
                }
            }

            // XXX this probably isn't good enough -AM
            String s = ((RubyString) rubyObject.callMethod("to_s")).getValue();
            if (s.length() > 0) {
                return new Character(s.charAt(0));
            } else {
                return new Character('\0');
            }
        } else if (javaClass == String.class) {
            return ((RubyString) rubyObject.callMethod("to_s")).getValue();
        } else {
            return ((JavaObject) rubyObject).getValue();
        }
    }

    public static IRubyObject[] convertJavaArrayToRuby(Ruby ruby, Object[] objects) {
        IRubyObject[] rubyObjects = new IRubyObject[objects.length];
        for (int i = 0; i < objects.length; i++) {
            rubyObjects[i] = convertJavaToRuby(ruby, objects[i]);
        }
        return rubyObjects;
    }

    public static IRubyObject convertJavaToRuby(Ruby ruby, Object object) {
        if (object == null) {
            return ruby.getNil();
        }
        return convertJavaToRuby(ruby, object, object.getClass());
    }

    public static IRubyObject convertJavaToRuby(Ruby ruby, Object object, Method returningMethod) {
        return convertJavaToRuby(ruby, object, returnedObjectType(object, returningMethod));
    }

    public static IRubyObject convertJavaToRuby(Ruby ruby, Object object, Class javaClass) {
        if (object == null) {
            return ruby.getNil();
        }

        if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return RubyBoolean.newBoolean(ruby, ((Boolean) object).booleanValue());
            } else if (cName == "float" || cName == "double") {
                return RubyFloat.newFloat(ruby, ((Number) object).doubleValue());
            } else if (cName == "char") {
                return RubyFixnum.newFixnum(ruby, ((Character) object).charValue());
            } else {
                // else it's one of the integral types
                return RubyFixnum.newFixnum(ruby, ((Number) object).longValue());
            }
        } else if (javaClass == Boolean.class) {
            return RubyBoolean.newBoolean(ruby, ((Boolean) object).booleanValue());
        } else if (javaClass == Float.class || javaClass == Double.class) {
            return RubyFloat.newFloat(ruby, ((Number) object).doubleValue());
        } else if (javaClass == Character.class) {
            return RubyFixnum.newFixnum(ruby, ((Character) object).charValue());
        } else if (Number.class.isAssignableFrom(javaClass)) {
            return RubyFixnum.newFixnum(ruby, ((Number) object).longValue());
        } else if (javaClass == String.class) {
            return RubyString.newString(ruby, object.toString());
        } else if (IRubyObject.class.isAssignableFrom(javaClass)) {
            return (IRubyObject) object;
        } else if (object instanceof RubyProxy) {
            return ((RubyProxy) object).getRubyObject();
        } else {
            // Look if a RubyObject exists which already represents object.
            Iterator iter = ruby.objectSpace.iterator(ruby.getClasses().getObjectClass());
            while (iter.hasNext()) {
                IRubyObject rubyObject = (IRubyObject) iter.next();
                if (rubyObject instanceof JavaObject) {
                    if (((JavaObject) rubyObject).getValue() == object) {
                        return rubyObject;
                    }
                }
            }
            RubyClass rubyClass = (RubyClass) ruby.getJavaSupport().loadClass(javaClass, null);
            return new JavaObject(ruby, rubyClass, object);
        }
    }

    protected static Class getCanonicalJavaClass(Class type) {
        // Replace wrapper classes with the primitive class that each
        // represents.
        if (type == Double.class)
            return Double.TYPE;
        if (type == Float.class)
            return Float.TYPE;
        if (type == Integer.class)
            return Integer.TYPE;
        if (type == Long.class)
            return Long.TYPE;
        if (type == Short.class)
            return Short.TYPE;
        if (type == Byte.class)
            return Byte.TYPE;
        if (type == Character.class)
            return Character.TYPE;
        if (type == Void.class)
            return Void.TYPE;
        if (type == Boolean.class)
            return Boolean.TYPE;

        return type;
    }

    private static Class returnedObjectType(Object javaResult, Method returningMethod) {
        Class widestClass = returningMethod.getReturnType();
        if (javaResult == null) {
            return widestClass;
        }
        Class narrowestClass = javaResult.getClass();

        if (narrowestClass.isPrimitive()) {
            return narrowestClass;
        }
        if (widestClass.isPrimitive()) {
            return widestClass;
        }

        Asserts.isTrue(widestClass.isAssignableFrom(narrowestClass));

        if (Modifier.isPublic(narrowestClass.getModifiers())) {
            return narrowestClass;
        }
        if (widestClass.isInterface()) {
            return widestClass;
        }

        // If there is any interface on the narrowest that isn't on the widest,
        // then we should use that. The theory is that this should minimize information
        // loss. (This theory is open to discussion ;-)
        Iterator narrowestClassInterfaces = Arrays.asList(narrowestClass.getInterfaces()).iterator();
        List widestClassInterfaces = Arrays.asList(widestClass.getInterfaces());
        while (narrowestClassInterfaces.hasNext()) {
            Class iface = (Class) narrowestClassInterfaces.next();
            if (!widestClassInterfaces.contains(iface)) {
                return iface;
            }
        }

        while (true) {
            narrowestClass = narrowestClass.getSuperclass();
            Asserts.isTrue(narrowestClass != null);
            if (Modifier.isPublic(narrowestClass.getModifiers())) {
                return narrowestClass;
            }
        }
    }

    public static Object convertArgument(Object argument, Class parameterType) {
        Object result = argument;
        if (result instanceof JavaObject) {
            result = ((JavaObject) result).getValue();
        }
        // FIXME: do convertions for all numeric types
        if (parameterType.equals(Integer.class) || parameterType.equals(Integer.TYPE)) {
            if (result instanceof Long) {
                result = new Integer(((Long) result).intValue());
            }
        } else if (parameterType.equals(Long.class) || parameterType.equals(Long.TYPE)) {
            if (result instanceof Integer) {
                result = new Long(((Integer) result).longValue());
            }
        }
        return result;
    }
}
