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

import java.lang.reflect.Array;

import java.util.*;
import org.jruby.*;

/**
 *
 * @author Jan Arne Petersen, Alan Moore
 * @version $Revision$
 */
public class JavaUtil {
    public static boolean isCompatible(RubyObject arg, Class javaClass) {
        if (arg.isNil()) {
            return true;
        }

        if (javaClass == Object.class || javaClass == null) {
            // return arg.getJavaClass() != RubyObject.class;
            return true;
        } else if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return arg instanceof RubyBoolean;
            }
            if (cName == "float" || cName == "double") {
                return arg instanceof RubyFloat;
            }
            // else it's one of the integral types
            return arg instanceof RubyFixnum;
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
        } else if (List.class.isAssignableFrom(javaClass)) {
            return arg instanceof RubyArray;
        } else if (Map.class.isAssignableFrom(javaClass)) {
            return arg instanceof RubyHash;
        } else {
            return javaClass.isAssignableFrom(arg.getJavaClass());
        }
    }

    public static Object convertRubyToJava(Ruby ruby, RubyObject rubyObject) {
        return convertRubyToJava(ruby, rubyObject, null);
    }

    public static Object convertRubyToJava(Ruby ruby, RubyObject rubyObject, Class javaClass) {
        if (rubyObject == ruby.getNil()) {
            return null;
        }

        if (javaClass == Object.class || javaClass == null) {
            /* The Java method doesn't care what class it is, but we need to 
               know what to convert it to, so we use the object's own class.
               If that doesn't help, we use String to force a call to the 
               object's "to_s" method. */
            javaClass = rubyObject.getJavaClass();
            if (javaClass == RubyObject.class) {
                javaClass = String.class;
            }
        }

        if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return new Boolean(rubyObject.isTrue());
            }
            if (cName == "float") {
                return new Float((float) ((RubyFloat) rubyObject).getDoubleValue());
            }
            if (cName == "double") {
                return new Double(((RubyFloat) rubyObject).getDoubleValue());
            }
            if (cName == "long") {
                return new Long(((RubyFixnum) rubyObject).getLongValue());
            }
            if (cName == "int") {
                return new Integer((int) ((RubyFixnum) rubyObject).getLongValue());
            }
            if (cName == "short") {
                return new Short((short) ((RubyFixnum) rubyObject).getLongValue());
            }
            if (cName == "byte") {
                return new Byte((byte) ((RubyFixnum) rubyObject).getLongValue());
            }
            // XXX: this probably isn't good enough -AM
            return new Character((char) ((RubyFixnum) rubyObject).getLongValue());
        } else if (javaClass == String.class) {
            // If Ruby class isn't the String class call to_s method
            if (rubyObject instanceof RubyString) {
                return ((RubyString) rubyObject).getValue();
            } else {
                return ((RubyString) rubyObject.funcall("to_s")).getValue();
            }
        } else if (javaClass.isArray()) {
            try {
                Class arrayClass = javaClass.getComponentType();
                int len = ((RubyArray) rubyObject).getLength();
                Object javaObject = Array.newInstance(arrayClass, len);
                for (int i = 0; i < len; i++) {
                    Object item = convertRubyToJava(ruby, ((RubyArray) rubyObject).entry(i), arrayClass);
                    Array.set(javaObject, i, item);
                }
                return javaObject;
            } catch (NegativeArraySizeException ex) {
                return null;
            }
        } else if (List.class.isAssignableFrom(javaClass)) {
            if (javaClass == List.class) {
                javaClass = ArrayList.class;
            }
            try {
                List javaObject = (List) javaClass.newInstance();
                int len = ((RubyArray) rubyObject).getLength();
                for (int i = 0; i < len; i++) {
                    javaObject.add(convertRubyToJava(ruby, ((RubyArray) rubyObject).entry(i), null));
                }
                return javaObject;
            } catch (InstantiationException iExcptn) {
            } catch (IllegalAccessException iaExcptn) {
            }
            return null;
        } else if (Map.class.isAssignableFrom(javaClass)) {
            if (javaClass == Map.class) {
                javaClass = HashMap.class;
            }
            try {
                Map javaObject = (Map) javaClass.newInstance();
                Iterator iter = ((RubyHash) rubyObject).getValueMap().entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    javaObject.put(
                        convertRubyToJava(ruby, (RubyObject) entry.getKey(), null),
                        convertRubyToJava(ruby, (RubyObject) entry.getValue(), null));
                }
                return javaObject;
            } catch (InstantiationException iExcptn) {
            } catch (IllegalAccessException iaExcptn) {
            }
            return null;
        } else {
            return ((RubyJavaObject) rubyObject).getValue();
        }
    }

    public static RubyObject convertJavaToRuby(Ruby ruby, Object object) {
        return convertJavaToRuby(ruby, object, null);
    }

    public static RubyObject convertJavaToRuby(Ruby ruby, Object object, Class javaClass) {
        if (object == null) {
            return ruby.getNil();
        }

        if (javaClass == null) {
            javaClass = object.getClass();
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
        } else if (javaClass == String.class) {
            return RubyString.newString(ruby, object.toString());
        } else if (javaClass.isArray()) {
            Class arrayClass = javaClass.getComponentType();
            int len = Array.getLength(object);
            RubyObject[] items = new RubyObject[len];
            for (int i = 0; i < len; i++) {
                items[i] = convertJavaToRuby(ruby, Array.get(object, i), arrayClass);
            }
            return RubyArray.create(ruby, null, items);
        } else if (List.class.isAssignableFrom(javaClass)) {
            int len = ((List) object).size();
            RubyObject[] items = new RubyObject[len];
            for (int i = 0; i < len; i++) {
                items[i] = convertJavaToRuby(ruby, ((List) object).get(i), null);
            }
            return RubyArray.create(ruby, null, items);
        } else if (Map.class.isAssignableFrom(javaClass)) {
            int len = ((Map) object).size();
            RubyObject[] items = new RubyObject[len * 2];
            Iterator iter = ((Map) object).entrySet().iterator();
            for (int i = 0; i < len; i++) {
                Map.Entry entry = (Map.Entry) iter.next();
                items[2 * i] = convertJavaToRuby(ruby, entry.getKey(), null);
                items[2 * i + 1] = convertJavaToRuby(ruby, entry.getValue(), null);
            }
            return RubyHash.create(ruby, null, items);
        } else {
            return new RubyJavaObject(ruby, (RubyClass)ruby.getJavaSupport().loadClass(javaClass, null), object);
        }
    }
}
