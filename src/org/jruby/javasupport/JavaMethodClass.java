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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubyBoolean;
import org.jruby.RubyJavaObject;
import org.jruby.util.Asserts;
import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

public class JavaMethodClass extends JavaCallable implements IndexCallable {
    private final Method method;

    private static final int NAME = 1;
    private static final int ARITY = 2;
    private static final int PUBLIC_P = 3;
    private static final int FINAL_P = 4;
    private static final int INVOKE = 5;
    private static final int INVOKE_STATIC = 6;
    private static final int ARGUMENT_TYPES = 7;
    private static final int INSPECT = 8;
    private static final int STATIC_P = 9;

    public static RubyClass createJavaMethodClass(Ruby runtime, RubyModule javaModule) {
        RubyClass javaMethodClass =
                javaModule.defineClassUnder("JavaMethod", runtime.getClasses().getObjectClass());
        javaMethodClass.defineMethod("name", IndexedCallback.create(NAME, 0));
        javaMethodClass.defineMethod("arity", IndexedCallback.create(ARITY, 0));
        javaMethodClass.defineMethod("public?", IndexedCallback.create(PUBLIC_P, 0));
        javaMethodClass.defineMethod("final?", IndexedCallback.create(FINAL_P, 0));
        javaMethodClass.defineMethod("invoke", IndexedCallback.createOptional(INVOKE, 1));
        javaMethodClass.defineMethod("invoke_static", IndexedCallback.createOptional(INVOKE_STATIC));
        javaMethodClass.defineMethod("argument_types", IndexedCallback.create(ARGUMENT_TYPES, 0));
        javaMethodClass.defineMethod("inspect", IndexedCallback.create(INSPECT, 0));
        javaMethodClass.defineMethod("static?", IndexedCallback.create(STATIC_P, 0));

        return javaMethodClass;
    }

    public JavaMethodClass(Ruby runtime, Method method) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaMethod"));
        this.method = method;
    }

    public static JavaMethodClass create(Ruby runtime, Method method) {
        return new JavaMethodClass(runtime, method);
    }

    public static JavaMethodClass create(Ruby runtime, Class javaClass, String methodName, Class[] argumentTypes) {
        try {
            Method method = javaClass.getMethod(methodName, argumentTypes);
            return create(runtime, method);
        } catch (NoSuchMethodException e) {
            throw new NameError(runtime, "undefined method '" + methodName + "' for class '" + javaClass.getName() + "'");
        }
    }

    public RubyString name() {
        return RubyString.newString(getRuntime(), method.getName());
    }

    protected int getArity() {
        return method.getParameterTypes().length;
    }

    public RubyBoolean public_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isPublic(method.getModifiers()));
    }

    public RubyBoolean final_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isFinal(method.getModifiers()));
    }

    public IRubyObject invoke(IRubyObject[] args) {
        if (args.length != 1 + getArity()) {
            throw new ArgumentError(getRuntime(), args.length, 1 + getArity());
        }
        IRubyObject invokee = args[0];
        if (! (invokee instanceof RubyJavaObject)) {
            throw new TypeError(getRuntime(), "invokee not a java object");
        }
        Object javaInvokee = ((RubyJavaObject) invokee).getValue();
        Object[] arguments = new Object[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        convertArguments(arguments);
        return invokeWithExceptionHandling(javaInvokee, arguments);
    }

    private IRubyObject invoke_static(IRubyObject[] args) {
        if (args.length != getArity()) {
            throw new ArgumentError(getRuntime(), args.length, getArity());
        }
        Object[] arguments = new Object[args.length];
        System.arraycopy(args, 0, arguments, 0, arguments.length);
        convertArguments(arguments);
        return invokeWithExceptionHandling(null, arguments);
    }

    private IRubyObject invokeWithExceptionHandling(Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return new RubyJavaObject(runtime, runtime.getClasses().getJavaObjectClass(), result);
        } catch (IllegalArgumentException iae) {
            throw new TypeError(getRuntime(), "expected " + argument_types().inspect());
        } catch (IllegalAccessException iae) {
            // FIXME: what's the best exception to throw here?
            throw new TypeError(getRuntime(), "illegal access");
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException((Exception) ite.getTargetException());
            Asserts.notReached();
            return null;
        }
    }

    private void convertArguments(Object[] arguments) {
        Class[] parameterTypes = parameterTypes();
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            Class parameterType = parameterTypes[i];
            arguments[i] = JavaUtil.convertArgument(argument, parameterType);
        }
    }

    protected Class[] parameterTypes() {
        return method.getParameterTypes();
    }

    protected String nameOnInspection() {
        return "#<" + getType().toString() + "/" + method.getName() + "(";
    }

    public RubyBoolean static_p() {
        return RubyBoolean.newBoolean(getRuntime(), isStatic());
    }

    private boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case NAME :
                return name();
            case ARITY :
                return arity();
            case PUBLIC_P :
                return public_p();
            case FINAL_P :
                return final_p();
            case INVOKE :
                return invoke(args);
            case INVOKE_STATIC :
                return invoke_static(args);
            case ARGUMENT_TYPES :
                return argument_types();
            case INSPECT :
                return inspect();
            case STATIC_P :
                return static_p();
            default :
                return super.callIndexed(index, args);
        }
    }
}
