/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

package org.jruby.javasupport.proxy;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.proxies.ConcreteJavaProxy.NewMethodReified;
import org.jruby.java.proxies.ConcreteJavaProxy.StaticJCreateMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.util.ClassUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JavaNameMangler;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.castAsClass;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.javasupport.JavaCallable.inspectParameterTypes;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 * Generalized proxy for classes and interfaces.
 *
 * API looks a lot like java.lang.reflect.Proxy, except that you can specify a
 * super class in addition to a set of interfaces.
 *
 * The main implication for users of this class is to handle the case where a
 * proxy method overrides an existing method, because in this case the
 * invocation handler should "default" to calling the super implementation
 * {JavaProxyMethod.invokeSuper}.
 *
 *
 * @author krab@trifork.com
 * @see java.lang.reflect.Proxy
 *
 */
@JRubyClass(name="Java::JavaProxyClass")
public class JavaProxyClass extends JavaProxyReflectionObject {

    private final Class proxyClass;
    private final ArrayList<JavaProxyMethod> methods = new ArrayList<>();
    private final HashMap<String, ArrayList<JavaProxyMethod>> methodMap = new HashMap<>();

    private JavaProxyClass(ThreadContext context, final Class<?> proxyClass) {
        super(context.runtime, Access.getClass(context, "Java", "JavaProxyClass"));
        this.proxyClass = proxyClass;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaProxyClass &&
            this.proxyClass == ((JavaProxyClass) other).proxyClass;
    }

    @Override
    public int hashCode() {
        return proxyClass.hashCode();
    }

    public Object getValue() {
        return this;
    }

    public Class getSuperclass() {
        return proxyClass.getSuperclass();
    }

    public Class[] getInterfaces() {
        Class[] ifaces = proxyClass.getInterfaces();
        Class[] result = new Class[ifaces.length - 1];
        for ( int i = 0, j = 0; i < ifaces.length; i++ ) {
            if ( ifaces[i] == ReifiedJavaProxy.class ) continue;
            result[ j++ ] = ifaces[i];
        }
        return result;
    }

    private transient JavaProxyConstructor[] constructors;

    @Deprecated(since = "10.0.0.0")
    public JavaProxyConstructor[] getConstructors() {
        return getConstructors(getCurrentContext());
    }

    public JavaProxyConstructor[] getConstructors(ThreadContext context) {
        JavaProxyConstructor[] constructorsCached = this.constructors;
        if ( constructorsCached != null ) return constructorsCached;

        final Constructor[] ctors = proxyClass.getConstructors();
        List<JavaProxyConstructor> constructors = new ArrayList<>(ctors.length);
        for (int i = 0; i < ctors.length; i++) {
            JavaProxyConstructor jpc = new JavaProxyConstructor(context.runtime, this, ctors[i]);
            if (!jpc.isExportable()) constructors.add(jpc);
        }
        return this.constructors = constructors.toArray(new JavaProxyConstructor[constructors.size()]);
    }

    @Deprecated(since = "10.0.0.0")
    public JavaProxyConstructor getConstructor(final Class[] args)
        throws SecurityException, NoSuchMethodException {

        final Class[] realArgs = new Class[args.length + 2];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        realArgs[ args.length ] = Ruby.class;
        realArgs[ args.length + 1 ] = RubyClass.class;

        @SuppressWarnings("unchecked")
        Constructor<?> constructor = proxyClass.getConstructor(realArgs);
        return new JavaProxyConstructor(getCurrentContext().runtime, this, constructor);
    }

    public JavaProxyMethod[] getMethods() {
        return methods.toArray(new JavaProxyMethod[methods.size()]);
    }

    public JavaProxyMethod getMethod(String name, Class[] parameterTypes) {
        final List<JavaProxyMethod> methods = methodMap.get(name);
        if ( methods != null && methods.size() > 0 ) {
            for ( int i = methods.size(); --i >= 0; ) {
                ProxyMethodImpl impl = (ProxyMethodImpl) methods.get(i);
                if ( impl.matches(name, parameterTypes) ) return impl;
            }
        }
        return null;
    }

