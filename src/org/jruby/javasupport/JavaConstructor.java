/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class JavaConstructor extends JavaCallable {
    private final Constructor constructor;

    public static RubyClass createJavaConstructorClass(Ruby ruby, RubyModule javaModule) {
        RubyClass result =
                javaModule.defineClassUnder("JavaConstructor", ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();
        
        result.defineMethod("arity", 
                callbackFactory.getMethod(JavaConstructor.class, "arity"));
        result.defineMethod("inspect", 
                callbackFactory.getMethod(JavaConstructor.class, "inspect"));
        result.defineMethod("argument_types", 
                callbackFactory.getMethod(JavaConstructor.class, "argument_types"));
        result.defineMethod("new_instance", 
                callbackFactory.getOptMethod(JavaConstructor.class, "new_instance"));
        
        return result;
    }

    public JavaConstructor(Ruby runtime, Constructor constructor) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaConstructor"));
        this.constructor = constructor;
    }

    public int getArity() {
        return constructor.getParameterTypes().length;
    }

    public IRubyObject new_instance(IRubyObject[] args) {
        if (args.length != getArity()) {
            throw new ArgumentError(getRuntime(), args.length, getArity());
        }
        Object[] constructorArguments = new Object[args.length];
        Class[] types = constructor.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            constructorArguments[i] = JavaUtil.convertArgument(args[i], types[i]);
        }
        try {
            Object result = constructor.newInstance(constructorArguments);
            return JavaObject.wrap(getRuntime(), result);

        } catch (IllegalArgumentException iae) {
            throw new TypeError(getRuntime(), "expected " + argument_types().inspect() +
                                              ", got [" + constructorArguments[0].getClass().getName() + ", ...]");
        } catch (IllegalAccessException iae) {
            throw new TypeError(getRuntime(), "illegal access");
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException((Exception) ite.getTargetException());
            Asserts.notReached();
            return null;
        } catch (InstantiationException ie) {
            throw new TypeError(getRuntime(), "can't make instance of " + constructor.getDeclaringClass().getName());
        }
    }

    protected String nameOnInspection() {
        return getType().toString();
    }

    protected Class[] parameterTypes() {
        return constructor.getParameterTypes();
    }
}
