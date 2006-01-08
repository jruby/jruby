/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jruby.IRuby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaMethod extends JavaCallable {
    private final Method method;

    public static RubyClass createJavaMethodClass(IRuby runtime, RubyModule javaModule) {
        RubyClass result = 
            javaModule.defineClassUnder("JavaMethod", runtime.getObject());
        CallbackFactory callbackFactory = runtime.callbackFactory(JavaMethod.class);

        JavaAccessibleObject.registerRubyMethods(runtime, result);
        
        result.defineMethod("name", 
                callbackFactory.getMethod("name"));
        result.defineMethod("arity", 
                callbackFactory.getMethod("arity"));
        result.defineMethod("public?", 
                callbackFactory.getMethod("public_p"));
        result.defineMethod("final?", 
                callbackFactory.getMethod("final_p"));
        result.defineMethod("static?", 
                callbackFactory.getMethod("static_p"));
        result.defineMethod("invoke", 
                callbackFactory.getOptMethod("invoke"));
        result.defineMethod("invoke_static", 
                callbackFactory.getOptMethod("invoke_static"));
        result.defineMethod("argument_types", 
                callbackFactory.getMethod("argument_types"));
        result.defineMethod("inspect", 
                callbackFactory.getMethod("inspect"));
        result.defineMethod("return_type", 
                callbackFactory.getMethod("return_type"));

        return result;
    }

    public JavaMethod(IRuby runtime, Method method) {
        super(runtime, (RubyClass) runtime.getModule("Java").getClass("JavaMethod"));
        this.method = method;

        // Special classes like Collections.EMPTY_LIST are inner classes that are private but 
        // implement public interfaces.  Their methods are all public methods for the public 
        // interface.  Let these public methods execute via setAccessible(true). 
        if (Modifier.isPublic(method.getModifiers()) &&
            Modifier.isPublic(method.getClass().getModifiers()) &&
            !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            accesibleObject().setAccessible(true);
        }
    }

    public static JavaMethod create(IRuby runtime, Method method) {
        return new JavaMethod(runtime, method);
    }

    public static JavaMethod create(IRuby runtime, Class javaClass, String methodName, Class[] argumentTypes) {
        try {
            Method method = javaClass.getMethod(methodName, argumentTypes);
            return create(runtime, method);
        } catch (NoSuchMethodException e) {
            throw runtime.newNameError("undefined method '" + methodName + "' for class '" + javaClass.getName() + "'");
        }
    }

    public static JavaMethod createDeclared(IRuby runtime, Class javaClass, String methodName, Class[] argumentTypes) {
        try {
            Method method = javaClass.getDeclaredMethod(methodName, argumentTypes);
            return create(runtime, method);
        } catch (NoSuchMethodException e) {
            throw runtime.newNameError("undefined method '" + methodName + "' for class '" + javaClass.getName() + "'");
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
        Class klass = method.getReturnType();
        
        if (klass.equals(void.class)) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), klass);
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

    protected int getModifiers() {
        return method.getModifiers();
    }

    protected AccessibleObject accesibleObject() {
        return method;
    }
}
