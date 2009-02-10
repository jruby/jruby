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

package org.jruby.javasupport.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public class JavaProxyClassFactory {

    private static final Type JAVA_LANG_CLASS_TYPE = Type.getType(Class.class);

    private static final Type[] EMPTY_TYPE_ARR = new Type[0];

    private static final org.objectweb.asm.commons.Method HELPER_GET_PROXY_CLASS_METHOD = org.objectweb.asm.commons.Method
            .getMethod(JavaProxyClass.class.getName()
                    + " initProxyClass(java.lang.Class)");

    private static final org.objectweb.asm.commons.Method CLASS_FORNAME_METHOD = org.objectweb.asm.commons.Method
            .getMethod("java.lang.Class forName(java.lang.String)");

    private static final String INVOCATION_HANDLER_FIELD_NAME = "__handler";

    private static final String PROXY_CLASS_FIELD_NAME = "__proxy_class";

    private static final Class[] EMPTY_CLASS_ARR = new Class[0];

    private static final Type INVOCATION_HANDLER_TYPE = Type
            .getType(JavaProxyInvocationHandler.class);

    private static final Type PROXY_METHOD_TYPE = Type
            .getType(JavaProxyMethod.class);

    private static final Type PROXY_CLASS_TYPE = Type
            .getType(JavaProxyClass.class);

    private static final org.objectweb.asm.commons.Method INVOCATION_HANDLER_INVOKE_METHOD = org.objectweb.asm.commons.Method
            .getMethod("java.lang.Object invoke(java.lang.Object, "
                    + PROXY_METHOD_TYPE.getClassName()
                    + ", java.lang.Object[])");

    private static final Type PROXY_HELPER_TYPE = Type
            .getType(InternalJavaProxyHelper.class);

    private static final org.objectweb.asm.commons.Method PROXY_HELPER_GET_METHOD = org.objectweb.asm.commons.Method
            .getMethod(PROXY_METHOD_TYPE.getClassName() + " initProxyMethod("
                    + JavaProxyClass.class.getName()
                    + ",java.lang.String,java.lang.String,boolean)");

    private static final Type JAVA_PROXY_TYPE = Type
            .getType(InternalJavaProxy.class);

    private static int counter;

    private static Map proxies = Collections.synchronizedMap(new HashMap());

    private static Method defineClass_method; // statically initialized below

    private static synchronized int nextId() {
        return counter++;
    }
    
    @Deprecated
    static JavaProxyClass newProxyClass(ClassLoader loader,
            String targetClassName, Class superClass, Class[] interfaces, Set names)
            throws InvocationTargetException {
        return newProxyClass(JavaProxyClass.runtimeTLS.get(), loader, targetClassName, superClass, interfaces, names);
    }
    
    // TODO: we should be able to optimize this quite a bit post-1.0.  JavaClass already
    // has all the methods organized by method name; the next version (supporting protected
    // methods/fields) will have them organized even further. So collectMethods here can
    // just lookup the overridden methods in the JavaClass map, should be much faster.
    static JavaProxyClass newProxyClass(Ruby runtime, ClassLoader loader,
            String targetClassName, Class superClass, Class[] interfaces, Set names)
            throws InvocationTargetException {
        if (loader == null) {
            loader = JavaProxyClassFactory.class.getClassLoader();
        }

        if (superClass == null) {
            superClass = Object.class;
        }

        if (interfaces == null) {
            interfaces = EMPTY_CLASS_ARR;
        }

        Set key = new HashSet();
        key.add(superClass);
        for (int i = 0; i < interfaces.length; i++) {
            key.add(interfaces[i]);
        }

        // add (potentially) overridden names to the key.
        // TODO: see note above re: optimizations
        if (names != null) {
            key.addAll(names);
        }

        JavaProxyClass proxyClass = (JavaProxyClass) proxies.get(key);
        if (proxyClass == null) {

            if (targetClassName == null) {
                // We always prepend an org.jruby.proxy package to the beginning
                // because java and javax packages are protected and signed
                // jars prevent us generating new classes with those package
                // names. See JRUBY-2439.
                String pkg = proxyPackageName(superClass);
                String fullName = superClass.getName();
                int ix = fullName.lastIndexOf('.');
                String cName = fullName;
                if(ix != -1) {
                    cName = fullName.substring(ix+1);
                }
                targetClassName = pkg + "." + cName + "$Proxy" + nextId();
            }

            validateArgs(runtime, targetClassName, superClass);

            Map methods = new HashMap();
            collectMethods(superClass, interfaces, methods, names);

            Type selfType = Type.getType("L"
                    + toInternalClassName(targetClassName) + ";");
            proxyClass = generate(loader, targetClassName, superClass,
                    interfaces, methods, selfType);

            proxies.put(key, proxyClass);
        }

        return proxyClass;
    }

    static JavaProxyClass newProxyClass(ClassLoader loader,
            String targetClassName, Class superClass, Class[] interfaces)
            throws InvocationTargetException {
        return newProxyClass(loader,targetClassName,superClass,interfaces,null);
    }

    private static JavaProxyClass generate(final ClassLoader loader,
            final String targetClassName, final Class superClass,
            final Class[] interfaces, final Map methods, final Type selfType) {
        ClassWriter cw = beginProxyClass(targetClassName, superClass,
                interfaces);

        GeneratorAdapter clazzInit = createClassInitializer(selfType, cw);

        generateConstructors(superClass, selfType, cw);

        generateGetProxyClass(selfType, cw);

        generateGetInvocationHandler(selfType, cw);

        generateProxyMethods(superClass, methods, selfType, cw, clazzInit);

        // finish class initializer
        clazzInit.returnValue();
        clazzInit.endMethod();

        // end class
        cw.visitEnd();

        byte[] data = cw.toByteArray();

        /*
         * try { FileOutputStream o = new
         * FileOutputStream(targetClassName.replace( '/', '.') + ".class");
         * o.write(data); o.close(); } catch (IOException ex) {
         * ex.printStackTrace(); }
         */

        Class clazz = invokeDefineClass(loader, selfType.getClassName(), data);

        // trigger class initialization for the class
        try {
            Field proxy_class = clazz.getDeclaredField(PROXY_CLASS_FIELD_NAME);
            proxy_class.setAccessible(true);
            return (JavaProxyClass) proxy_class.get(clazz);
        } catch (Exception ex) {
            InternalError ie = new InternalError();
            ie.initCause(ex);
            throw ie;
        }
    }

    static {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    defineClass_method = ClassLoader.class.getDeclaredMethod(
                            "defineClass", new Class[] { String.class,
                                    byte[].class, int.class, int.class, ProtectionDomain.class });
                } catch (Exception e) {
                    // should not happen!
                    e.printStackTrace();
                    return null;
                }
                defineClass_method.setAccessible(true);
                return null;
            }
        });
    }

    private static Class invokeDefineClass(ClassLoader loader,
            String className, byte[] data) {
        try {
            return (Class) defineClass_method
                    .invoke(loader, new Object[] { className, data,
                            new Integer(0), new Integer(data.length), JavaProxyClassFactory.class.getProtectionDomain() });
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private static ClassWriter beginProxyClass(final String targetClassName,
            final Class superClass, final Class[] interfaces) {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;
        String name = toInternalClassName(targetClassName);
        String signature = null;
        String supername = toInternalClassName(superClass);
        String[] interfaceNames = new String[interfaces.length + 1];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = toInternalClassName(interfaces[i]);
        }
        interfaceNames[interfaces.length] = toInternalClassName(InternalJavaProxy.class);

        // start class
        cw.visit(Opcodes.V1_3, access, name, signature, supername,
                interfaceNames);

        cw.visitField(Opcodes.ACC_PRIVATE, INVOCATION_HANDLER_FIELD_NAME,
                INVOCATION_HANDLER_TYPE.getDescriptor(), null, null).visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                PROXY_CLASS_FIELD_NAME, PROXY_CLASS_TYPE.getDescriptor(), null,
                null).visitEnd();

        return cw;
    }

    private static void generateProxyMethods(Class superClass, Map methods,
            Type selfType, ClassVisitor cw, GeneratorAdapter clazzInit) {
        Iterator it = methods.values().iterator();
        while (it.hasNext()) {
            MethodData md = (MethodData) it.next();
            Type superClassType = Type.getType(superClass);
            generateProxyMethod(selfType, superClassType, cw, clazzInit, md);
        }
    }

    private static void generateGetInvocationHandler(Type selfType,
            ClassVisitor cw) {
        // make getter for handler
        GeneratorAdapter gh = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                new org.objectweb.asm.commons.Method("___getInvocationHandler",
                        INVOCATION_HANDLER_TYPE, EMPTY_TYPE_ARR), null,
                EMPTY_TYPE_ARR, cw);

        gh.loadThis();
        gh.getField(selfType, INVOCATION_HANDLER_FIELD_NAME,
                INVOCATION_HANDLER_TYPE);
        gh.returnValue();
        gh.endMethod();
    }

    private static void generateGetProxyClass(Type selfType, ClassVisitor cw) {
        // make getter for proxy class
        GeneratorAdapter gpc = new GeneratorAdapter(Opcodes.ACC_PUBLIC,
                new org.objectweb.asm.commons.Method("___getProxyClass",
                        PROXY_CLASS_TYPE, EMPTY_TYPE_ARR), null,
                EMPTY_TYPE_ARR, cw);
        gpc.getStatic(selfType, PROXY_CLASS_FIELD_NAME, PROXY_CLASS_TYPE);
        gpc.returnValue();
        gpc.endMethod();
    }

    private static void generateConstructors(Class superClass, Type selfType,
            ClassVisitor cw) {
        Constructor[] cons = superClass.getDeclaredConstructors();
        for (int i = 0; i < cons.length; i++) {
            Constructor constructor = cons[i];

            // We generate all constructors and let some fail during invocation later
            generateConstructor(selfType, constructor, cw);
        }
    }

    private static GeneratorAdapter createClassInitializer(Type selfType,
            ClassVisitor cw) {
        GeneratorAdapter clazzInit;
        clazzInit = new GeneratorAdapter(Opcodes.ACC_PRIVATE
                | Opcodes.ACC_STATIC, new org.objectweb.asm.commons.Method(
                "<clinit>", Type.VOID_TYPE, EMPTY_TYPE_ARR), null,
                EMPTY_TYPE_ARR, cw);

        clazzInit.visitLdcInsn(selfType.getClassName());
        clazzInit.invokeStatic(JAVA_LANG_CLASS_TYPE, CLASS_FORNAME_METHOD);
        clazzInit
                .invokeStatic(PROXY_HELPER_TYPE, HELPER_GET_PROXY_CLASS_METHOD);
        clazzInit.dup();
        clazzInit.putStatic(selfType, PROXY_CLASS_FIELD_NAME, PROXY_CLASS_TYPE);
        return clazzInit;
    }

    private static void generateProxyMethod(Type selfType, Type superType,
            ClassVisitor cw, GeneratorAdapter clazzInit, MethodData md) {
        if (!md.generateProxyMethod()) {
            return;
        }

        org.objectweb.asm.commons.Method m = md.getMethod();
        Type[] ex = toType(md.getExceptions());

        String field_name = "__mth$" + md.getName() + md.scrambledSignature();

        // create static private method field
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE
                | Opcodes.ACC_STATIC, field_name, PROXY_METHOD_TYPE
                .getDescriptor(), null, null);
        fv.visitEnd();

        clazzInit.dup();
        clazzInit.push(m.getName());
        clazzInit.push(m.getDescriptor());
        clazzInit.push(md.isImplemented());
        clazzInit.invokeStatic(PROXY_HELPER_TYPE, PROXY_HELPER_GET_METHOD);
        clazzInit.putStatic(selfType, field_name, PROXY_METHOD_TYPE);

        org.objectweb.asm.commons.Method sm = new org.objectweb.asm.commons.Method(
                "__super$" + m.getName(), m.getReturnType(), m
                        .getArgumentTypes());

        //
        // construct the proxy method
        //
        GeneratorAdapter ga = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null,
                ex, cw);

        ga.loadThis();
        ga.getField(selfType, INVOCATION_HANDLER_FIELD_NAME,
                INVOCATION_HANDLER_TYPE);

        // if the method is extending something, then we have
        // to test if the handler is initialized...

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
            ga.getStatic(JAVA_PROXY_TYPE, "NO_ARGS", Type
                    .getType(Object[].class));
        } else {
            // box arguments
            ga.loadArgArray();
        }

        Label before = ga.mark();

        ga.invokeInterface(INVOCATION_HANDLER_TYPE,
                INVOCATION_HANDLER_INVOKE_METHOD);

        Label after = ga.mark();

        ga.unbox(m.getReturnType());
        ga.returnValue();

        // this is a simple rethrow handler
        Label rethrow = ga.mark();
        ga.visitInsn(Opcodes.ATHROW);

        for (int i = 0; i < ex.length; i++) {
            ga.visitTryCatchBlock(before, after, rethrow, ex[i]
                    .getInternalName());
        }

        ga.visitTryCatchBlock(before, after, rethrow, "java/lang/Error");
        ga.visitTryCatchBlock(before, after, rethrow,
                "java/lang/RuntimeException");

        Type thr = Type.getType(Throwable.class);
        Label handler = ga.mark();
        Type udt = Type.getType(UndeclaredThrowableException.class);
        int loc = ga.newLocal(thr);
        ga.storeLocal(loc, thr);
        ga.newInstance(udt);
        ga.dup();
        ga.loadLocal(loc, thr);
        ga.invokeConstructor(udt, org.objectweb.asm.commons.Method
                .getMethod("void <init>(java.lang.Throwable)"));
        ga.throwException();

        ga.visitTryCatchBlock(before, after, handler, "java/lang/Throwable");

        ga.endMethod();

        //
        // construct the super-proxy method
        //
        if (md.isImplemented()) {

            GeneratorAdapter ga2 = new GeneratorAdapter(Opcodes.ACC_PUBLIC, sm,
                    null, ex, cw);

            ga2.loadThis();
            ga2.loadArgs();
            ga2.invokeConstructor(superType, m);
            ga2.returnValue();
            ga2.endMethod();
        }
    }

    private static Class[] generateConstructor(Type selfType,
            Constructor constructor, ClassVisitor cw) {

        Class[] superConstructorParameterTypes = constructor
                .getParameterTypes();
        Class[] newConstructorParameterTypes = new Class[superConstructorParameterTypes.length + 1];
        System.arraycopy(superConstructorParameterTypes, 0,
                newConstructorParameterTypes, 0,
                superConstructorParameterTypes.length);
        newConstructorParameterTypes[superConstructorParameterTypes.length] = JavaProxyInvocationHandler.class;

        int access = Opcodes.ACC_PUBLIC;
        String name1 = "<init>";
        String signature = null;
        Class[] superConstructorExceptions = constructor.getExceptionTypes();

        org.objectweb.asm.commons.Method super_m = new org.objectweb.asm.commons.Method(
                name1, Type.VOID_TYPE, toType(superConstructorParameterTypes));
        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(
                name1, Type.VOID_TYPE, toType(newConstructorParameterTypes));

        GeneratorAdapter ga = new GeneratorAdapter(access, m, signature,
                toType(superConstructorExceptions), cw);

        ga.loadThis();
        ga.loadArgs(0, superConstructorParameterTypes.length);
        ga.invokeConstructor(Type.getType(constructor.getDeclaringClass()),
                super_m);

        ga.loadThis();
        ga.loadArg(superConstructorParameterTypes.length);
        ga.putField(selfType, INVOCATION_HANDLER_FIELD_NAME,
                INVOCATION_HANDLER_TYPE);

        // do a void return
        ga.returnValue();
        ga.endMethod();
        return newConstructorParameterTypes;
    }

    private static String toInternalClassName(Class clazz) {
        return toInternalClassName(clazz.getName());
    }

    private static String toInternalClassName(String name) {
        return name.replace('.', '/');
    }

    private static Type[] toType(Class[] parameterTypes) {
        Type[] result = new Type[parameterTypes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Type.getType(parameterTypes[i]);
        }
        return result;
    }

    private static void collectMethods(Class superClass, Class[] interfaces,
            Map methods, Set names) {
        HashSet allClasses = new HashSet();
        addClass(allClasses, methods, superClass, names);
        addInterfaces(allClasses, methods, interfaces, names);
    }

    static class MethodData {
        Set methods = new HashSet();

        final Method mostSpecificMethod;
        final Class[] mostSpecificParameterTypes;

        boolean hasPublicDecl = false;

        MethodData(Method method) {
            this.mostSpecificMethod = method;
            this.mostSpecificParameterTypes = mostSpecificMethod.getParameterTypes();
            hasPublicDecl = method.getDeclaringClass().isInterface()
                    || Modifier.isPublic(method.getModifiers());
        }

        public String scrambledSignature() {
            StringBuilder sb = new StringBuilder();
            Class[] parms = getParameterTypes();
            for (int i = 0; i < parms.length; i++) {
                sb.append('$');
                String name = parms[i].getName();
                name = name.replace('[', '1');
                name = name.replace('.', '_');
                name = name.replace(';', '2');
                sb.append(name);
            }
            return sb.toString();
        }

        public Class getDeclaringClass() {
            return mostSpecificMethod.getDeclaringClass();
        }

        public org.objectweb.asm.commons.Method getMethod() {
            return new org.objectweb.asm.commons.Method(getName(), Type
                    .getType(getReturnType()), getType(getParameterTypes()));
        }

        private Type[] getType(Class[] parameterTypes) {
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

        public Class[] getExceptions() {

            Set all = new HashSet();

            Iterator it = methods.iterator();
            while (it.hasNext()) {
                Method m = (Method) it.next();
                Class[] ex = m.getExceptionTypes();
                for (int i = 0; i < ex.length; i++) {
                    Class exx = ex[i];

                    if (all.contains(exx)) {
                        continue;
                    }

                    boolean add = true;
                    Iterator it2 = all.iterator();
                    while (it2.hasNext()) {
                        Class de = (Class) it2.next();

                        if (de.isAssignableFrom(exx)) {
                            add = false;
                            break;
                        } else if (exx.isAssignableFrom(de)) {
                            it2.remove();
                            add = true;
                        }

                    }

                    if (add) {
                        all.add(exx);
                    }
                }
            }

            return (Class[]) all.toArray(new Class[all.size()]);
        }

        public boolean generateProxyMethod() {
            return !isFinal() && !isPrivate();
        }

        public void add(Method method) {
            methods.add(method);
            hasPublicDecl |= Modifier.isPublic(method.getModifiers());
        }

        Class getReturnType() {
            return mostSpecificMethod.getReturnType();
        }

        boolean isFinal() {
            if (mostSpecificMethod.getDeclaringClass().isInterface()) {
                return false;
            }

            int mod = mostSpecificMethod.getModifiers();
            return Modifier.isFinal(mod);
        }

        boolean isPrivate() {
            if (mostSpecificMethod.getDeclaringClass().isInterface()) {
                return false;
            }

            int mod = mostSpecificMethod.getModifiers();
            return Modifier.isPrivate(mod);
        }

        boolean isImplemented() {
            if (mostSpecificMethod.getDeclaringClass().isInterface()) {
                return false;
            }

            int mod = mostSpecificMethod.getModifiers();
            return !Modifier.isAbstract(mod);
        }
    }

    static class MethodKey {
        private String name;

        private Class[] arguments;

        MethodKey(Method m) {
            this.name = m.getName();
            this.arguments = m.getParameterTypes();
        }

        public boolean equals(Object obj) {
            if (obj instanceof MethodKey) {
                MethodKey key = (MethodKey) obj;

                return name.equals(key.name)
                        && Arrays.equals(arguments, key.arguments);
            }

            return false;
        }

        public int hashCode() {
            return name.hashCode();
        }
    }

    private static void addInterfaces(Set allClasses, Map methods,
            Class[] interfaces, Set names) {
        for (int i = 0; i < interfaces.length; i++) {
            addInterface(allClasses, methods, interfaces[i], names);
        }
    }

    private static void addInterface(Set allClasses, Map methods,
            Class interfaze, Set names) {
        if (allClasses.add(interfaze)) {
            addMethods(methods, interfaze, names);
            addInterfaces(allClasses, methods, interfaze.getInterfaces(), names);
        }
    }

    private static void addMethods(Map methods, Class classOrInterface, Set names) {
        Method[] mths = classOrInterface.getDeclaredMethods();
        for (int i = 0; i < mths.length; i++) {
            if (names == null || names.contains(mths[i].getName())) {
                addMethod(methods, mths[i]);
            }
        }
    }

    private static void addMethod(Map methods, Method method) {
        int acc = method.getModifiers();
        
        if (Modifier.isStatic(acc) || Modifier.isPrivate(acc)) {
            return;
        }

        MethodKey mk = new MethodKey(method);
        MethodData md = (MethodData) methods.get(mk);
        if (md == null) {
            md = new MethodData(method);
            methods.put(mk, md);
        }
        md.add(method);
    }

    private static void addClass(Set allClasses, Map methods, Class clazz, Set names) {
        if (allClasses.add(clazz)) {
            addMethods(methods, clazz, names);
            Class superClass = clazz.getSuperclass();
            if (superClass != null) {
                addClass(allClasses, methods, superClass, names);
            }

            addInterfaces(allClasses, methods, clazz.getInterfaces(), names);
        }
    }

    private static void validateArgs(Ruby runtime, String targetClassName, Class superClass) {

        if (Modifier.isFinal(superClass.getModifiers())) {
            throw runtime.newTypeError("cannot extend final class " + superClass.getName());
        }

        String targetPackage = packageName(targetClassName);

        String pkg = targetPackage.replace('.', '/');
        if (pkg.startsWith("java")) {
            throw runtime.newTypeError("cannot add classes to package " + pkg);
        }

        Package p = Package.getPackage(pkg);
        if (p != null) {
            if (p.isSealed()) {
                throw runtime.newTypeError("package " + p + " is sealed");
            }
        }
    }

    private static String packageName(Class clazz) {
        String clazzName = clazz.getName();
        return packageName(clazzName);
    }

    private static String packageName(String clazzName) {
        int idx = clazzName.lastIndexOf('.');
        if (idx == -1) {
            return "";
        } else {
            return clazzName.substring(0, idx);
        }
    }

    private static String proxyPackageName(Class clazz) {
        String clazzName = clazz.getName();
        int idx = clazzName.lastIndexOf('.');
        if (idx == -1) {
            return "org.jruby.proxy";
        } else {
            return "org.jruby.proxy." + clazzName.substring(0, idx);
        }
    }

}