    @Override
    public final Class getJavaClass() {
        return proxyClass;
    }

    @JRubyClass(name="Java::JavaProxyMethod")
    public static class ProxyMethodImpl extends JavaProxyReflectionObject
        implements JavaProxyMethod {

        private final Method method;
        private final Method superMethod;
        private final Class[] parameterTypes;

        private final JavaProxyClass proxyClass;

        private Object state;

        public static RubyClass createJavaProxyMethodClass(ThreadContext context, RubyClass Object, RubyModule Java) {
            var JavaProxyMethod = (RubyClass) Java.defineClassUnder(context, "JavaProxyMethod", Object, NOT_ALLOCATABLE_ALLOCATOR).
                    defineMethods(context, ProxyMethodImpl.class);
            JavaProxyReflectionObject.registerRubyMethods(context, JavaProxyMethod);

            return JavaProxyMethod;
        }

        public ProxyMethodImpl(Ruby runtime, final JavaProxyClass clazz,
            final Method method, final Method superMethod) {
            super(runtime, getJavaProxyMethod(runtime.getCurrentContext()));
            this.method = method;
            this.parameterTypes = method.getParameterTypes();
            this.superMethod = superMethod;
            this.proxyClass = clazz;
        }

        private static RubyClass getJavaProxyMethod(ThreadContext context) {
            return context.runtime.getJavaSupport().getJavaModule(context).getClass(context, "JavaProxyMethod");
        }

        @Override
        public boolean equals(Object other) {
            if ( ! ( other instanceof ProxyMethodImpl ) ) return false;
            final ProxyMethodImpl that = (ProxyMethodImpl) other;
            return this.method == that.method || this.method.equals( that.method );
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        public Method getMethod() {
            return method;
        }

        public Method getSuperMethod() {
            return superMethod;
        }

        public int getModifiers() {
            return method.getModifiers();
        }

        public String getName() {
            return method.getName();
        }

        public final Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        public final Class<?>[] getParameterTypes() {
            return parameterTypes;
        }

        public final boolean isVarArgs() {
            return method.isVarArgs();
        }

        public boolean hasSuperImplementation() {
            return superMethod != null;
        }

        public Object invoke(Object proxy, Object[] args) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {

            if ( ! hasSuperImplementation() ) throw new NoSuchMethodException();

            return superMethod.invoke(proxy, args);
        }

        public Object getState() {
            return state;
        }

        public void setState(Object state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return method.toString();
        }

        @Deprecated(since = "9.1.6.0")
        public Object defaultResult() {
            final Class returnType = method.getReturnType();

            if (returnType == Void.TYPE) return null;
            if (returnType == Boolean.TYPE) return Boolean.FALSE;
            if (returnType == Byte.TYPE) return Byte.valueOf((byte) 0);
            if (returnType == Short.TYPE) return Short.valueOf((short) 0);
            if (returnType == Integer.TYPE) return Integer.valueOf(0);
            if (returnType == Long.TYPE) return Long.valueOf(0L);
            if (returnType == Float.TYPE) return Float.valueOf(0.0f);
            if (returnType == Double.TYPE) return Double.valueOf(0.0);

            return null;
        }

        public final boolean matches(final String name, final Class<?>[] parameterTypes) {
            return method.getName().equals(name) && Arrays.equals(this.parameterTypes, parameterTypes);
        }

        public final Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Deprecated(since = "10.0.0.0")
        public RubyObject name() {
            return newString(getCurrentContext(), getName());
        }

        @JRubyMethod(name = "declaring_class")
        public final JavaProxyClass getDeclaringClass() {
            return proxyClass;
        }

        @Deprecated(since = "10.0.0.0")
        public RubyArray argument_types() {
            return argument_types(getCurrentContext());
        }

        @JRubyMethod
        public RubyArray argument_types(ThreadContext context) {
            return toClassArray(context, getParameterTypes());
        }

        @Deprecated(since = "10.0.0.0")
        public IRubyObject super_p() {
            return super_p(getCurrentContext());
        }

        @JRubyMethod(name = "super?")
        public IRubyObject super_p(ThreadContext context) {
            return hasSuperImplementation() ? context.tru : context.fals;
        }

        @Deprecated(since = "10.0.0.0")
        public RubyFixnum arity() {
            return arity(getCurrentContext());
        }

        @JRubyMethod
        public RubyFixnum arity(ThreadContext context) {
            return asFixnum(context, getArity());
        }

        @Deprecated(since = "10.0.0.0")
        public RubyString inspect() {
            return inspect(getCurrentContext());
        }

        @Override
        @JRubyMethod
        public RubyString inspect(ThreadContext context) {
            StringBuilder buf = new StringBuilder();
            buf.append("#<");
            buf.append( getDeclaringClass().nameOnInspection() ).append('/').append( getName() );
            inspectParameterTypes(buf, this);
            buf.append('>');
            return newString(context, buf.toString());
        }

        /**
         * @param args
         * @return
         * @deprecated Use {@link ProxyMethodImpl#do_invoke(ThreadContext, IRubyObject[])} instead.
         */
        @Deprecated(since = "10.0.0.0")
        public IRubyObject do_invoke(final IRubyObject[] args) {
            return do_invoke(getCurrentContext(), args);
        }


        @JRubyMethod(name = "invoke", rest = true)
        public IRubyObject do_invoke(ThreadContext context, final IRubyObject[] args) {
            if (args.length != 1 + getArity()) throw argumentError(context, args.length, 1 + getArity());

            if (!(args[0] instanceof JavaProxy invokee)) {
                throw typeError(context, "not a java proxy: " + (args[0] == null ? null : args[0].getClass()));
            }

            Object receiver_value = invokee.getObject();
            final Object[] arguments = new Object[ args.length - 1 ];

            final Class[] parameterTypes = getParameterTypes();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = args[i + 1].toJava( parameterTypes[i] );
            }

            try {
                Object javaResult = superMethod.invoke(receiver_value, arguments);
                return JavaUtil.convertJavaToRuby(context.runtime, javaResult, getReturnType());
            } catch (IllegalArgumentException ex) {
                throw typeError(context, "expected " + argument_types(context).inspect(context));
            } catch (IllegalAccessException ex) {
                throw typeError(context, "illegal access on '" + superMethod.getName() + "': " + ex.getMessage());
            } catch (InvocationTargetException ex) {
                if (context.runtime.getDebug().isTrue()) ex.getTargetException().printStackTrace();

                context.runtime.getJavaSupport().handleNativeException(ex.getTargetException(), superMethod);
                return context.nil; // only reached if there was an exception handler installed
            }
        }

        public final int getArity() {
            return getParameterTypes().length;
        }

    }

