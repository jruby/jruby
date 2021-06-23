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
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyModule.undefinedMethodMessage;
import static org.jruby.util.CodegenUtils.getBoxType;
import static org.jruby.util.CodegenUtils.prettyParams;
import static org.jruby.util.RubyStringBuilder.ids;

// @JRubyClass(name="Java::JavaMethod")
public class JavaMethod extends JavaCallable {

    //private final static boolean USE_HANDLES = RubyInstanceConfig.USE_GENERATED_HANDLES;
    //private final static boolean HANDLE_DEBUG = false;

    private final Method method;
    private final Class<?> boxedReturnType;
    private final boolean isFinal;
    private final JavaUtil.JavaConverter returnConverter;

    public final Method getValue() { return method; }

    public static RubyClass createJavaMethodClass(Ruby runtime, RubyModule javaModule) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result =
            javaModule.defineClassUnder("JavaMethod", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        JavaAccessibleObject.registerRubyMethods(runtime, result);
        JavaCallable.registerRubyMethods(runtime, result);

        result.defineAnnotatedMethods(JavaMethod.class);

        return result;
    }

    public JavaMethod(Ruby runtime, Method method) {
        super(runtime, null, method.getParameterTypes());
        this.method = method;
        this.isFinal = Modifier.isFinal(method.getModifiers());
        final Class<?> returnType = method.getReturnType();
        if (returnType.isPrimitive() && returnType != void.class) {
            this.boxedReturnType = getBoxType(returnType);
        } else {
            this.boxedReturnType = returnType;
        }

        // Special classes like Collections.EMPTY_LIST are inner classes that are private but
        // implement public interfaces.  Their methods are all public methods for the public
        // interface.  Let these public methods execute via setAccessible(true).
        if (RubyInstanceConfig.SET_ACCESSIBLE) {
            // we should be able to setAccessible ok...
            try {
                if ( Modifier.isPublic(method.getModifiers()) &&
                    ! Modifier.isPublic(method.getDeclaringClass().getModifiers()) ) {
                    Java.trySetAccessible(method);
                }
            } catch (SecurityException se) {
                // we shouldn't get here if JavaClass.CAN_SET_ACCESSIBLE is doing
                // what it should, so we warn.
               runtime.getWarnings().warn("failed to setAccessible: " + method + ", exception follows: " + se.getMessage());
            }
        }

        returnConverter = JavaUtil.getJavaConverter(returnType);
    }

    public static JavaMethod create(Ruby runtime, Method method) {
        return new JavaMethod(runtime, method);
    }

