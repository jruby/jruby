/*
 * JavaReflectionMethod.java - No description
 * Created on 21. September 2001, 15:03
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

package org.jruby.core;

import java.lang.reflect.*;

import org.jruby.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class JavaReflectionMethod implements RubyCallbackMethod {
    private Method method = null;

    public JavaReflectionMethod(Method method) {
        this.method = method;
    }

    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        Object[] newArgs = new Object[args.length];
        
        for (int i = 0; i < newArgs.length; i++) {
            newArgs[i] = convertRubyToJava(ruby, args[i], method.getParameterTypes()[i]);
        }

        try {
            Object result = method.invoke(((RubyJavaObject)recv).getValue(), newArgs);
            return convertJavaToRuby(ruby, result, method.getReturnType());
        } catch (IllegalAccessException iaExcptn) {
        } catch (IllegalArgumentException iaExcptn) {
        } catch (InvocationTargetException itExcptn) {
        }
        return ruby.getNil();
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