    //called from reified java concrete-extended classes with super-overrides in RubyClass#extraClinitLookup
    @SuppressWarnings("unchecked")
    public void initMethod(ThreadContext context, final String name, final String desc, final boolean hasSuper) {
        final Class proxy = this.proxyClass;
        try {
            Class[] paramTypes = parse(proxy.getClassLoader(), desc);
            Method method = proxy.getDeclaredMethod(name, paramTypes);
            Method superMethod = null;
            if ( hasSuper ) {
                superMethod = proxy.getDeclaredMethod(generateSuperName(proxy.getName(), name), paramTypes);
            }

            JavaProxyMethod proxyMethod = new ProxyMethodImpl(context.runtime, this, method, superMethod);
            methods.add(proxyMethod);

            ArrayList<JavaProxyMethod> methodsWithName = this.methodMap.get(name);
            if (methodsWithName == null) {
                methodsWithName = new ArrayList<>(2);
                methodMap.put(name, methodsWithName);
            }
            methodsWithName.add(proxyMethod);
        }
        catch (ClassNotFoundException e) {
            throw new InternalError(e.getMessage(), e);
        }
        catch (SecurityException e) {
            throw new InternalError(e.getMessage(), e);
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Generate a "super" stub for the given proxy class name and super method name.
     *
     * This name is intended to be unique to this class and method in order to allow jumping into the super chain at any
     * point in the hierarchy, bypassing the default behavior of virtual and reflective calls.
     *
     * @param className the proxy class name
     * @param superName the super method name
     * @return a unique stub method name for the given proxy class and super method
     */
    public static String generateSuperName(String className, String superName) {
        return "__super$" + JavaNameMangler.mangleMethodName(className) + "$" + superName;
    }

    private static Class[] parse(final ClassLoader loader, String desc) throws ClassNotFoundException {
        final ArrayList<Class> types = new ArrayList<>(8);
        int idx = 1;
        while (desc.charAt(idx) != ')') {

            int arr = 0;
            while (desc.charAt(idx) == '[') {
                idx++; arr += 1;
            }

            Class type;

            switch (desc.charAt(idx)) {
                case 'L':
                    int semi = desc.indexOf(';', idx);
                    final String name = desc.substring(idx + 1, semi);
                    idx = semi;
                    try {
                        type = AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {
                            public Class run() throws ClassNotFoundException {
                                return Class.forName(name.replace('/', '.'), false, loader);
                            }
                        });
                    } catch (PrivilegedActionException e) {
                        throw (ClassNotFoundException) e.getException();
                    }
                    break;

                case 'B': type = Byte.TYPE; break;
                case 'C': type = Character.TYPE; break;
                case 'Z': type = Boolean.TYPE; break;
                case 'S': type = Short.TYPE; break;
                case 'I': type = Integer.TYPE; break;
                case 'J': type = Long.TYPE; break;
                case 'F': type = Float.TYPE; break;
                case 'D': type = Double.TYPE; break;
                default:
                    throw new InternalError("cannot parse " + desc + '[' + idx + ']');
            }

            idx++;

            if (arr != 0) {
                type = Array.newInstance(type, new int[arr]).getClass();
            }

            types.add(type);
        }

        return types.isEmpty() ? ClassUtils.EMPTY_CLASS_ARRAY : types.toArray(new Class[types.size()]);
    }

    //
    // Ruby-level methods
    //

    public static void createJavaProxyClasses(ThreadContext context, final RubyModule Java, RubyClass Object) {
        JavaProxyClass.createJavaProxyClassClass(context, Object, Java);
        ProxyMethodImpl.createJavaProxyMethodClass(context, Object, Java);
        JavaProxyConstructor.createJavaProxyConstructorClass(context, Object, Java);
    }

    public static RubyClass createJavaProxyClassClass(ThreadContext context, RubyClass Object, final RubyModule Java) {
        RubyClass JavaProxyClass = Java.defineClassUnder(context, "JavaProxyClass", Object, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, JavaProxyClass.class);
        JavaProxyReflectionObject.registerRubyMethods(context, JavaProxyClass);

        return JavaProxyClass;
    }
//
//    @JRubyMethod(meta = true)
//    public static RubyObject get(IRubyObject self, IRubyObject obj) {
//        final Ruby runtime = self.getRuntime();
//
//        throw runtime.newNotImplementedError("Internal implementation has changed. JavaProxyClass's have been partially replaced with reification.");
//    }

    private static final HashSet<String> EXCLUDE_MODULES = new HashSet<>(8, 1);
    static {
        EXCLUDE_MODULES.add("Kernel");
        EXCLUDE_MODULES.add("Java");
        EXCLUDE_MODULES.add("JavaProxyMethods");
        EXCLUDE_MODULES.add("Enumerable");
    }

    @Deprecated(since = "10.0.0.0")
    public static RubyObject get_with_class(final IRubyObject self, IRubyObject obj) {
        return get_with_class(((RubyBasicObject) self).getCurrentContext(), self, obj);
    }

    @JRubyMethod(meta = true)
    public static RubyObject get_with_class(ThreadContext context, final IRubyObject self, IRubyObject obj) {
        return getProxyClass(context, castAsClass(context, obj));
    }
    
    // Note: called from <clinit> of reified classes
    public static JavaProxyClass setProxyClassReified(ThreadContext context, final RubyClass clazz,
            final Class<? extends ReifiedJavaProxy> reified, final boolean allocator) {
        JavaProxyClass proxyClass = new JavaProxyClass(context, reified);
        clazz.setInstanceVariable("@java_proxy_class", proxyClass);

        RubyClass singleton = clazz.singletonClass(context);

        singleton.setInstanceVariable("@java_proxy_class", proxyClass);
        singleton.setInstanceVariable("@java_class", Java.wrapJavaObject(context, reified));

        if (allocator) {
            DynamicMethod oldNewMethod = singleton.searchMethod("new");
            boolean defaultNew = !(oldNewMethod instanceof AbstractIRMethod); // TODO: is this the proper way to check if user-code has/not defined a method?
            if (defaultNew) {
                singleton.addMethod(context, "new", new NewMethodReified(clazz, reified));
            }
            // Install initialize
            StaticJCreateMethod.tryInstall(context.runtime, clazz, proxyClass, reified, defaultNew);
        }
        return proxyClass;
    }

    /**
     * These objects are to allow static initializers in reified code. See
     * RubyClass.BaseReifier for details
     */
    private static final ThreadLocal<Object[]> lookup = new ThreadLocal<>();

    public static int addStaticInitLookup(Object... objects) {
        // TODO: is this a log or an exception?
        if (objects != null) ensureStaticIntConsumed();
        lookup.set(objects);
        return System.identityHashCode(objects); // 0 if null
    }

    public static void ensureStaticIntConsumed() {
        if (lookup.get() != null) {
            throw new IllegalStateException("Thread local class wasn't consumed for: " + lookup.get()[1]);
        }
    }

    // used by reified code in RubyClass
    public static Object[] getStaticInitLookup(final int id) {
        final Object[] objects = lookup.get();
        if (objects == null) throw new IllegalStateException("Thread local class wasn't set up for reification");
        if (System.identityHashCode(objects) != id) {
            throw new IllegalStateException("Thread local class wasn't what reification was expecting: " +  lookup.get()[1]);
        }
        lookup.set(null);
        return objects;
    }

    @Deprecated(since = "10.0.0.0")
    public static JavaProxyClass getProxyClass(final Ruby runtime, final RubyClass clazz) {
        return getProxyClass(runtime.getCurrentContext(), clazz);
    }

    public static JavaProxyClass getProxyClass(ThreadContext context, final RubyClass clazz) {
    	clazz.reifyWithAncestors();
    	return (JavaProxyClass) clazz.getInstanceVariable("@java_proxy_class");
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject superclass() {
        return superclass(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject superclass(ThreadContext context) {
        return Java.getInstance(context.runtime, getSuperclass());
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray methods() {
        return methods(getCurrentContext());
    }

    @JRubyMethod
    public RubyArray methods(ThreadContext context) {
        return toRubyArray(context, getMethods());
    }

    @Deprecated(since = "10.0.0.0")
    public RubyArray interfaces() {
        return interfaces(getCurrentContext());
    }

    @JRubyMethod
    public RubyArray interfaces(ThreadContext context) {
        return toClassArray(context, getInterfaces());
    }

    @Deprecated(since = "10.0.0.0")
    public final RubyArray constructors() {
        return constructors(getCurrentContext());
    }

    @JRubyMethod
    public final RubyArray constructors(ThreadContext context) {
        return toRubyArray(context, getConstructors(context));
    }

    public final String nameOnInspection() {
        return "[Proxy:" + getSuperclass().getName() + ']';
    }
}
