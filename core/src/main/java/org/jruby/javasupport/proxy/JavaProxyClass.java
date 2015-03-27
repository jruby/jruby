/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.javasupport.JavaClass.EMPTY_CLASS_ARRAY;
import static org.jruby.javasupport.JavaCallable.inspectParameterTypes;

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
public class JavaProxyClass extends JavaProxyReflectionObject {
    static ThreadLocal<Ruby> runtimeTLS = new ThreadLocal<Ruby>();

    private final Class proxyClass;
    private final ArrayList<JavaProxyMethod> methods = new ArrayList<JavaProxyMethod>();
    private final HashMap<String, ArrayList<JavaProxyMethod>> methodMap = new HashMap<String, ArrayList<JavaProxyMethod>>();

    /* package scope */
    JavaProxyClass(final Class<?> proxyClass) {
        super(getThreadLocalRuntime(), getThreadLocalRuntime().getModule("Java").getClass("JavaProxyClass"));
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

    private static Ruby getThreadLocalRuntime() {
        return runtimeTLS.get();
    }

    public static JavaProxyClass getProxyClass(Ruby runtime, Class superClass,
            Class[] interfaces, Set<String> names) throws InvocationTargetException {
        Ruby save = runtimeTLS.get();
        runtimeTLS.set(runtime);
        try {
            ClassLoader loader = runtime.getJRubyClassLoader();

            return runtime.getJavaProxyClassFactory().newProxyClass(runtime, loader, null, superClass, interfaces, names);
        } finally {
            runtimeTLS.set(save);
        }
    }

    public static JavaProxyClass getProxyClass(Ruby runtime, Class superClass,
            Class[] interfaces) throws InvocationTargetException {
        return getProxyClass(runtime, superClass, interfaces, null);
    }

    public static Object newProxyInstance(Ruby runtime, Class superClass, Class[] interfaces,
            Class[] constructorParameters, Object[] constructorArgs,
            JavaProxyInvocationHandler handler) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            SecurityException, NoSuchMethodException {

        JavaProxyClass proxyClass = getProxyClass(runtime, superClass, interfaces);
        JavaProxyConstructor constructor = proxyClass.getConstructor(
            constructorParameters == null ? EMPTY_CLASS_ARRAY : constructorParameters
        );
        return constructor.newInstance(constructorArgs, handler);

    }

    public Class getSuperclass() {
        return proxyClass.getSuperclass();
    }

    public Class[] getInterfaces() {
        Class[] ifaces = proxyClass.getInterfaces();
        Class[] result = new Class[ifaces.length - 1];
        for ( int i = 0, j = 0; i < ifaces.length; i++ ) {
            if ( ifaces[i] == InternalJavaProxy.class ) continue;
            result[ j++ ] = ifaces[i];
        }
        return result;
    }

    private transient JavaProxyConstructor[] constructors;

    public JavaProxyConstructor[] getConstructors() {
        JavaProxyConstructor[] constructors = this.constructors;
        if ( constructors != null ) return constructors;

        final Ruby runtime = getRuntime();
        final Constructor[] ctors = proxyClass.getConstructors();
        constructors = new JavaProxyConstructor[ ctors.length ];
        for ( int i = 0; i < ctors.length; i++ ) {
            constructors[i] = new JavaProxyConstructor(runtime, this, ctors[i]);
        }
        return this.constructors = constructors;
    }

    public JavaProxyConstructor getConstructor(final Class[] args)
        throws SecurityException, NoSuchMethodException {

        final Class[] realArgs = new Class[args.length + 1];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        realArgs[ args.length ] = JavaProxyInvocationHandler.class;

        @SuppressWarnings("unchecked")
        Constructor<?> constructor = proxyClass.getConstructor(realArgs);
        return new JavaProxyConstructor(getRuntime(), this, constructor);
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

    /** return the class of instances of this proxy class */
    final Class getProxyClass() { return proxyClass; }

    @Override
    public Class getJavaClass() {
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

        public static RubyClass createJavaProxyMethodClass(Ruby runtime, RubyModule Java) {
            RubyClass JavaProxyMethod = Java.defineClassUnder("JavaProxyMethod",
                runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

            JavaProxyReflectionObject.registerRubyMethods(runtime, JavaProxyMethod);
            JavaProxyMethod.defineAnnotatedMethods(ProxyMethodImpl.class);
            return JavaProxyMethod;
        }

        public ProxyMethodImpl(Ruby runtime, final JavaProxyClass clazz,
            final Method method, final Method superMethod) {
            super(runtime, getJavaProxyMethod(runtime));
            this.method = method;
            this.parameterTypes = method.getParameterTypes();
            this.superMethod = superMethod;
            this.proxyClass = clazz;
        }

        private static RubyClass getJavaProxyMethod(final Ruby runtime) {
            return runtime.getJavaSupport().getJavaModule().getClass("JavaProxyMethod");
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

        public Object defaultResult() {
            final Class returnType = method.getReturnType();

            if (returnType == Void.TYPE) return null;
            if (returnType == Boolean.TYPE) return Boolean.FALSE;
            if (returnType == Byte.TYPE) return Byte.valueOf((byte) 0);
            if (returnType == Short.TYPE) return Short.valueOf((short) 0);
            if (returnType == Integer.TYPE) return Integer.valueOf(0);
            if (returnType == Long.TYPE) return Long.valueOf(0L);
            if (returnType == Float.TYPE) return new Float(0.0f);
            if (returnType == Double.TYPE) return new Double(0.0);

            return null;
        }

        public final boolean matches(final String name, final Class<?>[] parameterTypes) {
            return method.getName().equals(name) && Arrays.equals(this.parameterTypes, parameterTypes);
        }

        public final Class<?> getReturnType() {
            return method.getReturnType();
        }

        public RubyObject name() {
            return getRuntime().newString(getName());
        }

        @JRubyMethod(name = "declaring_class")
        public final JavaProxyClass getDeclaringClass() {
            return proxyClass;
        }

        @JRubyMethod
        public RubyArray argument_types() {
            return toRubyArray(getParameterTypes());
        }

        @JRubyMethod(name = "super?")
        public IRubyObject super_p() {
            return hasSuperImplementation() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod
        public RubyFixnum arity() {
            return getRuntime().newFixnum(getArity());
        }

        @Deprecated
        protected String nameOnInspection() {
            return getDeclaringClass().nameOnInspection() + "/" + getName();
        }

        @Override
        @JRubyMethod
        public RubyString inspect() {
            StringBuilder str = new StringBuilder();
            str.append("#<");
            str.append( getDeclaringClass().nameOnInspection() ).append('/').append( getName() );
            inspectParameterTypes(str, this);
            str.append(">");
            return getRuntime().newString( str.toString() );
        }

        @JRubyMethod(name = "invoke", rest = true)
        public IRubyObject do_invoke(final IRubyObject[] args) {
            final Ruby runtime=  getRuntime();
            if ( args.length != 1 + getArity() ) {
                throw runtime.newArgumentError(args.length, 1 + getArity());
            }

            final IRubyObject invokee = args[0];
            if ( ! ( invokee instanceof JavaObject ) ) {
                throw runtime.newTypeError("invokee not a java object");
            }

            Object receiver_value = ((JavaObject) invokee).getValue();

            final Object[] arguments = new Object[ args.length - 1 ];

            final Class[] parameterTypes = getParameterTypes();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = args[i + 1].toJava( parameterTypes[i] );
            }

            try {
                Object javaResult = superMethod.invoke(receiver_value, arguments);
                return JavaUtil.convertJavaToRuby(runtime, javaResult, getReturnType());
            }
            catch (IllegalArgumentException ex) {
                throw runtime.newTypeError("expected " + argument_types().inspect());
            }
            catch (IllegalAccessException ex) {
                throw runtime.newTypeError("illegal access on '" + superMethod.getName() + "': " +
                        ex.getMessage());
            }
            catch (InvocationTargetException ex) {
                if ( runtime.getDebug().isTrue() ) ex.getTargetException().printStackTrace();

                runtime.getJavaSupport().handleNativeException(ex.getTargetException(), superMethod);
                return runtime.getNil(); // only reached if there was an exception handler installed
            }
        }

        public final int getArity() {
            return getParameterTypes().length;
        }

    }

    @SuppressWarnings("unchecked")
    JavaProxyMethod initMethod(final String name, final String desc, final boolean hasSuper) {
        final Class proxy = this.proxyClass;
        try {
            Class[] paramTypes = parse(proxy.getClassLoader(), desc);
            Method method = proxy.getDeclaredMethod(name, paramTypes);
            Method superMethod = null;
            if ( hasSuper ) {
                superMethod = proxy.getDeclaredMethod("__super$" + name, paramTypes);
            }

            JavaProxyMethod proxyMethod = new ProxyMethodImpl(getRuntime(), this, method, superMethod);
            methods.add(proxyMethod);

            ArrayList<JavaProxyMethod> methodsWithName = this.methodMap.get(name);
            if (methodsWithName == null) {
                methodsWithName = new ArrayList<JavaProxyMethod>(2);
                methodMap.put(name, methodsWithName);
            }
            methodsWithName.add(proxyMethod);

            return proxyMethod;
        }
        catch (ClassNotFoundException e) {
            throw new InternalError(e.getMessage());
        }
        catch (SecurityException e) {
            throw new InternalError(e.getMessage());
        }
        catch (NoSuchMethodException e) {
            throw new InternalError(e.getMessage());
        }
    }

    private static Class[] parse(final ClassLoader loader, String desc) throws ClassNotFoundException {
        final List<Class> types = new ArrayList<Class>();
        int idx = 1;
        while (desc.charAt(idx) != ')') {

            int arr = 0;
            while (desc.charAt(idx) == '[') {
                idx += 1;
                arr += 1;
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
                throw new InternalError("cannot parse " + desc + "[" + idx + "]");
            }

            idx += 1;

            if (arr != 0) {
                type = Array.newInstance(type, new int[arr]).getClass();
            }

            types.add(type);
        }

        return types.toArray(new Class[types.size()]);
    }

    //
    // Ruby-level methods
    //

    @Deprecated
    public static void createJavaProxyModule(Ruby runtime) {
        createJavaProxyClasses(runtime, runtime.getJavaSupport().getJavaModule());
    }

    public static void createJavaProxyClasses(final Ruby runtime, final RubyModule Java) {
        JavaProxyClass.createJavaProxyClassClass(runtime, Java);
        ProxyMethodImpl.createJavaProxyMethodClass(runtime, Java);
        JavaProxyConstructor.createJavaProxyConstructorClass(runtime, Java);
    }

    public static RubyClass createJavaProxyClassClass(final Ruby runtime, final RubyModule Java) {
        RubyClass JavaProxyClass = Java.defineClassUnder("JavaProxyClass",
            runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR
        );
        JavaProxyReflectionObject.registerRubyMethods(runtime, JavaProxyClass);
        JavaProxyClass.defineAnnotatedMethods(JavaProxyClass.class);
        return JavaProxyClass;
    }

    @JRubyMethod(meta = true)
    public static RubyObject get(IRubyObject self, IRubyObject obj) {
        final Ruby runtime = self.getRuntime();
        if ( ! ( obj instanceof JavaClass ) ) {
            throw runtime.newTypeError(obj, runtime.getJavaSupport().getJavaClassClass());
        }

        final JavaClass type = (JavaClass) obj;
        try {
            return getProxyClass(runtime, type.javaClass(), EMPTY_CLASS_ARRAY);
        }
        catch (Error e) {
            RaiseException ex = runtime.newArgumentError("unable to create proxy class for " + type.getValue());
            ex.initCause(e);
            throw ex;
        }
        catch (InvocationTargetException e) {
            RaiseException ex = runtime.newArgumentError("unable to create proxy class for " + type.getValue());
            ex.initCause(e);
            throw ex;
        }
    }

    private static final HashSet<String> EXCLUDE_MODULES = new HashSet<String>();
    static {
        EXCLUDE_MODULES.add("Kernel");
        EXCLUDE_MODULES.add("Java");
        EXCLUDE_MODULES.add("JavaProxyMethods");
        EXCLUDE_MODULES.add("Enumerable");
    }

    private static final HashSet<String> EXCLUDE_METHODS = new HashSet<String>();
    static {
        EXCLUDE_METHODS.add("class");
        EXCLUDE_METHODS.add("finalize");
        EXCLUDE_METHODS.add("initialize");
        EXCLUDE_METHODS.add("java_class");
        EXCLUDE_METHODS.add("java_object");
        EXCLUDE_METHODS.add("__jcreate!");
        EXCLUDE_METHODS.add("__jsend!");
    }

    @JRubyMethod(meta = true)
    public static RubyObject get_with_class(IRubyObject recv, IRubyObject obj) {
        final Ruby runtime = recv.getRuntime();

        if (!(obj instanceof RubyClass)) {
            throw runtime.newTypeError(obj, runtime.getClassClass());
        }

        RubyClass clazz = (RubyClass)obj;

        // Let's only generate methods for those the user may actually
        // intend to override.  That includes any defined in the current
        // class, and any ancestors that are also JavaProxyClasses (but none
        // from any other ancestor classes). Methods defined in mixins will
        // be considered intentionally overridden, except those from Kernel,
        // Java, and JavaProxyMethods, as well as Enumerable.
        // TODO: may want to exclude other common mixins?

        JavaClass javaClass = null;
        Set<String> names = new HashSet<String>(); // need names ordered for key generation later
        List<Class<?>> interfaceList = new ArrayList<Class<?>>();

        List<IRubyObject> ancestors = clazz.getAncestorList();
        boolean skipRemainingClasses = false;
        for (IRubyObject ancestorObject: ancestors) {
            RubyModule ancestor = (RubyModule) ancestorObject;
            if (ancestor instanceof RubyClass) {
                if (skipRemainingClasses) continue;
                // we only collect methods and interfaces for
                // user-defined proxy classes.
                if (!ancestor.getInstanceVariables().hasInstanceVariable("@java_proxy_class")) {
                    skipRemainingClasses = true;
                    continue;
                }

                // get JavaClass if this is the new proxy class; verify it
                // matches if this is a superclass proxy.
                IRubyObject var = ancestor.getInstanceVariables().getInstanceVariable("@java_class");
                if (var == null) {
                    throw runtime.newTypeError(
                            "no java_class defined for proxy (or ancestor): " + ancestor);
                } else if (!(var instanceof JavaClass)) {
                    throw runtime.newTypeError(
                            "invalid java_class defined for proxy (or ancestor): " +
                            ancestor + ": " + var);
                }
                if (javaClass == null) {
                    javaClass = (JavaClass)var;
                } else if (javaClass != var) {
                    throw runtime.newTypeError(
                            "java_class defined for " + clazz + " (" + javaClass +
                            ") does not match java_class for ancestor " + ancestor +
                            " (" + var + ")");
                }
                // get any included interfaces
                var = ancestor.getInstanceVariables().getInstanceVariable("@java_interfaces");
                if (var != null && !(var instanceof RubyNil)) {
                    if (!(var instanceof RubyArray)) {
                        throw runtime.newTypeError(
                                "invalid java_interfaces defined for proxy (or ancestor): " +
                                ancestor + ": " + var);
                    }
                    RubyArray ifcArray = (RubyArray)var;
                    int size = ifcArray.size();
                    for (int i = size; --i >= 0; ) {
                        IRubyObject ifc = ifcArray.eltInternal(i);
                        if (!(ifc instanceof JavaClass)) {
                            throw runtime.newTypeError(
                                "invalid java interface defined for proxy (or ancestor): " +
                                ancestor + ": " + ifc);
                        }
                        Class interfaceClass = ((JavaClass)ifc).javaClass();
                        if (!interfaceClass.isInterface()) {
                            throw runtime.newTypeError(
                                    "invalid java interface defined for proxy (or ancestor): " +
                                    ancestor + ": " + ifc + " (not an interface)");
                        }
                        if (!interfaceList.contains(interfaceClass)) {
                            interfaceList.add(interfaceClass);
                        }
                    }
                }
                // set this class's method names in var @__java_ovrd_methods if this
                // is the new class; otherwise, get method names from there if this is
                // a proxy superclass.

                // FIXME: shouldn't need @__java_ovrd_methods, just query locally defined methods.

                var = ancestor.getInstanceVariables().getInstanceVariable("@__java_ovrd_methods");
                if (var == null) {
                    // lock in the overridden methods for the new class, and any as-yet
                    // uninstantiated ancestor class.
                    Map<String, DynamicMethod> methods;
                    RubyArray methodNames;
                    synchronized(methods = ancestor.getMethods()) {
                        methodNames = RubyArray.newArrayLight(runtime,methods.size());
                        for (String methodName: methods.keySet()) {
                            if (!EXCLUDE_METHODS.contains(methodName)) {
                                names.add(methodName);
                                methodNames.append(runtime.newString(methodName));
                            }
                        }
                    }
                    ancestor.setInstanceVariable("@__java_ovrd_methods",methodNames);
                } else {
                    if (!(var instanceof RubyArray)) {
                        throw runtime.newTypeError(
                                "invalid @__java_ovrd_methods defined for proxy: " +
                                ancestor + ": " + var);
                    }
                    RubyArray methodNames = (RubyArray)var;
                    int size = methodNames.size();
                    for (int i = size; --i >= 0; ) {
                        IRubyObject methodName = methodNames.eltInternal(i);
                        if (!(methodName instanceof RubyString)) {
                            throw runtime.newTypeError(
                                    "invalid method name defined for proxy (or ancestor): " +
                                    ancestor + ": " + methodName);
                        }
                        names.add(methodName.asJavaString());
                    }
                }
            } else if (!EXCLUDE_MODULES.contains(ancestor.getName())) {
                Map<String, DynamicMethod> methods;
                synchronized(methods = ancestor.getMethods()) {
                    for (String methodName: methods.keySet()) {
                        if (!EXCLUDE_METHODS.contains(methodName)) {
                            names.add(methodName);
                        }
                    }
                }
            }
        }

        if (javaClass == null) {
            throw runtime.newArgumentError("unable to create proxy class: no java_class defined for " + clazz);
        }

        int interfaceCount = interfaceList.size();
        Class<?>[] interfaces = new Class<?>[interfaceCount];
        for (int i = interfaceCount; --i >= 0; ) {
            interfaces[i] = interfaceList.get(i);
        }

        try {
            return getProxyClass(runtime, javaClass.javaClass(), interfaces, names);
        }
        catch (Error e) {
            RaiseException ex = runtime.newArgumentError("unable to create proxy class for " + javaClass.getValue() + " : " + e.getMessage());
            //e.printStackTrace();
            ex.initCause(e);
            throw ex;
        }
        catch (InvocationTargetException e) {
            RaiseException ex = runtime.newArgumentError("unable to create proxy class for " + javaClass.getValue() + " : " + e.getMessage());
            //e.printStackTrace();
            ex.initCause(e);
            throw ex;
        }
    }

    @JRubyMethod
    public RubyObject superclass() {
        return JavaClass.get(getRuntime(), getSuperclass());
    }

    @JRubyMethod
    public RubyArray methods() {
        return toRubyArray( getMethods() );
    }

    @JRubyMethod
    public RubyArray interfaces() {
        return toRubyArray(getInterfaces());
    }

    @JRubyMethod
    public final RubyArray constructors() {
        return toRubyArray( getConstructors() );
    }

    public final String nameOnInspection() {
        return "[Proxy:" + getSuperclass().getName() + "]";
    }
}
