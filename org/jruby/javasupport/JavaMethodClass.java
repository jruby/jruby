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
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.NameError;

import java.lang.reflect.Method;

public class JavaMethodClass extends RubyObject implements IndexCallable {
    private final Method method;

    private static final int NAME = 1;
    private static final int ARITY = 2;

    public static RubyClass createJavaMethodClass(Ruby runtime, RubyModule javaModule) {
        RubyClass javaMethodClass =
                javaModule.defineClassUnder("JavaMethod", runtime.getClasses().getObjectClass());
        javaMethodClass.defineMethod("name", IndexedCallback.create(NAME, 0));
        javaMethodClass.defineMethod("arity", IndexedCallback.create(ARITY, 0));

        return javaMethodClass;
    }

    public JavaMethodClass(Ruby runtime, Method method) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaMethod"));
        this.method = method;
    }

    public static JavaMethodClass create(Ruby runtime, Class javaClass, String methodName) {
        try {
            Method method = javaClass.getMethod(methodName, new Class[0]);
            return new JavaMethodClass(runtime, method);
        } catch (NoSuchMethodException e) {
            throw new NameError(runtime, "undefined method '" + methodName + "' for class '" + javaClass.getName() + "'");
        }
    }

    public RubyString name() {
        return RubyString.newString(getRuntime(), method.getName());
    }

    public RubyFixnum arity() {
        return RubyFixnum.newFixnum(getRuntime(), method.getParameterTypes().length);
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case NAME :
                return name();
            case ARITY :
                return arity();
        }
        return super.callIndexed(index, args);
    }
}
