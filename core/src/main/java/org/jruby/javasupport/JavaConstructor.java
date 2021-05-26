/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// @JRubyClass(name="Java::JavaConstructor")
public class JavaConstructor extends JavaCallable {

    private final Constructor<?> constructor;
    //private final JavaUtil.JavaConverter objectConverter;

    public final Constructor getValue() { return constructor; }

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
        super(runtime, null, constructor.getParameterTypes());
        this.constructor = constructor;
        //this.objectConverter = JavaUtil.getJavaConverter(constructor.getDeclaringClass());
    }

    public static JavaConstructor create(Ruby runtime, Constructor<?> constructor) {
        return new JavaConstructor(runtime, constructor);
    }

    public static JavaConstructor getMatchingConstructor(final Ruby runtime,
        final Class<?> javaClass, final Class<?>[] argumentTypes) {
        try {
            return create(runtime, javaClass.getConstructor(argumentTypes));
        }
        catch (NoSuchMethodException e) {
            final int argLength = argumentTypes.length;
            // Java reflection does not allow retrieving constructors like methods
            Search: for (Constructor<?> ctor : javaClass.getConstructors()) {
                final Class<?>[] ctorTypes = ctor.getParameterTypes();
                final int ctorLength = ctorTypes.length;

                if ( ctorLength != argLength ) continue Search;
                // for zero args case we can stop searching
                if ( ctorLength == 0 && argLength == 0 ) {
                    return create(runtime, ctor);
                }

                boolean found = true;
                TypeScan: for ( int i = 0; i < argLength; i++ ) {
                    //if ( i >= ctorLength ) found = false;
                    if ( ctorTypes[i].isAssignableFrom(argumentTypes[i]) ) {
                        found = true; // continue TypeScan;
                    } else {
                        continue Search; // not-found
                    }
                }

                // if we get here, we found a matching method, use it
                // TODO: choose narrowest method by continuing to search
                if ( found ) return create(runtime, ctor);
            }
        }
        return null; // no matching ctor found
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof JavaConstructor &&
            this.constructor.equals( ((JavaConstructor) other).constructor );
    }

    @Override
    public final int hashCode() {
        return constructor.hashCode();
    }

    //@Override
    //public final int getArity() {
    //    return parameterTypes.length;
    //}

    //@Override
    //public final Class<?>[] getParameterTypes() {
    //    return parameterTypes;
    //}

    @Override
    public final Class<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return constructor.getGenericParameterTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return constructor.getGenericExceptionTypes();
    }

    public Annotation[][] getParameterAnnotations() {
        return constructor.getParameterAnnotations();
    }

    @Override
    public final boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    @Override
    public final int getModifiers() {
        return constructor.getModifiers();
    }

    public String toGenericString() {
        return constructor.toGenericString();
    }

    public Class<?> getDeclaringClass() {
        return constructor.getDeclaringClass();
    }

    public AccessibleObject accessibleObject() {
        return constructor;
    }

    @JRubyMethod
    public IRubyObject type_parameters(ThreadContext context) {
        return Java.getInstance(context.runtime, constructor.getTypeParameters());
    }

    @JRubyMethod
    public IRubyObject return_type(ThreadContext context) {
        return context.runtime.getNil();
    }

    @JRubyMethod
    @SuppressWarnings("deprecation")
    public IRubyObject declaring_class(ThreadContext context) {
        return Java.getProxyClass(context.runtime, getDeclaringClass());
    }

    @JRubyMethod(rest = true)
    public final IRubyObject new_instance(ThreadContext context, final IRubyObject[] args) {
        checkArity(context, args.length);

        return newInstanceExactArity(context, convertArguments(args));
    }

    public final IRubyObject new_instance(ThreadContext context, final Object[] arguments) {
        checkArity(context, arguments.length);

        return newInstanceExactArity(context, arguments);
    }

    private IRubyObject newInstanceExactArity(ThreadContext context, Object[] arguments) {
        try {
            Object result = constructor.newInstance(arguments);
            return JavaObject.wrap(context.runtime, result);
        }
        catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, false, arguments);
        }
        catch (IllegalAccessException iae) {
            throw context.runtime.newTypeError("illegal access");
        }
        catch (InvocationTargetException ite) {
            context.runtime.getJavaSupport().handleNativeException(ite.getTargetException(), constructor); // NOTE: we no longer unwrap
            // not reached
            assert false;
            return null;
        }
        catch (InstantiationException ie) {
            throw context.runtime.newTypeError("can't make instance of " + constructor.getDeclaringClass().getName());
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object... arguments) {
        checkArity(context, arguments.length);

        try {
            return constructor.newInstance(arguments);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context) {
        checkArity(context, 0);

        try {
            return constructor.newInstance();
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0) {
        checkArity(context, 1);

        try {
            return constructor.newInstance(arg0);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1) {
        checkArity(context, 2);

        try {
            return constructor.newInstance(arg0, arg1);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1, Object arg2) {
        checkArity(context, 3);

        try {
            return constructor.newInstance(arg0, arg1, arg2);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1, arg2);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1, Object arg2, Object arg3) {
        checkArity(context, 4);

        try {
            return constructor.newInstance(arg0, arg1, arg2, arg3);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1, arg2, arg3);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    boolean isConstructor() { return true; } // for error message in base class

}
