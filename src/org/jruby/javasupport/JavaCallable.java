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

import java.lang.reflect.Modifier;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;

public abstract class JavaCallable extends JavaAccessibleObject {

    public JavaCallable(Ruby runtime, RubyClass rubyClass, Object javaObject) {
        super(runtime, rubyClass);
    }

    public final RubyFixnum arity() {
        return getRuntime().newFixnum(getArity());
    }

    public final RubyArray argument_types() {
        Class[] parameterTypes = parameterTypes();
        RubyArray result = getRuntime().newArray(parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            result.append(getRuntime().newString(parameterTypes[i].getName()));
        }
        return result;
    }

    public final RubyString inspect() {
        StringBuffer result = new StringBuffer();
        result.append(nameOnInspection());
        Class[] parameterTypes = parameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            result.append(parameterTypes[i].getName());
            if (i < parameterTypes.length - 1) {
                result.append(',');
            }
        }
        result.append(")>");
        return getRuntime().newString(result.toString());
    }

    protected abstract int getArity();
    protected abstract Class[] parameterTypes();
    protected abstract int getModifiers();

    /**
     * @return the name used in the head of the string returned from inspect()
     */
    protected abstract String nameOnInspection();

    public RubyBoolean public_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isPublic(getModifiers()));
    }


    public static void registerRubyMethods(Ruby runtime, RubyClass result, Class klass) {
        registerRubyMethods(runtime, result);
        
        CallbackFactory callbackFactory = runtime.callbackFactory();

        result.defineMethod("public?",  callbackFactory.getMethod(klass, "public_p"));
    }
}
