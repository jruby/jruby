/*
 * JavaUtil.java - No description
 * Created on 22. September 2001, 16:23
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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
        if (javaClass == Boolean.TYPE || javaClass == Boolean.class) {
            return arg instanceof RubyBoolean;
        }
        if (javaClass == Integer.TYPE || javaClass == Integer.class ||
            javaClass == Long.TYPE || javaClass == Long.class) {
            return arg instanceof RubyFixnum;
        }
        if (javaClass == Float.TYPE || javaClass == Float.class || 
            javaClass == Double.TYPE || javaClass == Double.class) {
            return arg instanceof RubyFloat;
        }
        if (javaClass == String.class) {
            return arg instanceof RubyString;
        }
        return javaClass.isAssignableFrom(((RubyJavaObject)arg).getValue().getClass());
    }
    
    public static Object convertRubyToJava(Ruby ruby, RubyObject rubyObject, Class javaClass) {
        if (rubyObject == ruby.getNil()) {
            return null;
        }
        if (javaClass == Boolean.TYPE || javaClass == Boolean.class) {
            return new Boolean(rubyObject.isTrue());
        }
        if (javaClass == Integer.TYPE || javaClass == Integer.class) {
            return new Integer((int)((RubyFixnum)rubyObject).getLongValue());
        }
        if (javaClass == Long.TYPE || javaClass == Long.class) {
            return new Long(((RubyFixnum)rubyObject).getLongValue());
        }
        if (javaClass == Float.TYPE || javaClass == Float.class) {
            return new Float((float)((RubyFloat)rubyObject).getDoubleValue());
        }
        if (javaClass == Double.TYPE || javaClass == Double.class) {
            return new Double(((RubyFloat)rubyObject).getDoubleValue());
        }
        if (javaClass == String.class) {
            return ((RubyString)rubyObject).getString();
        }
        return ((RubyJavaObject)rubyObject).getValue();
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
        return new RubyJavaObject(ruby, ruby.getRubyClass("JavaObject"), object);
    }
}