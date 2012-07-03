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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.proxy.InternalJavaProxy;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

@JRubyClass(name="Java::JavaMethod")
public class JavaMethod extends JavaCallable {

    private static final Logger LOG = LoggerFactory.getLogger("JavaMethod");

    private final static boolean USE_HANDLES = RubyInstanceConfig.USE_GENERATED_HANDLES;
    private final static boolean HANDLE_DEBUG = false;
    private final Method method;
    private final Class boxedReturnType;
    private final boolean isFinal;
    private final JavaUtil.JavaConverter returnConverter;

    public Object getValue() {
        return method;
    }

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
        super(runtime, runtime.getJavaSupport().getJavaMethodClass(), method.getParameterTypes());
        this.method = method;
        this.isFinal = Modifier.isFinal(method.getModifiers());
        if (method.getReturnType().isPrimitive() && method.getReturnType() != void.class) {
            this.boxedReturnType = CodegenUtils.getBoxType(method.getReturnType());
        } else {
            this.boxedReturnType = method.getReturnType();
        }

        boolean methodIsPublic = Modifier.isPublic(method.getModifiers());
        boolean classIsPublic = Modifier.isPublic(method.getDeclaringClass().getModifiers());

        // Special classes like Collections.EMPTY_LIST are inner classes that are private but 
        // implement public interfaces.  Their methods are all public methods for the public 
        // interface.  Let these public methods execute via setAccessible(true).
        if (JavaClass.CAN_SET_ACCESSIBLE) {
            // we should be able to setAccessible ok...
            try {
                if (methodIsPublic &&
                    !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                    accessibleObject().setAccessible(true);
                }
            } catch (SecurityException se) {
                // we shouldn't get here if JavaClass.CAN_SET_ACCESSIBLE is doing
                // what it should, so we warn.
               runtime.getWarnings().warn("failed to setAccessible: " + accessibleObject() + ", exception follows: " + se.getMessage());
            }
        }
        
