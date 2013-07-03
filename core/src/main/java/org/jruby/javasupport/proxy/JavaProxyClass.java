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
    private final HashMap<String, List<JavaProxyMethod>> methodMap = new HashMap<String, List<JavaProxyMethod>>();
    private final RubyArray constructors;

    /* package scope */
    JavaProxyClass(Class proxyClass) {
        super(getThreadLocalRuntime(), 
                (RubyClass) getThreadLocalRuntime().getModule("Java").getClass("JavaProxyClass"));
        
        this.proxyClass = proxyClass;
        this.constructors = buildRubyArray(getConstructors());
    }

    public boolean equals(Object other) {
        return other instanceof JavaProxyClass &&
            this.proxyClass == ((JavaProxyClass)other).proxyClass;
    }
    
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
            Class[] interfaces, Set names) throws InvocationTargetException {
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
        return getProxyClass(runtime,superClass,interfaces,null);
    }
    
    public static Object newProxyInstance(Ruby runtime, Class superClass, Class[] interfaces, 
            Class[] constructorParameters, Object[] constructorArgs, 
            JavaProxyInvocationHandler handler) throws IllegalArgumentException, 
            InstantiationException, IllegalAccessException, InvocationTargetException,
            SecurityException, NoSuchMethodException {
        JavaProxyClass jpc = getProxyClass(runtime, superClass, interfaces);
        JavaProxyConstructor cons = jpc.getConstructor(constructorParameters == null ? 
                new Class[0] : constructorParameters);
        
        return cons.newInstance(constructorArgs, handler);

    }

    public Class getSuperclass() {
        return proxyClass.getSuperclass();
    }

    public Class[] getInterfaces() {
        Class[] ifaces = proxyClass.getInterfaces();
        Class[] result = new Class[ifaces.length - 1];
        int pos = 0;
        for (int i = 0; i < ifaces.length; i++) {
            if (ifaces[i] != InternalJavaProxy.class) {
                result[pos++] = ifaces[i];
            }
        }
        return result;
    }

    public JavaProxyConstructor[] getConstructors() {
        Constructor[] cons = proxyClass.getConstructors();
        JavaProxyConstructor[] result = new JavaProxyConstructor[cons.length];
        for (int i = 0; i < cons.length; i++) {
            result[i] = new JavaProxyConstructor(getRuntime(), this, cons[i]);
        }
        return result;
    }

    public JavaProxyConstructor getConstructor(Class[] args)
            throws SecurityException, NoSuchMethodException {

        Class[] realArgs = new Class[args.length + 1];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        realArgs[args.length] = JavaProxyInvocationHandler.class;

        Constructor constructor = proxyClass.getConstructor(realArgs);
        return new JavaProxyConstructor(getRuntime(), this, constructor);
    }

    public JavaProxyMethod[] getMethods() {
        return methods.toArray(new JavaProxyMethod[methods.size()]);
    }

    public JavaProxyMethod getMethod(String name, Class[] parameterTypes) {
        List<JavaProxyMethod> methods = methodMap.get(name);
        if (methods != null) {
            for (int i = methods.size(); --i >= 0; ) {
                ProxyMethodImpl jpm = (ProxyMethodImpl) methods.get(i);
                if (jpm.matches(name, parameterTypes)) return jpm;
            }
        }
        return null;
    }

    /** return the class of instances of this proxy class */
    Class getProxyClass() {
        return proxyClass;
    }
    
    @Override
    public Class getJavaClass() {
        return proxyClass;
    }

    @JRubyClass(name="JavaProxy::JavaProxyMethod")
    public static class ProxyMethodImpl extends JavaProxyReflectionObject
            implements JavaProxyMethod {
        private final Method m;

        private Object state;

        private final Method sm;
        private final Class[] parameterTypes;

        private final JavaProxyClass clazz;

        public ProxyMethodImpl(Ruby runtime, JavaProxyClass clazz, Method m,
                Method sm) {
            super(runtime, runtime.getJavaSupport().getJavaModule()
                    .getClass("JavaProxyMethod"));
            this.m = m;
            this.parameterTypes = m.getParameterTypes();
            this.sm = sm;
            this.clazz = clazz;
        }

        public boolean equals(Object other) {
            return other instanceof ProxyMethodImpl &&
                this.m == ((ProxyMethodImpl)other).m;
        }
        
        public int hashCode() {
            return m.hashCode();
        }

        public Method getMethod() {
            return m;
        }

        public Method getSuperMethod() {
            return sm;
        }

        public int getModifiers() {
            return m.getModifiers();
        }

        public String getName() {
            return m.getName();
        }

        public Class<?>[] getExceptionTypes() {
            return m.getExceptionTypes();
        }

        public Class<?>[] getParameterTypes() {
            return parameterTypes;
        }
        
        public boolean isVarArgs() {
            return m.isVarArgs();
        }

        public Object getState() {
            return state;
        }

        public boolean hasSuperImplementation() {
            return sm != null;
        }

        public Object invoke(Object proxy, Object[] args) throws IllegalArgumentException, 
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            
            if (!hasSuperImplementation()) throw new NoSuchMethodException();

            return sm.invoke(proxy, args);
        }

        public void setState(Object state) {
            this.state = state;
        }

        public String toString() {
            return m.toString();
        }

        public Object defaultResult() {
            Class rt = m.getReturnType();
            
            if (rt == Void.TYPE) return null;
            if (rt == Boolean.TYPE) return Boolean.FALSE;
            if (rt == Byte.TYPE) return Byte.valueOf((byte) 0);
            if (rt == Short.TYPE) return Short.valueOf((short) 0);
            if (rt == Integer.TYPE) return Integer.valueOf(0);
            if (rt == Long.TYPE) return Long.valueOf(0L);
            if (rt == Float.TYPE) return new Float(0.0f);
            if (rt == Double.TYPE) return new Double(0.0);

            return null;
        }

        public boolean matches(String name, Class[] parameterTypes) {
            return m.getName().equals(name) && Arrays.equals(this.parameterTypes, parameterTypes);
        }

        public Class getReturnType() {
            return m.getReturnType();
        }
        
        public static RubyClass createJavaProxyMethodClass(Ruby runtime, RubyModule javaProxyModule) {
            RubyClass result = javaProxyModule.defineClassUnder("JavaProxyMethod", 
                    runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

            JavaProxyReflectionObject.registerRubyMethods(runtime, result);

            result.defineAnnotatedMethods(ProxyMethodImpl.class);

            return result;
        }

        public RubyObject name() {
            return getRuntime().newString(getName());
        }

        @JRubyMethod(name = "declaring_class")
        public JavaProxyClass getDeclaringClass() {
            return clazz;
        }

        @JRubyMethod
        public RubyArray argument_types() {
            return buildRubyArray(getParameterTypes());
        }

        @JRubyMethod(name = "super?")
        public IRubyObject super_p() {
            return hasSuperImplementation() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod
        public RubyFixnum arity() {
            return getRuntime().newFixnum(getArity());
        }

        protected String nameOnInspection() {
            return getDeclaringClass().nameOnInspection() + "/" + getName();
        }

        @JRubyMethod
        public IRubyObject inspect() {
            StringBuilder result = new StringBuilder();
            result.append(nameOnInspection());
            result.append("(");
            Class[] parameterTypes = getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                result.append(parameterTypes[i].getName());
                if (i < parameterTypes.length - 1) {
                    result.append(',');
                }
            }
            result.append(")>");
            return getRuntime().newString(result.toString());
        }

        @JRubyMethod(name = "invoke", rest = true)
        public IRubyObject do_invoke(IRubyObject[] nargs) {
            if (nargs.length != 1 + getArity()) {
                throw getRuntime().newArgumentError(nargs.length, 1 + getArity());
            }

            IRubyObject invokee = nargs[0];
            if (!(invokee instanceof JavaObject)) {
                throw getRuntime().newTypeError("invokee not a java object");
            }
            Object receiver_value = ((JavaObject) invokee).getValue();
            Object[] arguments = new Object[nargs.length - 1];
            System.arraycopy(nargs, 1, arguments, 0, arguments.length);

            Class[] parameterTypes = getParameterTypes();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = 
                    ((IRubyObject) arguments[i]).toJava(parameterTypes[i]);
            }

            try {
                Object javaResult = sm.invoke(receiver_value, arguments);
                return JavaUtil.convertJavaToRuby(getRuntime(), javaResult, getReturnType());
            } catch (IllegalArgumentException e) {
                throw getRuntime().newTypeError("expected " + argument_types().inspect());
            } catch (IllegalAccessException iae) {
                throw getRuntime().newTypeError("illegal access on '" + sm.getName() + "': " + 
                        iae.getMessage());
            } catch (InvocationTargetException ite) {
                if (getRuntime().getDebug().isTrue()) ite.getTargetException().printStackTrace();
                getRuntime().getJavaSupport().handleNativeException(ite.getTargetException(), sm);
                // This point is only reached if there was an exception handler
                // installed.
                return getRuntime().getNil();
            }
        }

        private int getArity() {
            return getParameterTypes().length;
        }

    }

    JavaProxyMethod initMethod(String name, String desc, boolean hasSuper) {
        Class proxy = proxyClass;
        try {
            Class[] parms = parse(proxy.getClassLoader(), desc);
            Method m = proxy.getDeclaredMethod(name, parms);
            Method sm = null;
            if (hasSuper) {
                sm = proxy.getDeclaredMethod("__super$" + name, parms);
            }

            JavaProxyMethod jpm = new ProxyMethodImpl(getRuntime(), this, m, sm);
            methods.add(jpm);
            List<JavaProxyMethod> methodsWithName = methodMap.get(name);
            if (methodsWithName == null) {
                methodsWithName = new ArrayList<JavaProxyMethod>(2);
                methodMap.put(name,methodsWithName);
            }
            methodsWithName.add(jpm);
            
            return jpm;
        } catch (ClassNotFoundException e) {
            throw new InternalError(e.getMessage());
        } catch (SecurityException e) {
            throw new InternalError(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.getMessage());
        }
    }

    private static Class[] parse(final ClassLoader loader, String desc)
            throws ClassNotFoundException {
        List<Class> al = new ArrayList<Class>();
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

            al.add(type);
        }

        return (Class[]) al.toArray(new Class[al.size()]);
    }

    //
    // Ruby-level methods
    //
        
    public static RubyClass createJavaProxyClassClass(Ruby runtime, RubyModule javaModule) {
        RubyClass result = javaModule.defineClassUnder("JavaProxyClass",
                runtime.getObject(),ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        JavaProxyReflectionObject.registerRubyMethods(runtime, result);

        result.defineAnnotatedMethods(JavaProxyClass.class);

        return result;
    }

    @JRubyMethod(meta = true)
    public static RubyObject get(IRubyObject recv, IRubyObject obj) {
        if (!(obj instanceof JavaClass)) {
            throw recv.getRuntime().newTypeError(obj, recv.getRuntime().getJavaSupport().getJavaClassClass());
        }
        JavaClass type = (JavaClass)obj;
        
        try {
            return getProxyClass(recv.getRuntime(), (Class) type.getValue(), new Class[0]);
        } catch (Error e) {
            RaiseException ex = recv.getRuntime().newArgumentError("unable to create proxy class for " + type.getValue());
            ex.initCause(e);
            throw ex;
        } catch (InvocationTargetException e) {
            RaiseException ex = recv.getRuntime().newArgumentError("unable to create proxy class for " + type.getValue());
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
        Ruby runtime = recv.getRuntime();
        
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
            return getProxyClass(recv.getRuntime(), javaClass.javaClass(), interfaces, names);
        } catch (Error e) {
            RaiseException ex = recv.getRuntime().newArgumentError("unable to create proxy class for " + javaClass.getValue() + " : " + e.getMessage());
            //e.printStackTrace();
            ex.initCause(e);
            throw ex;
        } catch (InvocationTargetException e) {
            RaiseException ex = recv.getRuntime().newArgumentError("unable to create proxy class for " + javaClass.getValue() + " : " + e.getMessage());
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
        return buildRubyArray(getMethods());
    }

    @JRubyMethod
    public RubyArray interfaces() {
        return buildRubyArray(getInterfaces());
    }

    @JRubyMethod
    public RubyArray constructors() {
        return this.constructors;
    }

    public static void createJavaProxyModule(Ruby runtime) {
        // TODO Auto-generated method stub

        RubyModule javaProxyModule = runtime.getJavaSupport().getJavaModule();
        JavaProxyClass.createJavaProxyClassClass(runtime, javaProxyModule);
        ProxyMethodImpl.createJavaProxyMethodClass(runtime, javaProxyModule);
        JavaProxyConstructor.createJavaProxyConstructorClass(runtime, javaProxyModule);
    }

    public String nameOnInspection() {
        return "[Proxy:" + getSuperclass().getName() + "]";
    }
}
