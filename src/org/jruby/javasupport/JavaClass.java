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
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.exceptions.TypeError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NameError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;

public class JavaClass extends RubyObject {
    private final Class javaClass;

    private JavaClass(Ruby runtime, String name) {
        this(runtime, runtime.getJavaSupport().loadJavaClass(name));
    }

    public JavaClass(Ruby runtime, Class javaClass) {
        super(runtime, (RubyClass) runtime.getClasses().getClassFromPath("Java::JavaClass"));
        this.javaClass = javaClass;
    }

    public Class getValue() {
        return javaClass;
    }

    public static RubyClass createJavaClassClass(Ruby ruby, RubyModule javaModule) {
        RubyClass result = javaModule.defineClassUnder("JavaClass", 
                ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();
        
        result.includeModule(ruby.getClasses().getComparableModule());

        result.defineSingletonMethod("for_name", 
                callbackFactory.getSingletonMethod(JavaClass.class, "for_name", IRubyObject.class));
        result.defineMethod("public?", 
                callbackFactory.getMethod(JavaClass.class, "public_p"));
        result.defineMethod("final?", 
                callbackFactory.getMethod(JavaClass.class, "final_p"));
        result.defineMethod("interface?", 
                callbackFactory.getMethod(JavaClass.class, "interface_p"));
        result.defineMethod("array?", 
                callbackFactory.getMethod(JavaClass.class, "array_p"));
        result.defineMethod("name", 
                callbackFactory.getMethod(JavaClass.class, "name"));
        result.defineMethod("to_s", 
                callbackFactory.getMethod(JavaClass.class, "name"));
        result.defineMethod("superclass", 
                callbackFactory.getMethod(JavaClass.class, "superclass"));
        result.defineMethod("<=>", 
                callbackFactory.getMethod(JavaClass.class, "op_cmp", IRubyObject.class));
        result.defineMethod("java_instance_methods", 
                callbackFactory.getMethod(JavaClass.class, "java_instance_methods"));
        result.defineMethod("java_class_methods", 
                callbackFactory.getMethod(JavaClass.class, "java_class_methods"));
        result.defineMethod("java_method", 
                callbackFactory.getOptMethod(JavaClass.class, "java_method"));
        result.defineMethod("constructors", 
                callbackFactory.getMethod(JavaClass.class, "constructors"));
        result.defineMethod("constructor", 
                callbackFactory.getOptMethod(JavaClass.class, "constructor"));
        result.defineMethod("array_class", 
                callbackFactory.getMethod(JavaClass.class, "array_class"));
        result.defineMethod("new_array", 
                callbackFactory.getMethod(JavaClass.class, "new_array", IRubyObject.class));
        result.defineMethod("fields", 
                callbackFactory.getMethod(JavaClass.class, "fields"));
        result.defineMethod("field", 
                callbackFactory.getMethod(JavaClass.class, "field", IRubyObject.class));
        result.defineMethod("interfaces", 
                callbackFactory.getMethod(JavaClass.class, "interfaces"));
        result.defineMethod("primitive?", 
                callbackFactory.getMethod(JavaClass.class, "primitive_p"));
        result.defineMethod("assignable_from?", 
                callbackFactory.getMethod(JavaClass.class, "assignable_from_p", IRubyObject.class));
        result.defineMethod("component_type", 
                callbackFactory.getMethod(JavaClass.class, "component_type"));

        result.getMetaClass().undefineMethod("new");

        return result;
    }

    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return new JavaClass(recv.getRuntime(), name.asSymbol());
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

    public RubyBoolean array_p() {
        return RubyBoolean.newBoolean(runtime, javaClass.isArray());
    }

    public RubyString name() {
        return RubyString.newString(runtime, javaClass.getName());
    }

    public IRubyObject superclass() {
        Class superclass = javaClass.getSuperclass();
        if (superclass == null) {
            return runtime.getNil();
        }
        return new JavaClass(runtime, superclass.getName());
    }

    public RubyFixnum op_cmp(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw new TypeError(getRuntime(), "<=> requires JavaClass (" + other.getType() + " given)");
        }
        JavaClass otherClass = (JavaClass) other;
        if (this.javaClass == otherClass.javaClass) {
            return RubyFixnum.newFixnum(getRuntime(), 0);
        }
        if (otherClass.javaClass.isAssignableFrom(this.javaClass)) {
            return RubyFixnum.newFixnum(getRuntime(), -1);
        }
        return RubyFixnum.newFixnum(getRuntime(), 1);
    }

