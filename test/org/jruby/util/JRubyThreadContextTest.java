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
 * Copyright (C) 2007, 2008 Robert Egglestone <robert@cs.auckland.ac.nz>
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
package org.jruby.util;

import junit.framework.TestCase;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.Ruby;
import java.lang.reflect.Method;
import org.jruby.RubyInstanceConfig;

/**
 * Test that the thread context classloader can be changed.
 *
 * There are two types of tests: runtime, and request.
 * + runtime tests check that the context may change between different Ruby instances.
 * + request tests check that the context may change between requests on the same Ruby instance.
 *
 * Currently per per request tests fail, as implementing a fix for this is much more difficult.
 */
public class JRubyThreadContextTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // start off with a neutral parent
        ClassLoader parent = JRubyThreadContextTest.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(parent);
    }

    /**
     * Check that the thread context can be different between Ruby instances
     */
    public void testThreadContextPerRuntime() {
        Ruby ruby = Ruby.newInstance();
        try {
            ruby.getJRubyClassLoader().loadClass("org.jruby.GiveMeAString");
            fail("org.jruby.GiveMeAString is on the classpath!?");
        } catch (ClassNotFoundException e) {
            // expected
        }

        // change the thread context to include the class
        SimpleClassLoader simpleLoader = new SimpleClassLoader();
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setLoader(simpleLoader);
        ruby = Ruby.newInstance(config);

        try {
            ruby.getJRubyClassLoader().loadClass("org.jruby.GiveMeAString");
        } catch (ClassNotFoundException e) {
            fail("Classloader change unnoticed");
        }
    }

    /**
     * Checks if a class can be redefined between different Ruby instances.
     */
    public void testRedefineClassPerRuntime() throws Exception {
        ClassLoader v1 = new VersionedClassLoader("First");
        ClassLoader v2 = new VersionedClassLoader("Second");

        RubyInstanceConfig config1 = new RubyInstanceConfig();
        config1.setLoader(v1);
        RubyInstanceConfig config2 = new RubyInstanceConfig();
        config2.setLoader(v2);
        Ruby ruby1 = Ruby.newInstance(config1);
        Ruby ruby2 = Ruby.newInstance(config2);

        assertEquals("First", getMessage(ruby1));
        assertEquals("Second", getMessage(ruby2));
        assertEquals("First", getMessage(ruby1));
    }

    private String getMessage(Ruby ruby) throws Exception {
        JRubyClassLoader loader = ruby.getJRubyClassLoader();
        Class hello = loader.loadClass("Hello");
        Method getMessage = hello.getMethod("getMessage", new Class[0]);
        return (String)getMessage.invoke(null, new Object[0]);
    }

    private static class SimpleClassLoader extends ClassLoader {

        public SimpleClassLoader() {
        }

        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("org.jruby.GiveMeAString")) {
                return String.class;
            }
            return super.loadClass(name, resolve);
        }

    }

    private static class VersionedClassLoader extends ClassLoader implements Opcodes {

        private String message;

        public VersionedClassLoader(String message) {
            this.message = message;
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            if (name.equals("Hello")) {
                byte[] classBytes = createClass(message);
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            return super.findClass(name);
        }

        public static byte[] createClass(String message) {
            /*
            public class Hello {
                public static String getMessage() {
                    return ".....";
                }
            }
            */
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            MethodVisitor mv;

            cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, "Hello", null, "java/lang/Object", null);

			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();

            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getMessage", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitLdcInsn(message);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }

    }

}
