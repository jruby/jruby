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
import org.jruby.exceptions.NameError;

import java.lang.reflect.Method;

public class JavaMethodClass extends RubyObject {
    private Method method;

    public static RubyClass createJavaMethodClass(Ruby runtime, RubyModule javaModule) {
        RubyClass javaMethodClass =
                javaModule.defineClassUnder("JavaMethod", runtime.getClasses().getObjectClass());
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
}
