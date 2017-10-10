/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
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

package org.jruby.javasupport.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaSupportImpl;
import org.jruby.runtime.Helpers;
import org.jruby.util.ArraySupport;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import static org.jruby.javasupport.JavaClass.EMPTY_CLASS_ARRAY;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class JavaProxyClassFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JavaProxyClassFactory.class);

    static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    static final Type JAVA_LANG_CLASS_TYPE = Type.getType(Class.class);

    private static final org.objectweb.asm.commons.Method forName = org.objectweb.asm.commons.Method
        .getMethod("java.lang.Class forName(java.lang.String)");

    private static final String INVOCATION_HANDLER_FIELD_NAME = "__handler";

    private static final String PROXY_CLASS_FIELD_NAME = "__proxy_class";

    private static final Type PROXY_METHOD_TYPE = Type.getType(JavaProxyMethod.class);

    private static final Type PROXY_CLASS_TYPE = Type.getType(JavaProxyClass.class);

    private static final Type INVOCATION_HANDLER_TYPE = Type.getType(JavaProxyInvocationHandler.class);

    // public Object invoke(Object receiver, JavaProxyMethod method, Object[] args)
    private static final org.objectweb.asm.commons.Method invoke = org.objectweb.asm.commons.Method
        .getMethod("java.lang.Object invoke(java.lang.Object, " + PROXY_METHOD_TYPE.getClassName() + ", java.lang.Object[])");

    private static final Type INTERNAL_PROXY_HELPER_TYPE = Type.getType(InternalJavaProxyHelper.class);

    // public static JavaProxyClass initProxyClass(Class)
    private static final org.objectweb.asm.commons.Method initProxyClass = org.objectweb.asm.commons.Method
        .getMethod(JavaProxyClass.class.getName() + " initProxyClass(java.lang.Class)");

    // public static JavaProxyMethod initProxyMethod(JavaProxyClass proxyClass, String name, String desc, boolean hasSuper)
    private static final org.objectweb.asm.commons.Method initProxyMethod = org.objectweb.asm.commons.Method
        .getMethod(PROXY_METHOD_TYPE.getClassName() + " initProxyMethod("
                    + JavaProxyClass.class.getName() + ",java.lang.String,java.lang.String,boolean)");

    private static final Type JAVA_PROXY_TYPE = Type.getType(InternalJavaProxy.class);

    private static final AtomicInteger counter = new AtomicInteger(0);

    private static int nextId() { return counter.incrementAndGet(); }

    public static JavaProxyClassFactory createFactory() {
        final String factoryClassName = Options.JI_PROXYCLASSFACTORY.load();

        JavaProxyClassFactory factory = null;
        if ( factoryClassName != null ) {
            try {
                Class clazz = Class.forName(factoryClassName);
                Object instance = clazz.newInstance();
                if ( instance instanceof JavaProxyClassFactory ) {
                    factory = (JavaProxyClassFactory) instance;
                    LOG.info("Created proxy class factory: {}", factory);
                } else {
                    LOG.error("Invalid proxy class factory: {}", instance);
                }
            }
            catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException creating proxy class factory: ", e);
            }
            catch (InstantiationException e) {
                LOG.error("InstantiationException creating proxy class factory: ", e);
            }
            catch (IllegalAccessException e) {
                LOG.error("IllegalAccessException creating proxy class factory: ", e);
            }
        }

        return factory != null ? factory : new JavaProxyClassFactory();
    }

    static ThreadLocal<Ruby> runtimeTLS = new ThreadLocal<>();

    public final JavaProxyClass genProxyClass(final Ruby runtime, ClassDefiningClassLoader loader,
        String targetClassName, Class superClass, Class[] interfaces, Set<String> names)
        throws InvocationTargetException {
        final Ruby prev = runtimeTLS.get();
        runtimeTLS.set(runtime);
        try {
            return newProxyClass(runtime, loader, targetClassName, superClass, interfaces, names);
        }
        finally { runtimeTLS.set(prev); }
    }

    public JavaProxyClass newProxyClass(final Ruby runtime, ClassDefiningClassLoader loader,
        String targetClassName, Class superClass, Class[] interfaces, Set<String> names)
        throws InvocationTargetException {

        if (targetClassName == null) {
            targetClassName = targetClassName(superClass);
        }
        validateArgs(runtime, targetClassName, superClass);

        Type selfType = Type.getType('L' + toInternalClassName(targetClassName) + ';');
        Map<MethodKey, MethodData> methods = collectMethods(superClass, interfaces, names);

        return generate(loader, targetClassName, superClass, interfaces, methods, selfType);
    }

    private JavaProxyClass generate(ClassDefiningClassLoader loader, String targetClassName,
                                    Class superClass, Class[] interfaces,
                                    Map<MethodKey, MethodData> methods, Type selfType) {
        ClassWriter cw = beginProxyClass(targetClassName, superClass, interfaces);

        GeneratorAdapter clazzInit = createClassInitializer(selfType, cw);

        generateConstructors(superClass, selfType, cw);
        generate___getProxyClass(selfType, cw);
        generate___getInvocationHandler(selfType, cw);
        generateProxyMethods(superClass, methods, selfType, cw, clazzInit);

        // finish class initializer
        clazzInit.returnValue();
        clazzInit.endMethod();

        // end class
        cw.visitEnd();

        Class clazz = loader.defineClass(selfType.getClassName(), cw.toByteArray());

        // trigger class initialization for the class
        try {
            Field proxy_class = clazz.getDeclaredField(PROXY_CLASS_FIELD_NAME);
            // proxy_class.setAccessible(true); // field is public
            return (JavaProxyClass) proxy_class.get(clazz);
        }
        catch (NoSuchFieldException|IllegalAccessException ex) {
            Helpers.throwException(ex); return null; // re-throws (although unexpected)
        }
    }

    private static String targetClassName(final Class<?> clazz) {
        // We always prepend an org.jruby.proxy package to the beginning
        // because java and javax packages are protected and signed
        // jars prevent us generating new classes with those package
        // names. See JRUBY-2439.
        final String fullName = clazz.getName();
        final int idx = fullName.lastIndexOf('.');
        String className = idx == -1 ? fullName : fullName.substring(idx + 1);
        return proxyPackageName(fullName)
                .append('.').append(className)
                .append("$Proxy").append(nextId()).toString();
    }

    private static ClassWriter beginProxyClass(final String className,
            final Class superClass, final Class[] interfaces) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // start class
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                toInternalClassName(className), /*signature*/ null,
                toInternalClassName(superClass),
                interfaceNamesForProxyClass(interfaces));

        // private final JavaProxyInvocationHandler __handler;
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                INVOCATION_HANDLER_FIELD_NAME,
                INVOCATION_HANDLER_TYPE.getDescriptor(), null, null
        ).visitEnd();

        // /* public */ static final JavaProxyClass __proxy_class;
        cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                PROXY_CLASS_FIELD_NAME,
                PROXY_CLASS_TYPE.getDescriptor(), null, null
        ).visitEnd();

        return cw;
    }

    private static String[] interfaceNamesForProxyClass(final Class[] interfaces) {
        String[] interfaceNames = new String[interfaces.length + 1];

        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = toInternalClassName(interfaces[i]);
        }
        // all proxies implement our InternalJavaProxy interface :
        interfaceNames[interfaces.length] = toInternalClassName(InternalJavaProxy.class);

        return interfaceNames;
    }

    private static void generateProxyMethods(Class superClass,
            Map<MethodKey, MethodData> methods, Type selfType, ClassVisitor cw,
            GeneratorAdapter clazzInit) {
        for (MethodData md: methods.values()) {
            Type superClassType = Type.getType(superClass);
            generateProxyMethod(selfType, superClassType, cw, clazzInit, md);
        }
    }

    /**
     * @see InternalJavaProxy
     */
    private static void generate___getInvocationHandler(Type selfType, ClassVisitor cw) {
        // public JavaProxyInvocationHandler ___getInvocationHandler() { return this.__handler; }

        // make getter for handler (due implements InternalJavaProxy)
        GeneratorAdapter gh = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                new org.objectweb.asm.commons.Method("___getInvocationHandler",
                        INVOCATION_HANDLER_TYPE, EMPTY_TYPE_ARRAY), null,
                EMPTY_TYPE_ARRAY, cw);

        gh.loadThis();
        gh.getField(selfType, INVOCATION_HANDLER_FIELD_NAME, INVOCATION_HANDLER_TYPE);
        gh.returnValue();
        gh.endMethod();
    }

    /**
     * @see InternalJavaProxy
     */
    private static void generate___getProxyClass(Type selfType, ClassVisitor cw) {
        // public JavaProxyClass __getProxyClass() { return /* static */ __proxy_class; }

        // make getter for proxy class (due implements InternalJavaProxy)
        GeneratorAdapter gpc = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                new org.objectweb.asm.commons.Method("___getProxyClass",
                        PROXY_CLASS_TYPE, EMPTY_TYPE_ARRAY), null,
                EMPTY_TYPE_ARRAY, cw);
        gpc.getStatic(selfType, PROXY_CLASS_FIELD_NAME, PROXY_CLASS_TYPE);
        gpc.returnValue();
        gpc.endMethod();
    }

    private static void generateConstructors(Class superClass, Type selfType, ClassVisitor cw) {
        Constructor[] cons = superClass.getDeclaredConstructors();

        for (int i = 0; i < cons.length; i++) {
            // if the constructor is private, pretend it doesn't exist
            if (Modifier.isPrivate(cons[i].getModifiers())) continue;

            // otherwise, define everything and let some of them fail at invocation
            generateConstructor(selfType, cons[i], cw);
        }
    }

    private static GeneratorAdapter createClassInitializer(Type selfType, ClassVisitor cw) {
        GeneratorAdapter clazzInit = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                new org.objectweb.asm.commons.Method("<clinit>", Type.VOID_TYPE, EMPTY_TYPE_ARRAY),
                null, EMPTY_TYPE_ARRAY, cw);

        clazzInit.visitLdcInsn(selfType.getClassName());
        clazzInit.invokeStatic(JAVA_LANG_CLASS_TYPE, forName);
        clazzInit.invokeStatic(INTERNAL_PROXY_HELPER_TYPE, initProxyClass);
        clazzInit.dup();
        clazzInit.putStatic(selfType, PROXY_CLASS_FIELD_NAME, PROXY_CLASS_TYPE);
        // __proxy_class = InternalJavaProxyHelper.initProxyClass( Class.forName(className) );

        return clazzInit;
    }

    private static void generateProxyMethod(Type selfType, Type superType,
            ClassVisitor cw, GeneratorAdapter clazzInit, MethodData md) {
        if (!md.generateProxyMethod()) return;

        org.objectweb.asm.commons.Method m = md.getMethod();
        Type[] ex = toTypes(md.getExceptions());

        String field_name = "__mth$" + md.getName() + md.scrambledSignature();

        // private static JavaProxyMethod __mth$sort$java_util_Comparator;
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                field_name, PROXY_METHOD_TYPE.getDescriptor(), null, null);
        fv.visitEnd();

        // static { ... } initializer block
        clazzInit.dup();
        clazzInit.push(m.getName());
        clazzInit.push(m.getDescriptor());
        clazzInit.push(md.isImplemented());
        // JavaProxyMethod initProxyMethod(JavaProxyClass proxyClass, String name, String desc, boolean hasSuper)
        clazzInit.invokeStatic(INTERNAL_PROXY_HELPER_TYPE, initProxyMethod);
        clazzInit.putStatic(selfType, field_name, PROXY_METHOD_TYPE);

        org.objectweb.asm.commons.Method sm = new org.objectweb.asm.commons.Method(
                "__super$" + m.getName(), m.getReturnType(), m.getArgumentTypes()
        );

        //
        // construct the proxy method
        //
        GeneratorAdapter ga = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, ex, cw);

        ga.loadThis();
        ga.getField(selfType, INVOCATION_HANDLER_FIELD_NAME, INVOCATION_HANDLER_TYPE);

        // if the method is extending something, then we have to test if the handler is initialized...

        if (md.isImplemented()) {
            ga.dup();
            Label ok = ga.newLabel();
            ga.ifNonNull(ok);

            ga.loadThis();
            ga.loadArgs();
            ga.invokeConstructor(superType, m);
            ga.returnValue();
            ga.mark(ok);
        }

        ga.loadThis();
        ga.getStatic(selfType, field_name, PROXY_METHOD_TYPE);

        if (m.getArgumentTypes().length == 0) {
            // load static empty array
            ga.getStatic(JAVA_PROXY_TYPE, "NO_ARGS", toType(Object[].class));
        } else {
            // box arguments
            ga.loadArgArray();
        }

        Label before = ga.mark();

        ga.invokeInterface(INVOCATION_HANDLER_TYPE, invoke);

        Label after = ga.mark();

        ga.unbox(m.getReturnType());
        ga.returnValue();

        // this is a simple rethrow handler
        Label rethrow = ga.mark();
        ga.visitInsn(Opcodes.ATHROW);

        for (int i = 0; i < ex.length; i++) {
            ga.visitTryCatchBlock(before, after, rethrow, ex[i].getInternalName());
        }

        ga.visitTryCatchBlock(before, after, rethrow, "java/lang/Error");
        ga.visitTryCatchBlock(before, after, rethrow, "java/lang/RuntimeException");

        Type thr = toType(Throwable.class);
        Label handler = ga.mark();
        Type udt = toType(UndeclaredThrowableException.class);
        int loc = ga.newLocal(thr);
        ga.storeLocal(loc, thr);
        ga.newInstance(udt);
        ga.dup();
        ga.loadLocal(loc, thr);
        ga.invokeConstructor(udt, org.objectweb.asm.commons.Method.getMethod("void <init>(java.lang.Throwable)"));
        ga.throwException();

        ga.visitTryCatchBlock(before, after, handler, "java/lang/Throwable");

        ga.endMethod();

        //
        // construct the super-proxy method
        //
        if (md.isImplemented()) {
            GeneratorAdapter ga2 = new GeneratorAdapter(Opcodes.ACC_PUBLIC, sm, null, ex, cw);

            ga2.loadThis();
            ga2.loadArgs();
            ga2.invokeConstructor(superType, m);
            ga2.returnValue();
            ga2.endMethod();
        }
    }

    private static Class[] generateConstructor(Type selfType, Constructor constructor, ClassVisitor cw) {
        Class[] superConstructorParameterTypes = constructor.getParameterTypes();
        Class[] newConstructorParameterTypes = new Class[superConstructorParameterTypes.length + 1];
        ArraySupport.copy(superConstructorParameterTypes, newConstructorParameterTypes, 0, superConstructorParameterTypes.length);
        newConstructorParameterTypes[superConstructorParameterTypes.length] = JavaProxyInvocationHandler.class;

        int access = Opcodes.ACC_PUBLIC;
        String name1 = "<init>";
        String signature = null;
        Class[] superConstructorExceptions = constructor.getExceptionTypes();
        boolean superConstructorVarArgs = constructor.isVarArgs();

        org.objectweb.asm.commons.Method super_m = new org.objectweb.asm.commons.Method(
                name1, Type.VOID_TYPE, toTypes(superConstructorParameterTypes));
        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(
                name1, Type.VOID_TYPE, toTypes(newConstructorParameterTypes));

        String[] exceptionNames = toInternalNames( superConstructorExceptions );
        MethodVisitor mv = cw.visitMethod(access, m.getName(), m.getDescriptor(), signature, exceptionNames);
        // marking with @SafeVarargs so that we can correctly detect proxied var-arg constructors :
        if ( superConstructorVarArgs ) mv.visitAnnotation(Type.getDescriptor(VarArgs.class), true);
        GeneratorAdapter ga = new GeneratorAdapter(access, m, mv);

        ga.loadThis();
        ga.loadArgs(0, superConstructorParameterTypes.length);
        ga.invokeConstructor(toType(constructor.getDeclaringClass()), super_m);

        ga.loadThis();
        ga.loadArg(superConstructorParameterTypes.length);

        ga.putField(selfType, INVOCATION_HANDLER_FIELD_NAME, INVOCATION_HANDLER_TYPE);

        // do a void return
        ga.returnValue();
        ga.endMethod();

        return newConstructorParameterTypes;
    }

    static boolean isVarArgs(final Constructor<?> ctor) {
        return ctor.isVarArgs() || ctor.getAnnotation(VarArgs.class) != null;
    }

    //static boolean isVarArgs(final Method method) {
    //    return method.isVarArgs() || method.getAnnotation(VarArgs.class) != null;
    //}

    private static String toInternalClassName(Class clazz) {
        return toInternalClassName(clazz.getName());
    }

    private static String toInternalClassName(String name) {
        return name.replace('.', '/');
    }

    private static Type toType(Class clazz) {
        return Type.getType(clazz);
    }

    private static Type[] toTypes(Class[] params) {
        Type[] types = new Type[params.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = Type.getType(params[i]);
        }
        return types;
    }

    private static String[] toInternalNames(final Class[] params) {
        if (params == null) return null;
        String[] names = new String[params.length];
        for (int i = 0; i < names.length; ++i) {
            names[i] = Type.getType(params[i]).getInternalName();
        }
        return names;
    }

    private static Map<MethodKey, MethodData> collectMethods(
            final Class superClass,
            final Class[] interfaces,
            final Set<String> names) {

        Map<MethodKey, MethodData> methods = new HashMap<>();

        HashSet<Class> allClasses = new HashSet<>();
        addClass(allClasses, methods, superClass, names);
        addInterfaces(allClasses, methods, interfaces, names);

        return methods;
    }

    static final class MethodData {

        final Set<Method> methods = new HashSet<Method>();

        final Method mostSpecificMethod;
        final Class[] mostSpecificParameterTypes;

        //private boolean hasPublicDecl = false;

        MethodData(final Method method) {
            this.mostSpecificMethod = method;
            this.mostSpecificParameterTypes = mostSpecificMethod.getParameterTypes();
            //hasPublicDecl = method.getDeclaringClass().isInterface() || Modifier.isPublic(method.getModifiers());
        }

        private StringBuilder scrambledSignature() {
            StringBuilder sb = new StringBuilder();
            for ( Class param : getParameterTypes() ) {
                sb.append('$');
                final char[] name = param.getName().toCharArray();
                for (int i = 0; i < name.length; i++) {
                    final char c;
                    switch ( c = name[i] ) {
                        case '.' : sb.append('_'); break;
                        case '[' : sb.append('1'); break;
                        case ';' : sb.append('2'); break;
                        default : sb.append(c);
                    }
                }
            }
            return sb;
        }

        public Class getDeclaringClass() {
            return mostSpecificMethod.getDeclaringClass();
        }

        private org.objectweb.asm.commons.Method getMethod() {
            return new org.objectweb.asm.commons.Method(getName(), Type
                    .getType(getReturnType()), getType(getParameterTypes()));
        }

        private static Type[] getType(Class[] parameterTypes) {
            Type[] result = new Type[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                result[i] = Type.getType(parameterTypes[i]);
            }
            return result;
        }

        private String getName() {
            return mostSpecificMethod.getName();
        }

        private Class[] getParameterTypes() {
            return mostSpecificParameterTypes;
        }

        private Class[] getExceptions() {
            final IdentityHashMap<Class, ?> exceptions = new IdentityHashMap<>(8);

            for ( final Method method : this.methods ) {
                Class[] exTypes = method.getExceptionTypes();
                for (int i = 0; i < exTypes.length; i++) {
                    final Class<?> exType = exTypes[i];

                    if ( exceptions.containsKey(exType) ) continue;

                    boolean add = true;
                    Iterator<Class> it = exceptions.keySet().iterator();
                    while ( it.hasNext() ) {
                        final Class<?> curType = it.next();

                        if ( curType.isAssignableFrom(exType) ) {
                            add = false;
                            break;
                        }
                        if ( exType.isAssignableFrom(curType) ) {
                            it.remove();
                            add = true;
                        }
                    }

                    if ( add ) exceptions.put(exType, null);
                }
            }
            return exceptions.isEmpty() ? EMPTY_CLASS_ARRAY : exceptions.keySet().toArray(new Class[ exceptions.size() ]);
        }

        private boolean generateProxyMethod() {
            return ! isFinal() && ! isPrivate();
        }

        private void add(Method method) {
            methods.add(method);
            //hasPublicDecl |= Modifier.isPublic(method.getModifiers());
        }

        Class getReturnType() {
            return mostSpecificMethod.getReturnType();
        }

        boolean isFinal() {
            if ( mostSpecificMethod.getDeclaringClass().isInterface() ) {
                return false;
            }
            return Modifier.isFinal( mostSpecificMethod.getModifiers() );
        }

        boolean isPrivate() {
            if ( mostSpecificMethod.getDeclaringClass().isInterface() ) {
                return false;
            }
            return Modifier.isPrivate( mostSpecificMethod.getModifiers() );
        }

        boolean isImplemented() {
            if ( mostSpecificMethod.getDeclaringClass().isInterface() ) {
                return false;
            }
            return ! Modifier.isAbstract( mostSpecificMethod.getModifiers() );
        }
    }

    static final class MethodKey {

        private final String name;
        private final Class[] arguments;

        MethodKey(final Method method) {
            this.name = method.getName();
            this.arguments = method.getParameterTypes();
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof MethodKey ) {
                MethodKey key = (MethodKey) obj;
                return name.equals(key.name) && Arrays.equals(arguments, key.arguments);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            final StringBuilder str = new StringBuilder().append(name);
            str.append('(');
            final int last = arguments.length - 1;
            for ( int i=0; i<last; i++ ) {
                str.append(arguments[i].getName()).append(',');
            }
            if ( last >= 0 ) str.append(arguments[last].getName());
            str.append(')');
            return str.toString();
        }
    }

    private static void addInterfaces(
            final Set<Class> allClasses,
            final Map<MethodKey, MethodData> methods,
            final Class[] ifaces,
            final Set<String> names) {
        for ( int i = 0; i < ifaces.length; i++ ) {
            addInterface(allClasses, methods, ifaces[i], names);
        }
    }

    private static void addInterface(
            final Set<Class> allClasses,
            final Map<MethodKey, MethodData> methods,
            final Class iface,
            final Set<String> names) {
        if ( allClasses.add(iface) ) {
            addMethods(methods, iface, names);
            addInterfaces(allClasses, methods, iface.getInterfaces(), names);
        }
    }

    private static void addMethods(
            final Map<MethodKey, MethodData> methods,
            final Class classOrIface,
            final Set<String> names) {
        final Method[] decMethods = classOrIface.getDeclaredMethods();
        for ( int i = 0; i < decMethods.length; i++ ) {
            final Method decMethod = decMethods[i];
            if ( names.contains(decMethod.getName()) ) {
                addMethod(methods, decMethod);
            }
        }
    }

    private static void addMethod(final Map<MethodKey, MethodData> methods, final Method method) {
        final int mod = method.getModifiers();
        if ( Modifier.isStatic(mod) || Modifier.isPrivate(mod) ) {
            return;
        }

        MethodKey methodKey = new MethodKey(method);
        MethodData methodData = methods.get(methodKey);
        if (methodData == null) {
            methodData = new MethodData(method);
            methods.put(methodKey, methodData);
        }
        methodData.add(method);
    }

    private static void addClass(
            final Set<Class> allClasses,
            final Map<MethodKey, MethodData> methods,
            final Class clazz,
            final Set<String> names) {
        if ( allClasses.add(clazz) ) {
            addMethods(methods, clazz, names);
            Class superClass = clazz.getSuperclass();
            if ( superClass != null ) {
                addClass(allClasses, methods, superClass, names);
            }
            addInterfaces(allClasses, methods, clazz.getInterfaces(), names);
        }
    }

    private static void validateArgs(Ruby runtime, String targetClassName, Class superClass) {
        if ( Modifier.isFinal(superClass.getModifiers()) ) {
            throw runtime.newTypeError("cannot extend final class " + superClass.getName());
        }

        if ( ! hasPublicOrProtectedConstructor(superClass) ) {
            throw runtime.newTypeError("class " + superClass.getName() + " doesn't have a public or protected constructor");
        }

        String targetPackage = packageName(targetClassName);

        String packagePath = targetPackage.replace('.', '/');
        if (packagePath.startsWith("java")) {
            throw runtime.newTypeError("cannot add classes to package " + packagePath);
        }

        final Package pkg = Package.getPackage(packagePath);
        if ( pkg != null && pkg.isSealed() ) {
            throw runtime.newTypeError("package " + pkg + " is sealed");
        }
    }

    private static boolean hasPublicOrProtectedConstructor(final Class clazz) {
        Constructor[] constructors = clazz.getDeclaredConstructors();
        for ( Constructor constructor : constructors ) {
            final int mod = constructor.getModifiers();
            if ( Modifier.isPublic(mod) || Modifier.isProtected(mod) ) {
                return true;
            }
        }
        return false;
    }

    private static String packageName(String clazzName) {
        int idx = clazzName.lastIndexOf('.');
        if ( idx == -1 ) return "";
        return clazzName.substring(0, idx);
    }

    private static StringBuilder proxyPackageName(final String className) {
        final String proxyPackagePrefix = "org.jruby.proxy";
        final StringBuilder str = new StringBuilder(proxyPackagePrefix.length() + className.length() + 8);
        final int idx = className.lastIndexOf('.');
        str.append(proxyPackagePrefix);
        return idx == -1 ? str : str.append('.').append(className.substring(0, idx));
    }

    /**
     * Variable arguments marker for generated constructor.
     * @note could have used @SafeVarargs but it's Java 7+
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
    public static @interface VarArgs {}

    @Deprecated
    public final JavaProxyClass genProxyClass(final Ruby runtime, ClassLoader loader,
                                              String targetClassName, Class superClass, Class[] interfaces, Set<String> names)
            throws InvocationTargetException {
        if (loader instanceof ClassDefiningClassLoader) {
            return genProxyClass(runtime, (ClassDefiningClassLoader) loader, targetClassName, superClass, interfaces, names);
        }

        return genProxyClass(runtime, (ClassDefiningClassLoader) new OneShotClassLoader(loader), targetClassName, superClass, interfaces, names);
    }

    @Deprecated
    public JavaProxyClass newProxyClass(final Ruby runtime, ClassLoader loader,
                                        String targetClassName, Class superClass, Class[] interfaces, Set<String> names)
            throws InvocationTargetException {
        if (loader instanceof ClassDefiningClassLoader) {
            return newProxyClass(runtime, (ClassDefiningClassLoader) loader, targetClassName, superClass, interfaces, names);
        }

        return newProxyClass(runtime, (ClassDefiningClassLoader) new OneShotClassLoader(loader), targetClassName, superClass, interfaces, names);
    }

}