    public RubyArray java_instance_methods() {
        Method[] methods = javaClass.getMethods();
        RubyArray result = RubyArray.newArray(runtime, methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (! Modifier.isStatic(method.getModifiers())) {
                result.append(JavaMethod.create(runtime, method));
            }
        }
        return result;
    }

    public RubyArray java_class_methods() {
        Method[] methods = javaClass.getMethods();
        RubyArray result = RubyArray.newArray(runtime, methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (Modifier.isStatic(method.getModifiers())) {
                result.append(JavaMethod.create(runtime, method));
            }
        }
        return result;
    }

    public JavaMethod java_method(IRubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(getRuntime(), args.length, 1);
        }
        String methodName = args[0].asSymbol();
        Class[] argumentTypes = new Class[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            JavaClass type = for_name(this, args[i]);
            argumentTypes[i - 1] = type.javaClass;
        }
        return JavaMethod.create(runtime, javaClass, methodName, argumentTypes);
    }

    public RubyArray constructors() {
        Constructor[] constructors = javaClass.getConstructors();
        RubyArray result = RubyArray.newArray(getRuntime(), constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            result.append(new JavaConstructor(getRuntime(), constructors[i]));
        }
        return result;
    }

    public JavaConstructor constructor(IRubyObject[] args) {
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            String name = args[i].asSymbol();
            parameterTypes[i] = getRuntime().getJavaSupport().loadJavaClass(name);
        }
        Constructor constructor;
        try {
            constructor = javaClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException nsme) {
            throw new NameError(getRuntime(), "no matching java constructor");
        }
        return new JavaConstructor(getRuntime(), constructor);
    }

    public JavaClass array_class() {
        return new JavaClass(getRuntime(), Array.newInstance(javaClass, 0).getClass());
    }

    public JavaObject new_array(IRubyObject lengthArgument) {
        if (! (lengthArgument instanceof RubyInteger)) {
            throw new TypeError(getRuntime(), lengthArgument, getRuntime().getClasses().getIntegerClass());
        }
        int length = (int) ((RubyInteger) lengthArgument).getLongValue();
        return new JavaArray(getRuntime(), Array.newInstance(javaClass, length));
    }

    public RubyArray fields() {
        Field[] fields = javaClass.getFields();
        RubyArray result = RubyArray.newArray(getRuntime(), fields.length);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            result.append(RubyString.newString(getRuntime(), field.getName()));
        }
        return result;
    }

    public JavaField field(IRubyObject name) {
        String stringName = name.asSymbol();

        try {
            return new JavaField(getRuntime(),javaClass.getField(stringName));
        } catch (NoSuchFieldException nsfe) {
            throw new NameError(getRuntime(),
                                "undefined field '" + stringName + "' for class '" + javaClass.getName() + "'");
        }
    }

    public RubyArray interfaces() {
        Class[] interfaces = javaClass.getInterfaces();
        RubyArray result = RubyArray.newArray(getRuntime(), interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            result.append(RubyString.newString(getRuntime(), interfaces[i].getName()));
        }
        return result;
    }

    public RubyBoolean primitive_p() {
        return RubyBoolean.newBoolean(getRuntime(), isPrimitive());
    }

    public RubyBoolean assignable_from_p(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw new TypeError(getRuntime(), "assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class otherClass = ((JavaClass) other).getValue();

        if ((!javaClass.isPrimitive() && otherClass == Void.TYPE) ||
            javaClass.isAssignableFrom(otherClass)) {
            return getRuntime().getTrue();
        }
        otherClass = JavaUtil.primitiveToWrapper(otherClass);
        Class thisJavaClass = JavaUtil.primitiveToWrapper(javaClass);
        if (thisJavaClass.isAssignableFrom(otherClass)) {
            return getRuntime().getTrue();
        }
        if (Number.class.isAssignableFrom(thisJavaClass)) {
            if (Number.class.isAssignableFrom(otherClass)) {
                return getRuntime().getTrue();
            }
            if (otherClass.equals(Character.class)) {
                return getRuntime().getTrue();
            }
        }
        if (thisJavaClass.equals(Character.class)) {
            if (Number.class.isAssignableFrom(otherClass)) {
                return getRuntime().getTrue();
            }
        }
        return getRuntime().getFalse();
    }

    private boolean isPrimitive() {
        return javaClass.isPrimitive();
    }

    public JavaClass component_type() {
        if (! javaClass.isArray()) {
            throw new TypeError(getRuntime(), "not a java array-class");
        }
        return new JavaClass(getRuntime(), javaClass.getComponentType());
    }
}
