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
import org.jruby.RubyBoolean;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Modifier;

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
        javaClassClass.defineMethod("public?", CallbackFactory.getMethod(JavaClassClass.class, "public_p"));
        javaClassClass.defineMethod("final?", CallbackFactory.getMethod(JavaClassClass.class, "final_p"));
        javaClassClass.defineMethod("interface?", CallbackFactory.getMethod(JavaClassClass.class, "interface_p"));
        javaClassClass.defineMethod("primitive?", CallbackFactory.getMethod(JavaClassClass.class, "primitive_p"));
        javaClassClass.defineMethod("name", CallbackFactory.getMethod(JavaClassClass.class, "name"));
        javaClassClass.defineMethod("to_s", CallbackFactory.getMethod(JavaClassClass.class, "name"));
        javaClassClass.defineMethod("superclass", CallbackFactory.getMethod(JavaClassClass.class, "superclass"));
        javaClassClass.defineMethod(">", CallbackFactory.getMethod(JavaClassClass.class, "op_gt", IRubyObject.class));
        javaClassClass.defineMethod("<", CallbackFactory.getMethod(JavaClassClass.class, "op_lt", IRubyObject.class));

        javaClassClass.getInternalClass().undefMethod("new");

        return javaClassClass;
    }

    public static JavaClassClass for_name(IRubyObject recv, IRubyObject name) {
        return new JavaClassClass(recv.getRuntime(), name.toString());
    }

    public RubyBoolean public_p() {
        return RubyBoolean.newBoolean(runtime, Modifier.isPublic(javaClass.getModifiers()));
    }

    public RubyBoolean final_p() {
        return RubyBoolean.newBoolean(runtime, Modifier.isFinal(javaClass.getModifiers()));
    }

    public RubyBoolean interface_p() {
        return RubyBoolean.newBoolean(runtime, javaClass.isInterface());
    }

    public RubyString name() {
        return RubyString.newString(runtime, javaClass.getName());
    }

    public IRubyObject superclass() {
        Class superclass = javaClass.getSuperclass();
        if (superclass == null) {
            return runtime.getNil();
        }
        return new JavaClassClass(runtime, superclass.getName());
    }

    public RubyBoolean op_gt(IRubyObject other) {
        if (! (other instanceof JavaClassClass)) {
            throw new TypeError(runtime, "compared with non-javaclass");
        }
        boolean result = javaClass.isAssignableFrom(((JavaClassClass) other).javaClass);
        return RubyBoolean.newBoolean(runtime, result);
    }

    public RubyBoolean op_lt(IRubyObject other) {
        if (! (other instanceof JavaClassClass)) {
            throw new TypeError(runtime, "compared with non-javaclass");
        }
        boolean result = ((JavaClassClass) other).javaClass.isAssignableFrom(javaClass);
        return RubyBoolean.newBoolean(runtime, result);
    }
}
