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
import org.jruby.RubyClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.NameError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaClassClass extends RubyObject {
    private Class javaClass;

    private JavaClassClass(Ruby runtime, String name) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaClass"));
        try {
            this.javaClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new NameError(runtime, "java class not found: " + name);
        }
    }

    public static RubyClass createJavaClassClass(Ruby runtime, RubyModule javaModule) {
        RubyClass javaClassClass =
                javaModule.defineClassUnder("JavaClass", runtime.getClasses().getObjectClass());

        javaClassClass.defineSingletonMethod("for_name", CallbackFactory.getSingletonMethod(JavaClassClass.class, "for_name", IRubyObject.class));
        javaClassClass.defineMethod("to_s", CallbackFactory.getMethod(JavaClassClass.class, "to_s"));

        return javaClassClass;
    }

    public static JavaClassClass for_name(IRubyObject recv, IRubyObject name) {
        return new JavaClassClass(recv.getRuntime(), name.toString());
    }

    public RubyString to_s() {
        return RubyString.newString(runtime, javaClass.getName());
    }
}