    @Deprecated // no-longer used
    public static JavaMethod create(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        try {
            return create(runtime, javaClass.getMethod(methodName, argumentTypes));
        }
        catch (NoSuchMethodException e) {
            throw runtime.newNameError(undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, javaClass.getName()), false), methodName);
        }
    }

    @Deprecated // no-longer used
    public static JavaMethod createDeclared(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        try {
            return create(runtime, javaClass.getDeclaredMethod(methodName, argumentTypes));
        }
        catch (NoSuchMethodException e) {
            throw runtime.newNameError(undefinedMethodMessage(runtime, ids(runtime, methodName), ids(runtime, javaClass.getName()), false), methodName);
        }
    }

    public static JavaMethod getMatchingDeclaredMethod(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods.  also, the getDeclared calls may throw SecurityException if
        // we're running under a restrictive security policy.
        try {
            return new JavaMethod(runtime, javaClass.getDeclaredMethod(methodName, argumentTypes));
        }
        catch (NoSuchMethodException e) {
            // search through all declared methods to find a closest match
            MethodSearch: for ( Method method : javaClass.getDeclaredMethods() ) {
                if ( method.getName().equals(methodName) ) {
                    Class<?>[] targetTypes = method.getParameterTypes();

                    // for zero args case we can stop searching
                    if (targetTypes.length == 0 && argumentTypes.length == 0) {
                        return new JavaMethod(runtime, method);
                    }

                    TypeScan: for (int i = 0; i < argumentTypes.length; i++) {
                        if (i >= targetTypes.length) continue MethodSearch;

                        if (targetTypes[i].isAssignableFrom(argumentTypes[i])) {
                            continue TypeScan;
                        } else {
                            continue MethodSearch;
                        }
                    }

                    // if we get here, we found a matching method, use it
                    // TODO: choose narrowest method by continuing to search
                    return new JavaMethod(runtime, method);
                }
            }
        }
        // no matching method found
        return null;
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof JavaMethod && this.method.equals( ((JavaMethod) other).method );
    }

    @Override
    public final int hashCode() {
        return method.hashCode();
    }

    @JRubyMethod
    @Override
    public RubyString name(ThreadContext context) {
        return context.runtime.newString(method.getName());
    }

    @JRubyMethod(name = "public?")
    @Override
    public RubyBoolean public_p(ThreadContext context) {
        return context.runtime.newBoolean(Modifier.isPublic(method.getModifiers()));
    }

    @JRubyMethod(name = "final?")
    public RubyBoolean final_p(ThreadContext context) {
        return context.runtime.newBoolean(Modifier.isFinal(method.getModifiers()));
    }

    @JRubyMethod(rest = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject[] args) {
        checkArity(context, args.length - 1);

        final IRubyObject invokee = args[0];
        final Object[] arguments = convertArguments(args, 1);

        if ( invokee.isNil() ) {
            return invokeWithExceptionHandling(context, method, null, arguments);
        }

        final Object javaInvokee;
        if (!isStatic()) {
            javaInvokee = JavaUtil.unwrapJavaValue(invokee);
            if ( javaInvokee == null ) {
                throw context.runtime.newTypeError("invokee not a java object");
            }

            if ( ! method.getDeclaringClass().isInstance(javaInvokee) ) {
                throw context.runtime.newTypeError(
                    "invokee not instance of method's class" +
                    " (got" + javaInvokee.getClass().getName() +
                    " wanted " + method.getDeclaringClass().getName() + ")");
            }

            //
            // this test really means, that this is a ruby-defined subclass of a java class
            //
            if ( javaInvokee instanceof ReifiedJavaProxy &&
                // don't bother to check if final method, it won't
                // be there (not generated, can't be!)
                ! isFinal ) {
                JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
                JavaProxyMethod jpm = jpc.getMethod( method.getName(), parameterTypes );
                if ( jpm != null && jpm.hasSuperImplementation() ) {
                    return invokeWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, arguments);
                }
            }
        }
        else {
            javaInvokee = null;
        }

        return invokeWithExceptionHandling(context, method, javaInvokee, arguments);
    }

    @JRubyMethod(rest = true)
    public IRubyObject invoke_static(ThreadContext context, IRubyObject[] args) {
        checkArity(context, args.length);

        final Object[] arguments = convertArguments(args, 0);
        return invokeWithExceptionHandling(context, method, null, arguments);
    }

    @JRubyMethod
    @SuppressWarnings("deprecation")
    public IRubyObject return_type(ThreadContext context) {
        Class<?> klass = method.getReturnType();

        if (klass.equals(void.class)) {
            return context.runtime.getNil();
        }
        return Java.getProxyClass(context.runtime, klass);
    }

    @JRubyMethod
    public IRubyObject type_parameters(ThreadContext context) {
        return Java.getInstance(context.runtime, method.getTypeParameters());
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee, Object[] args) {
        checkArity(context, args.length);
        checkInstanceof(context.runtime, javaInvokee);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee, args);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee, args);
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(context, 0);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee);
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee, Object arg0) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(context, 1);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee, arg0);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0);
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee, Object arg0, Object arg1) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(context, 2);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee, arg0, arg1);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1);
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(context, 3);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee, arg0, arg1, arg2);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1, arg2);
    }

    public IRubyObject invokeDirect(ThreadContext context, Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(context, 4);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(context, javaInvokee, arg0, arg1, arg2, arg3);
        }

        return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1, arg2, arg3);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context, Object[] args) {
        checkArity(context, args.length);
        return invokeDirectWithExceptionHandling(context, method, null, args);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context) {
        checkArity(context, 0);
        return invokeDirectWithExceptionHandling(context, method, null);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context, Object arg0) {
        checkArity(context, 1);
        return invokeDirectWithExceptionHandling(context, method, null, arg0);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context, Object arg0, Object arg1) {
        checkArity(context, 2);
        return invokeDirectWithExceptionHandling(context, method, null, arg0, arg1);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context, Object arg0, Object arg1, Object arg2) {
        checkArity(context, 3);
        return invokeDirectWithExceptionHandling(context, method, null, arg0, arg1, arg2);
    }

    public IRubyObject invokeStaticDirect(ThreadContext context, Object arg0, Object arg1, Object arg2, Object arg3) {
        checkArity(context, 4);
        return invokeDirectWithExceptionHandling(context, method, null, arg0, arg1, arg2, arg3);
    }

    private void checkInstanceof(final Ruby runtime, Object javaInvokee) throws RaiseException {
        if (!method.getDeclaringClass().isInstance(javaInvokee)) {
            throw runtime.newTypeError("invokee not instance of method's class (" + "got" + javaInvokee.getClass().getName() + " wanted " + method.getDeclaringClass().getName() + ")");
        }
    }

    private IRubyObject invokeWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return returnConverter.convert(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectSuperWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object... arguments) {
        // super calls from proxies must use reflected method
        // FIXME: possible to make handles do the superclass call?
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee) {
        try {
            Object result = method.invoke(javaInvokee);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object arg0) {
        try {
            Object result = method.invoke(javaInvokee, arg0);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arg0);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object arg0, Object arg1) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arg0, arg1);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1, arg2);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arg0, arg1, arg2);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(ThreadContext context, Method method, Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1, arg2, arg3);
            return convertReturn(context.runtime, result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, method, arg0, arg1, arg2, arg3);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, method);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    private IRubyObject convertReturn(Ruby runtime, Object result) {
        if (result != null && result.getClass() != boxedReturnType) {
            // actual type does not exactly match method return type, re-get converter
            // FIXME: when the only autoconversions are primitives, this won't be needed
            return JavaUtil.convertJavaToUsableRubyObject(runtime, result);
        }
        return JavaUtil.convertJavaToUsableRubyObjectWithConverter(runtime, result, returnConverter);
    }

    //@Override
    //public final int getArity() {
    //    return parameterTypes.length;
    //}

    //@Override
    //public final Class<?>[] getParameterTypes() {
    //    return parameterTypes;
    //}

    public String getName() {
        return method.getName();
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return method.getGenericParameterTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return method.getGenericExceptionTypes();
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return method.getParameterAnnotations();
    }

    @Override
    public final boolean isVarArgs() {
        return method.isVarArgs();
    }

    @JRubyMethod(name = "static?")
    public RubyBoolean static_p(ThreadContext context) {
        return context.runtime.newBoolean(isStatic());
    }

    private boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    @Override
    public final int getModifiers() {
        return method.getModifiers();
    }

    @Override
    public String toGenericString() {
        return method.toGenericString();
    }

    @Override
    public final AccessibleObject accessibleObject() {
        return method;
    }

    private boolean mightBeProxy(Object javaInvokee) {
        // this test really means, that this is a ruby-defined subclass of a java class
        return javaInvokee instanceof ReifiedJavaProxy && !isFinal;
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee, Object... args) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, args);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee, args);
        }
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee);
        }
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee, Object arg0) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, arg0);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0);
        }
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee, Object arg0, Object arg1) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, arg0, arg1);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1);
        }
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, arg0, arg1, arg2);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1, arg2);
        }
    }

    private IRubyObject tryProxyInvocation(ThreadContext context, Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        JavaProxyClass jpc = ((ReifiedJavaProxy) javaInvokee).___jruby$proxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, arg0, arg1, arg2, arg3);
        } else {
            return invokeDirectWithExceptionHandling(context, method, javaInvokee, arg0, arg1, arg2, arg3);
        }
    }

    public static RaiseException newMethodNotFoundError(Ruby runtime, Class target, String prettyName, String simpleName) {
        return runtime.newNameError("java method not found: " + target.getName() + "." + prettyName, simpleName);
    }

    public static RaiseException newArgSizeMismatchError(Ruby runtime, Class ... argTypes) {
        return runtime.newArgumentError("argument count mismatch for method signature " + prettyParams(argTypes));
    }
}
