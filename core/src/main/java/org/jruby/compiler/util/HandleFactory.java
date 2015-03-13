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
 * Copyright (C) 2008 Charles O Nutter <headius@headius.com>
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
package org.jruby.compiler.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jruby.compiler.JITCompiler;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

public class HandleFactory {
    public static class Handle {
        private Error fail() { return new AbstractMethodError("invalid call signature for target method: " + getClass()); }
        public Object invoke(Object receiver) { throw fail(); }
        public Object invoke(Object receiver, Object arg0) { throw fail(); }
        public Object invoke(Object receiver, Object arg0, Object arg1) { throw fail(); }
        public Object invoke(Object receiver, Object arg0, Object arg1, Object arg2) { throw fail(); }
//        public Object invoke(Object receiver, Object arg0, Object arg1, Object arg2, Object arg3) { throw fail(); }
//        public Object invoke(Object receiver, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) { throw fail(); }
        public Object invoke(Object receiver, Object... args) { throw fail(); }
    }
    
    public static Handle createHandle(ClassDefiningClassLoader classLoader, Method method) {
        String name = createHandleName(method);

        Class handleClass;
        try {
            handleClass = classLoader.loadClass(name);
            return (Handle)handleClass.newInstance();
        } catch (Exception e) {
        }

        handleClass = createHandleClass(classLoader, method, name);
        
        try {
            return (Handle)handleClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class createHandleClass(ClassDefiningClassLoader classLoader, Method method, String name) {
        byte[] bytes = createHandleBytes(method, name);
        return (classLoader != null ? classLoader : new ClassDefiningJRubyClassLoader(ClassDefiningJRubyClassLoader.class.getClassLoader())).defineClass(name, bytes);
    }

    public static byte[] createHandleBytes(Method method, String name) {
        Class returnType = method.getReturnType();
        Class[] paramTypes = method.getParameterTypes();
        ClassVisitor cv = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cv.visit(ACC_PUBLIC | ACC_FINAL | ACC_SUPER, V1_5, name, null, p(Handle.class), null);

        SkinnyMethodAdapter m;
        String signature;
        boolean needsArgsVersion = true;
        switch (paramTypes.length) {
        case 0:
            signature = sig(Object.class, Object.class);
            break;
        case 1:
            signature = sig(Object.class, Object.class, Object.class);
            break;
        case 2:
            signature = sig(Object.class, Object.class, Object.class, Object.class);
            break;
        case 3:
            signature = sig(Object.class, Object.class, Object.class, Object.class, Object.class);
            break;
//        case 4:
//            signature = sig(Object.class, Object.class, Object.class, Object.class, Object.class);
//            break;
//        case 5:
//            signature = sig(Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
//            break;
        default:
            needsArgsVersion = false;
            signature = sig(Object.class, Object.class, Object[].class);
            break;
        }
        m = new SkinnyMethodAdapter(cv, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "invoke", signature, null, null);

        m.start();

        // load receiver
        if (!Modifier.isStatic(method.getModifiers())) {
            m.aload(1); // receiver
            if (method.getDeclaringClass() != Object.class) {
                m.checkcast(p(method.getDeclaringClass()));
            }
        }

        // load arguments
        switch (paramTypes.length) {
        case 0:
        case 1:
        case 2:
        case 3:
//        case 4:
//        case 5:
            for (int i = 0; i < paramTypes.length; i++) {
                loadUnboxedArgument(m, i + 2, paramTypes[i]);
            }
            break;
        default:
            for (int i = 0; i < paramTypes.length; i++) {
                m.aload(2); // Object[] args
                m.pushInt(i);
                m.aaload();
                Class paramClass = paramTypes[i];
                if (paramClass.isPrimitive()) {
                    Class boxType = getBoxType(paramClass);
                    m.checkcast(p(boxType));
                    m.invokevirtual(p(boxType), paramClass.toString() + "Value", sig(paramClass));
                } else if (paramClass != Object.class) {
                    m.checkcast(p(paramClass));
                }
            }
            break;
        }

        if (Modifier.isStatic(method.getModifiers())) {
            m.invokestatic(p(method.getDeclaringClass()), method.getName(), sig(returnType, paramTypes));
        } else if (Modifier.isInterface(method.getDeclaringClass().getModifiers())) {
            m.invokeinterface(p(method.getDeclaringClass()), method.getName(), sig(returnType, paramTypes));
        } else {
            m.invokevirtual(p(method.getDeclaringClass()), method.getName(), sig(returnType, paramTypes));
        }

        if (returnType == void.class) {
            m.aconst_null();
        } else if (returnType.isPrimitive()) {
            Class boxType = getBoxType(returnType);
            m.invokestatic(p(boxType), "valueOf", sig(boxType, returnType));
        }
        m.areturn();
        m.end();

        if (needsArgsVersion) {
            m = new SkinnyMethodAdapter(cv, ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "invoke", sig(Object.class, Object.class, Object[].class), null, null);

            m.start();

            // load handle
            m.aload(0);

            // load receiver
            m.aload(1);

            // load arguments
            for (int i = 0; i < paramTypes.length; i++) {
                m.aload(2); // args array
                m.ldc(i);
                m.aaload(); // i'th argument
            }

            // invoke specific arity version
            m.invokevirtual(name, "invoke", sig(Object.class, params(Object.class, Object.class, paramTypes.length)));

            // return result
            m.areturn();
            m.end();
        }

        // constructor
        m = new SkinnyMethodAdapter(cv, ACC_PUBLIC, "<init>", sig(void.class), null, null);
        m.start();
        m.aload(0);
        m.invokespecial(p(Handle.class), "<init>", sig(void.class));
        m.voidreturn();
        m.end();

        cv.visitEnd();

        byte[] bytes = ((ClassWriter)cv).toByteArray();

        return bytes;
    }
    private static String createHandleName(Method method) {
        Class returnType = method.getReturnType();
        Class[] paramTypes = method.getParameterTypes();
        return method.getDeclaringClass().getCanonicalName().replaceAll("\\.", "__") + "#" + method.getName() + "#" + JITCompiler.getHashForString(pretty(returnType, paramTypes));
    }
    
    public static void loadUnboxedArgument(SkinnyMethodAdapter m, int index, Class type) {
        m.aload(index); // Object arg0
        unboxAndCast(m, type);
    }
    
    public static void unboxAndCast(SkinnyMethodAdapter m, Class paramClass) {
        if (paramClass.isPrimitive()) {
            Class boxType = getBoxType(paramClass);
            m.checkcast(p(boxType));
            m.invokevirtual(p(boxType), paramClass.toString() + "Value", sig(paramClass));
        } else if (paramClass != Object.class) {
            m.checkcast(p(paramClass));
        }
    }

    private static class FakeLoader extends ClassLoader {
        public FakeLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
    };
    public static class Tool {
        public static void main(String[] args) {
            if (args.length != 2) {
                System.err.println("Usage:\n  tool <java class> <target dir>");
                System.exit(1);
            }

            String classname = args[0];
            String target = args[1];

            FakeLoader loader = new FakeLoader(Tool.class.getClassLoader());
            try {
                Class klass = loader.loadClass(classname, false);
                for (Method method : klass.getMethods()) {
                    String name = createHandleName(method);
                    byte[] bytes = createHandleBytes(method, name);
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(new File(target, name + ".class"));
                        fos.write(bytes);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    } finally {
                        try {fos.close();} catch (IOException ioe) {}
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            Method method = HandleFactory.class.getMethod("dummy", new Class[] {String.class});
            Handle handle = createHandle(null, method);
            
            String prop1 = "java.class.path";
            String prop2 = "";
            String tmp;
            Object result;
            
            for (int i = 0; i < 10; i++) {
                long time;
                
                System.out.print("reflected invocation: ");
                time = System.currentTimeMillis();
                for (int j = 0; j < 50000000; j++) {
                    result = method.invoke(null, prop1);
                    method.invoke(null, prop2);
                    tmp = prop1;
                    prop1 = prop2;
                    prop2 = tmp;
                    if (j % 10000000 == 0) {
                        System.out.println(prop2);
                    }
                }
                System.out.println(System.currentTimeMillis() - time);
                
                System.out.print("method invocation: ");
                time = System.currentTimeMillis();
                for (int j = 0; j < 50000000; j++) {
                    result = dummy(prop1);
                    dummy(prop2);
                    tmp = prop1;
                    prop1 = prop2;
                    prop2 = tmp;
                    if (j % 10000000 == 0) {
                        System.out.println(prop2);
                    }
                }
                System.out.println(System.currentTimeMillis() - time);

                System.out.print("handle invocation: ");
                time = System.currentTimeMillis();
                for (int j = 0; j < 50000000; j++) {
                    result = handle.invoke(null, prop1);
                    handle.invoke(null, prop2);
                    tmp = prop1;
                    prop1 = prop2;
                    prop2 = tmp;
                    if (j % 10000000 == 0) {
                        System.out.println(prop2);
                    }
                }
                System.out.println(System.currentTimeMillis() - time);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static String dummy(String str) {
        if (str.length() == 0) return null;
        return str;
    }
    
    public static int dummy2() {
        return 1;
    }
    
    public static Object dummy3(Object obj) {
        return obj;
    }
}
