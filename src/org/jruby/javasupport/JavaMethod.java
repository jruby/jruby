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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.NameError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaMethod extends JavaCallable {
    private final Method method;

    public static RubyClass createJavaMethodClass(Ruby runtime, RubyModule javaModule) {
        RubyClass result = 
            javaModule.defineClassUnder("JavaMethod", runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory();

        result.defineMethod("name", 
                callbackFactory.getMethod(JavaMethod.class, "name"));
        result.defineMethod("arity", 
                callbackFactory.getMethod(JavaMethod.class, "arity"));
        result.defineMethod("public?", 
                callbackFactory.getMethod(JavaMethod.class, "public_p"));
        result.defineMethod("final?", 
                callbackFactory.getMethod(JavaMethod.class, "final_p"));
        result.defineMethod("static?", 
                callbackFactory.getMethod(JavaMethod.class, "static_p"));
        result.defineMethod("invoke", 
                callbackFactory.getOptMethod(JavaMethod.class, "invoke"));
        result.defineMethod("invoke_static", 
                callbackFactory.getOptMethod(JavaMethod.class, "invoke_static"));
        result.defineMethod("argument_types", 
                callbackFactory.getMethod(JavaMethod.class, "argument_types"));
        result.defineMethod("inspect", 
                callbackFactory.getMethod(JavaMethod.class, "inspect"));
        result.defineMethod("return_type", 
                callbackFactory.getMethod(JavaMethod.class, "return_type"));

        return result;
    }

    public JavaMethod(Ruby runtime, Method method) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaMethod"));
        this.method = method;
    }

    public static JavaMethod create(Ruby runtime, Method method) {
        return new JavaMethod(runtime, method);
    }

    public static JavaMethod create(Ruby runtime, Class javaClass, String methodName, Class[] argumentTypes) {
        try {
            Method method = javaClass.getMethod(methodName, argumentTypes);
            return create(runtime, method);
        } catch (NoSuchMethodException e) {
            throw new NameError(runtime, "undefined method '" + methodName + "' for class '" + javaClass.getName() + "'");
        }
    }

    public RubyString name() {
        return getRuntime().newString(method.getName());
    }

    protected int getArity() {
        return method.getParameterTypes().length;
    }

    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(method.getModifiers()));
    }

    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(method.getModifiers()));
    }

    public IRubyObject invoke(IRubyObject[] args) {
        if (args.length != 1 + getArity()) {
            throw getRuntime().newArgumentError(args.length, 1 + getArity());
        }
        IRubyObject invokee = args[0];
        if (! (invokee instanceof JavaObject)) {
            throw getRuntime().newTypeError("invokee not a java object");
        }
        Object javaInvokee = ((JavaObject) invokee).getValue();
        Object[] arguments = new Object[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        convertArguments(arguments);

        if (! method.getDeclaringClass().isInstance(javaInvokee)) {
            throw getRuntime().newTypeError("invokee not instance of method's class (" +
                                              "got" + javaInvokee.getClass().getName() + " wanted " +
                                              method.getDeclaringClass().getName() + ")");
        }
        return invokeWithExceptionHandling(javaInvokee, arguments);
    }

    public IRubyObject invoke_static(IRubyObject[] args) {
        if (args.length != getArity()) {
            throw getRuntime().newArgumentError(args.length, getArity());
        }
        Object[] arguments = new Object[args.length];
        System.arraycopy(args, 0, arguments, 0, arguments.length);
        convertArguments(arguments);
        return invokeWithExceptionHandling(null, arguments);
    }

    public IRubyObject return_type() {
        String result = method.getReturnType().getName();
        if (result.equals("void")) {
            return getRuntime().getNil();
        }
        return getRuntime().newString(result);
    }

    private IRubyObject invokeWithExceptionHandling(Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return JavaObject.wrap(getRuntime(), result);
        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError("expected " + argument_types().inspect());
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access on '" + method.getName() + "': " + iae.getMessage());
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException(ite.getTargetException());
            // This point is only reached if there was an exception handler installed.
            return getRuntime().getNil();
        }
    }

    private void convertArguments(Object[] arguments) {
        Class[] parameterTypes = parameterTypes();
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = JavaUtil.convertArgument(arguments[i], parameterTypes[i]);
        }
    }

    protected Class[] parameterTypes() {
        return method.getParameterTypes();
    }

    protected String nameOnInspection() {
        return "#<" + getType().toString() + "/" + method.getName() + "(";
    }

    public RubyBoolean static_p() {
        return getRuntime().newBoolean(isStatic());
    }

    private boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }
}
