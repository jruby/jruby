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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="Java::JavaConstructor")
public class JavaConstructor extends JavaCallable {
    private final Constructor<?> constructor;
    private final JavaUtil.JavaConverter objectConverter;

    public Object getValue() {
        return constructor;
    }

    public static RubyClass createJavaConstructorClass(Ruby runtime, RubyModule javaModule) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result =
                javaModule.defineClassUnder("JavaConstructor", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        JavaAccessibleObject.registerRubyMethods(runtime, result);
        JavaCallable.registerRubyMethods(runtime, result);
        
        result.defineAnnotatedMethods(JavaConstructor.class);
        
        return result;
    }

    public JavaConstructor(Ruby runtime, Constructor<?> constructor) {
        super(runtime, runtime.getJavaSupport().getJavaConstructorClass(), constructor.getParameterTypes());
        this.constructor = constructor;
        
        this.objectConverter = JavaUtil.getJavaConverter(constructor.getDeclaringClass());
    }

    public static JavaConstructor create(Ruby runtime, Constructor<?> constructor) {
        return new JavaConstructor(runtime, constructor);
    }
    
    public static JavaConstructor getMatchingConstructor(Ruby runtime, Class<?> javaClass, Class<?>[] argumentTypes) {
        try {
            return create(runtime, javaClass.getConstructor(argumentTypes));
        } catch (NoSuchMethodException e) {
            // Java reflection does not allow retrieving constructors like methods
            CtorSearch: for (Constructor<?> ctor : javaClass.getConstructors()) {
                Class<?>[] targetTypes = ctor.getParameterTypes();
                
                // for zero args case we can stop searching
                if (targetTypes.length != argumentTypes.length) {
                    continue CtorSearch;
                } else if (targetTypes.length == 0 && argumentTypes.length == 0) {
                    return create(runtime, ctor);
                } else {
                    boolean found = true;
                    
                    TypeScan: for (int i = 0; i < argumentTypes.length; i++) {
                        if (i >= targetTypes.length) found = false;
                        
                        if (targetTypes[i].isAssignableFrom(argumentTypes[i])) {
                            found = true;
                            continue TypeScan;
                        } else {
                            found = false;
                            continue CtorSearch;
                        }
                    }

                    // if we get here, we found a matching method, use it
                    // TODO: choose narrowest method by continuing to search
                    if (found) {
                        return create(runtime, ctor);
                    }
                }
            }
        }
        // no matching ctor found
        return null;
    }

    public boolean equals(Object other) {
        return other instanceof JavaConstructor &&
            this.constructor == ((JavaConstructor)other).constructor;
    }
    
    public int hashCode() {
        return constructor.hashCode();
    }

    public int getArity() {
        return parameterTypes.length;
    }
    
    protected String nameOnInspection() {
        return getType().toString();
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    public Type[] getGenericParameterTypes() {
        return constructor.getGenericParameterTypes();
    }

    public Type[] getGenericExceptionTypes() {
        return constructor.getGenericExceptionTypes();
    }

    public Annotation[][] getParameterAnnotations() {
        return constructor.getParameterAnnotations();
    }
    
    public boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    public int getModifiers() {
        return constructor.getModifiers();
    }
    
    public String toGenericString() {
        return constructor.toGenericString();
    }

    public AccessibleObject accessibleObject() {
        return constructor;
    }
    
    @JRubyMethod
    public IRubyObject type_parameters() {
        return Java.getInstance(getRuntime(), constructor.getTypeParameters());
    }

    @JRubyMethod
    public IRubyObject return_type() {
        return getRuntime().getNil();
    }

    @JRubyMethod(rest = true)
    public IRubyObject new_instance(IRubyObject[] args) {
        int length = args.length;
        Class<?>[] types = parameterTypes;
        if (length != types.length) {
            throw getRuntime().newArgumentError(length, types.length);
        }
        Object[] constructorArguments = new Object[length];
        for (int i = length; --i >= 0; ) {
            constructorArguments[i] = args[i].toJava(types[i]);
        }
        try {
            Object result = constructor.newInstance(constructorArguments);
            return JavaObject.wrap(getRuntime(), result);

        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError("expected " + argument_types().inspect() +
                                              ", got [" + constructorArguments[0].getClass().getName() + ", ...]");
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access");
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException(ite.getTargetException(), constructor);
            // not reached
            assert false;
            return null;
        } catch (InstantiationException ie) {
            throw getRuntime().newTypeError("can't make instance of " + constructor.getDeclaringClass().getName());
        }
    }

    public IRubyObject new_instance(Object[] arguments) {
        checkArity(arguments.length);

        try {
            Object result = constructor.newInstance(arguments);
            return JavaObject.wrap(getRuntime(), result);
        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError("expected " + argument_types().inspect() +
                                              ", got [" + arguments[0].getClass().getName() + ", ...]");
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access");
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException(ite.getTargetException(), constructor);
            // not reached
            assert false;
            return null;
        } catch (InstantiationException ie) {
            throw getRuntime().newTypeError("can't make instance of " + constructor.getDeclaringClass().getName());
        }
    }

    public Object newInstanceDirect(Object... arguments) {
        checkArity(arguments.length);

        try {
            return constructor.newInstance(arguments);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    public Object newInstanceDirect() {
        checkArity(0);

        try {
            return constructor.newInstance();
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    public Object newInstanceDirect(Object arg0) {
        checkArity(1);

        try {
            return constructor.newInstance(arg0);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae, arg0);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    public Object newInstanceDirect(Object arg0, Object arg1) {
        checkArity(2);

        try {
            return constructor.newInstance(arg0, arg1);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae, arg0, arg1);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    public Object newInstanceDirect(Object arg0, Object arg1, Object arg2) {
        checkArity(3);

        try {
            return constructor.newInstance(arg0, arg1, arg2);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae, arg0, arg1, arg2);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    public Object newInstanceDirect(Object arg0, Object arg1, Object arg2, Object arg3) {
        checkArity(4);

        try {
            return constructor.newInstance(arg0, arg1, arg2, arg3);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(iae, arg0, arg1, arg2, arg3);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, constructor);
        } catch (Throwable t) {
            return handleThrowable(t, constructor);
        }
    }

    private IRubyObject handleIllegalAccessEx(IllegalAccessException iae) {
        throw getRuntime().newTypeError("illegal access on constructor for type " + constructor.getDeclaringClass().getSimpleName() + ": " + iae.getMessage());
    }

    private IRubyObject handlelIllegalArgumentEx(IllegalArgumentException iae, Object... arguments) {
        throw getRuntime().newTypeError(
                "for constructor of type " +
                constructor.getDeclaringClass().getSimpleName() +
                " expected " +
                argument_types().inspect() +
                "; got: " +
                dumpArgTypes(arguments) +
                "; error: " +
                iae.getMessage());
    }
}