        returnConverter = JavaUtil.getJavaConverter(method.getReturnType());
    }

    public static JavaMethod create(Ruby runtime, Method method) {
        return new JavaMethod(runtime, method);
    }

    public static JavaMethod create(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        try {
            Method method = javaClass.getMethod(methodName, argumentTypes);
            return create(runtime, method);
        } catch (NoSuchMethodException e) {
            throw runtime.newNameError("undefined method '" + methodName + "' for class '" + javaClass.getName() + "'",
                    methodName);
        }
    }

    public static JavaMethod createDeclared(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        try {
            return create(runtime, javaClass.getDeclaredMethod(methodName, argumentTypes));
        } catch (NoSuchMethodException e) {
            throw runtime.newNameError("undefined method '" + methodName + "' for class '" + javaClass.getName() + "'",
                    methodName);
        }
    }

    public static JavaMethod getMatchingDeclaredMethod(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods.  also, the getDeclared calls may throw SecurityException if
        // we're running under a restrictive security policy.
        try {
            return create(runtime, javaClass.getDeclaredMethod(methodName, argumentTypes));
        } catch (NoSuchMethodException e) {
            // search through all declared methods to find a closest match
            MethodSearch: for (Method method : javaClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    Class<?>[] targetTypes = method.getParameterTypes();
                
                    // for zero args case we can stop searching
                    if (targetTypes.length == 0 && argumentTypes.length == 0) {
                        return create(runtime, method);
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
                    return create(runtime, method);
                }
            }
        }
        // no matching method found
        return null;
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof JavaMethod &&
            this.method == ((JavaMethod)other).method;
    }
    
    @Override
    public int hashCode() {
        return method.hashCode();
    }

    @JRubyMethod
    @Override
    public RubyString name() {
        return getRuntime().newString(method.getName());
    }

    public int getArity() {
        return parameterTypes.length;
    }

    @JRubyMethod(name = "public?")
    @Override
    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(method.getModifiers()));
    }

    @JRubyMethod(name = "final?")
    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(method.getModifiers()));
    }

    @JRubyMethod(rest = true)
    public IRubyObject invoke(IRubyObject[] args) {
        checkArity(args.length - 1);
        Object[] arguments = new Object[args.length - 1];
        convertArguments(args, arguments, 1);

        IRubyObject invokee = args[0];
        if(invokee.isNil()) {
            return invokeWithExceptionHandling(method, null, arguments);
        }

        Object javaInvokee = null;

        if (!isStatic()) {
            javaInvokee = JavaUtil.unwrapJavaValue(getRuntime(), invokee, "invokee not a java object");

            if (! method.getDeclaringClass().isInstance(javaInvokee)) {
                throw getRuntime().newTypeError("invokee not instance of method's class (" +
                                                  "got" + javaInvokee.getClass().getName() + " wanted " +
                                                  method.getDeclaringClass().getName() + ")");
            }

            //
            // this test really means, that this is a ruby-defined subclass of a java class
            //
            if (javaInvokee instanceof InternalJavaProxy &&
                    // don't bother to check if final method, it won't
                    // be there (not generated, can't be!)
                    !Modifier.isFinal(method.getModifiers())) {
                JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee)
                        .___getProxyClass();
                JavaProxyMethod jpm;
                if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null &&
                        jpm.hasSuperImplementation()) {
                    return invokeWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, arguments);
                }
            }
        }
        return invokeWithExceptionHandling(method, javaInvokee, arguments);
    }

    @JRubyMethod(rest = true)
    public IRubyObject invoke_static(IRubyObject[] args) {
        checkArity(args.length);
        Object[] arguments = new Object[args.length];
        System.arraycopy(args, 0, arguments, 0, arguments.length);
        convertArguments(args, arguments, 0);
        return invokeWithExceptionHandling(method, null, arguments);
    }

    @JRubyMethod
    public IRubyObject return_type() {
        Class<?> klass = method.getReturnType();
        
        if (klass.equals(void.class)) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), klass);
    }

    @JRubyMethod
    public IRubyObject type_parameters() {
        return Java.getInstance(getRuntime(), method.getTypeParameters());
    }

    public IRubyObject invokeDirect(Object javaInvokee, Object[] args) {
        checkArity(args.length);
        checkInstanceof(javaInvokee);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee, args);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee, args);
    }

    public IRubyObject invokeDirect(Object javaInvokee) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(0);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee);
    }

    public IRubyObject invokeDirect(Object javaInvokee, Object arg0) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(1);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee, arg0);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee, arg0);
    }

    public IRubyObject invokeDirect(Object javaInvokee, Object arg0, Object arg1) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(2);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee, arg0, arg1);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1);
    }

    public IRubyObject invokeDirect(Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(3);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee, arg0, arg1, arg2);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1, arg2);
    }

    public IRubyObject invokeDirect(Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        assert method.getDeclaringClass().isInstance(javaInvokee);

        checkArity(4);

        if (mightBeProxy(javaInvokee)) {
            return tryProxyInvocation(javaInvokee, arg0, arg1, arg2, arg3);
        }

        return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1, arg2, arg3);
    }

    public IRubyObject invokeStaticDirect(Object[] args) {
        checkArity(args.length);
        return invokeDirectWithExceptionHandling(method, null, args);
    }

    public IRubyObject invokeStaticDirect() {
        checkArity(0);
        return invokeDirectWithExceptionHandling(method, null);
    }

    public IRubyObject invokeStaticDirect(Object arg0) {
        checkArity(1);
        return invokeDirectWithExceptionHandling(method, null, arg0);
    }

    public IRubyObject invokeStaticDirect(Object arg0, Object arg1) {
        checkArity(2);
        return invokeDirectWithExceptionHandling(method, null, arg0, arg1);
    }

    public IRubyObject invokeStaticDirect(Object arg0, Object arg1, Object arg2) {
        checkArity(3);
        return invokeDirectWithExceptionHandling(method, null, arg0, arg1, arg2);
    }

    public IRubyObject invokeStaticDirect(Object arg0, Object arg1, Object arg2, Object arg3) {
        checkArity(4);
        return invokeDirectWithExceptionHandling(method, null, arg0, arg1, arg2, arg3);
    }

    private void checkInstanceof(Object javaInvokee) throws RaiseException {
        if (!method.getDeclaringClass().isInstance(javaInvokee)) {
            throw getRuntime().newTypeError("invokee not instance of method's class (" + "got" + javaInvokee.getClass().getName() + " wanted " + method.getDeclaringClass().getName() + ")");
        }
    }

    private IRubyObject invokeWithExceptionHandling(Method method, Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return returnConverter.convert(getRuntime(), result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectSuperWithExceptionHandling(Method method, Object javaInvokee, Object... arguments) {
        // super calls from proxies must use reflected method
        // FIXME: possible to make handles do the superclass call?
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee, Object[] arguments) {
        try {
            Object result = method.invoke(javaInvokee, arguments);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee) {
        try {
            Object result = method.invoke(javaInvokee);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee, Object arg0) {
        try {
            Object result = method.invoke(javaInvokee, arg0);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arg0);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee, Object arg0, Object arg1) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arg0, arg1);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1, arg2);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arg0, arg1, arg2);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject invokeDirectWithExceptionHandling(Method method, Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        try {
            Object result = method.invoke(javaInvokee, arg0, arg1, arg2, arg3);
            return convertReturn(result);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(method, iae, arg0, arg1, arg2, arg3);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(method, iae);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(ite, method);
        } catch (Throwable t) {
            return handleThrowable(t, method);
        }
    }

    private IRubyObject convertReturn(Object result) {
        if (result != null && result.getClass() != boxedReturnType) {
            // actual type does not exactly match method return type, re-get converter
            // FIXME: when the only autoconversions are primitives, this won't be needed
            return JavaUtil.convertJavaToUsableRubyObject(getRuntime(), result);
        }
        return JavaUtil.convertJavaToUsableRubyObjectWithConverter(getRuntime(), result, returnConverter);
    }

    private IRubyObject handleIllegalAccessEx(Method method, IllegalAccessException iae) throws RaiseException {
        throw getRuntime().newTypeError("illegal access on '" + method.getName() + "': " + iae.getMessage());
    }

    private IRubyObject handlelIllegalArgumentEx(Method method, IllegalArgumentException iae, Object... arguments) throws RaiseException {
        throw getRuntime().newTypeError(
                "for method " +
                method.getDeclaringClass().getSimpleName() +
                "." +
                method.getName() +
                " expected " +
                argument_types().inspect() +
                "; got: " +
                dumpArgTypes(arguments) +
                "; error: " +
                iae.getMessage());
    }

    private void convertArguments(IRubyObject[] argsIn, Object[] argsOut, int from) {
        Class<?>[] types = parameterTypes;
        for (int i = argsOut.length; --i >= 0; ) {
            argsOut[i] = argsIn[i+from].toJava(types[i]);
        }
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    public Type[] getGenericParameterTypes() {
        return method.getGenericParameterTypes();
    }

    public Type[] getGenericExceptionTypes() {
        return method.getGenericExceptionTypes();
    }
    
    public Annotation[][] getParameterAnnotations() {
        return method.getParameterAnnotations();
    }

    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    protected String nameOnInspection() {
        return "#<" + getType().toString() + "/" + method.getName() + "(";
    }

    @JRubyMethod(name = "static?")
    public RubyBoolean static_p() {
        return getRuntime().newBoolean(isStatic());
    }
    
    public RubyBoolean bridge_p() {
        return getRuntime().newBoolean(method.isBridge());
    }

    private boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    public int getModifiers() {
        return method.getModifiers();
    }

    public String toGenericString() {
        return method.toGenericString();
    }

    public AccessibleObject accessibleObject() {
        return method;
    }

    private boolean mightBeProxy(Object javaInvokee) {
        // this test really means, that this is a ruby-defined subclass of a java class
        return javaInvokee instanceof InternalJavaProxy && !isFinal;
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee, Object... args) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, args);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee, args);
        }
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee);
        }
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee, Object arg0) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, arg0);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee, arg0);
        }
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee, Object arg0, Object arg1) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, arg0, arg1);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1);
        }
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee, Object arg0, Object arg1, Object arg2) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, arg0, arg1, arg2);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1, arg2);
        }
    }

    private IRubyObject tryProxyInvocation(Object javaInvokee, Object arg0, Object arg1, Object arg2, Object arg3) {
        JavaProxyClass jpc = ((InternalJavaProxy) javaInvokee).___getProxyClass();
        JavaProxyMethod jpm;
        if ((jpm = jpc.getMethod(method.getName(), parameterTypes)) != null && jpm.hasSuperImplementation()) {
            return invokeDirectSuperWithExceptionHandling(jpm.getSuperMethod(), javaInvokee, arg0, arg1, arg2, arg3);
        } else {
            return invokeDirectWithExceptionHandling(method, javaInvokee, arg0, arg1, arg2, arg3);
        }
    }

    public static RaiseException newMethodNotFoundError(Ruby runtime, Class target, String prettyName, String simpleName) {
        return runtime.newNameError("java method not found: " + target.getName() + "." + prettyName, simpleName);
    }

    public static RaiseException newArgSizeMismatchError(Ruby runtime, Class ... argTypes) {
        return runtime.newArgumentError("argument count mismatch for method signature " + CodegenUtils.prettyParams(argTypes));
    }
}
