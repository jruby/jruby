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

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class JavaUtil {
    public static boolean isCompatible(RubyObject arg, Class javaClass) {
        if (arg.isNil()) {
            return true;
        }
        if (javaClass.isArray()) {
            if (!(arg instanceof RubyArray)) {
                return false;
            }
            Class arrayClass = javaClass.getComponentType();
            for (int i = 0; i < ((RubyArray)arg).length(); i++) {
                if (!isCompatible(((RubyArray)arg).entry(i), arrayClass)) {
                    return false;
                }
            }
            return true;
        }
        if (javaClass == Object.class) {
            return arg.getJavaClass() != RubyObject.class;
        }
        return javaClass.isAssignableFrom(arg.getJavaClass());
    }
    
    public static Object convertRubyToJava(Ruby ruby, RubyObject rubyObject, Class javaClass) {
        if (rubyObject == ruby.getNil()) {
            return null;
        } else if (javaClass.isArray()) {
            try {
                Class arrayClass = javaClass.getComponentType();
                int len = (int)((RubyArray)rubyObject).length();
                Object javaObject = Array.newInstance(arrayClass, len);
                for (int i = 0; i < len; i++) {
                    Object item = convertRubyToJava(ruby, ((RubyArray)rubyObject).entry(i), arrayClass);
                    Array.set(javaObject, i, item);
                }
                return javaObject;
            }
            catch (NegativeArraySizeException ex) {
            }
        } else if (javaClass == Object.class) {
            javaClass = rubyObject.getJavaClass();
        } else if (javaClass == Boolean.TYPE || javaClass == Boolean.class) {
            return new Boolean(rubyObject.isTrue());
        } else if (javaClass == Integer.TYPE || javaClass == Integer.class) {
            return new Integer((int)((RubyFixnum)rubyObject).getLongValue());
        } else if (javaClass == Long.TYPE || javaClass == Long.class) {
            return new Long(((RubyFixnum)rubyObject).getLongValue());
        } else if (javaClass == Float.TYPE || javaClass == Float.class) {
            return new Float((float)((RubyFloat)rubyObject).getDoubleValue());
        } else if (javaClass == Double.TYPE || javaClass == Double.class) {
            return new Double(((RubyFloat)rubyObject).getDoubleValue());
        } else if (javaClass == String.class) {
            // If Ruby class is't the String class call to_s method
            if (rubyObject instanceof RubyString) {
                return ((RubyString)rubyObject).getValue();
            } else {
                return ((RubyString)rubyObject.funcall(ruby.intern("to_s"))).getValue();
            }
        } else if (rubyObject instanceof RubyJavaObject) {
            return ((RubyJavaObject)rubyObject).getValue();
        }
        return rubyObject.toString();
    }

    public static RubyObject convertJavaToRuby(Ruby ruby, Object object, Class javaClass) {
        if (object == null) {
            return ruby.getNil();
        }
        if (javaClass == Boolean.TYPE || javaClass == Boolean.class) {
            return RubyBoolean.m_newBoolean(ruby, ((Boolean)object).booleanValue());
        }
        if (javaClass == Integer.TYPE || javaClass == Integer.class ||
            javaClass == Long.TYPE || javaClass == Long.class) {
            return RubyFixnum.m_newFixnum(ruby, ((Number)object).intValue());
        }
        if (javaClass == Float.TYPE || javaClass == Float.class ||
            javaClass == Double.TYPE || javaClass == Double.class) {
            return RubyFloat.m_newFloat(ruby, ((Number)object).doubleValue());
        }
        if (javaClass == String.class) {
            return RubyString.m_newString(ruby, object.toString());
        }
        if (javaClass.isArray()) {
            Class arrayClass = javaClass.getComponentType();
            int len = Array.getLength(object);
            RubyObject[] items = new RubyObject[len];
            for (int i = 0; i < len; i++) {
                items[i] = convertJavaToRuby(ruby, Array.get(object, i), arrayClass);
            }
            return RubyArray.m_create(ruby, items);
        }
        return new RubyJavaObject(ruby, RubyJavaObject.loadClass(ruby, javaClass, null), object);
    }
}