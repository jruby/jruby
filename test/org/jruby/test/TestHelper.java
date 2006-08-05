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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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
package org.jruby.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.IRuby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Helper class, used for testing calls to java from ruby code.
 **/
public class TestHelper {
    private String privateField = "pfValue";
    public String localVariable1;

    // Function not used...but it gets rid of unused warnings in Eclipse (we do call those methods
    // from Ruby so they are not really unused).
    public static void removeWarningsFromEclipse() {
        TestHelper helper = new TestHelper("A");
        helper.privateMethod();
        TestHelper.staticPrivateMethod();
    }

    private TestHelper(String x) {
        privateField = x;
    }

    public TestHelper() {
    }

    private String privateMethod() {
        return privateField;
    }

    private static String staticPrivateMethod() {
        return "staticPM";
    }

    public String identityTest() {
        return "Original";
    }

    /**
     * used to test Java Arrays in Ruby.
     *  while we don't yet have a way to create them this can be used to test basic
     *  array functionalities
     */
    public static String[] createArray(int i) {
        return new String[i];
    }

    /**
     * used to test native exception handling.
     **/
    public static void throwException() {
        throw new RuntimeException("testException");
    }

    /**
     * @return object used to test casting
     */
    public static SomeInterface getInterfacedInstance() {
        return new SomeImplementation();
    }

    public static Object getLooslyCastedInstance() {
        return new SomeImplementation();
    }

    public static Object getNull() {
        return null;
    }

    public static interface SomeInterface {
        String doStuff();
        String dispatchObject(Object iObject);
    }

    private static class SomeImplementation implements SomeInterface {
        public String doStuff() {
            return "stuff done";
        }
        
        public String dispatchObject(Object iObject) {
            return iObject == null ? null : iObject.toString();
        }
    }


    public static Class loadAlternateClass() throws ClassNotFoundException {
        AlternateLoader loader = new AlternateLoader();
        Class klass = loader.loadClass("org.jruby.test.TestHelper");
        return klass;
    }

    /**
     * Used by JVM bytecode compiler tests to run compiled code
     */
    public static IRubyObject loadAndCall(IRubyObject self, String name, byte[] javaClass, String methodName)
            throws Throwable {
        Loader loader = new Loader();
        Class c = loader.loadClass(name, javaClass);
        Method method = c.getMethod(methodName, new Class[] { IRuby.class, IRubyObject.class });
        IRuby runtime = self.getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        
        tc.pushRubyClass(self.getType());

        try {
            return (IRubyObject) method.invoke(null, new Object[] { runtime, self });
        } catch (InvocationTargetException e) {
            throw unrollException(e);
        } finally {
            tc.popRubyClass();
        }
    }

    private static Throwable unrollException(InvocationTargetException e) {
        while (e.getCause() instanceof InvocationTargetException) {
            e = (InvocationTargetException) e.getCause();
        }
        return e.getCause();
    }

    public static String getClassName(Class klass) {
        return klass.getName();
    }

    public static void throwTestHelperException() {
        throw new TestHelperException();
    }

    private static class Loader extends ClassLoader {

        public Class loadClass(String name, byte[] javaClass) {
            Class cl = defineClass(name,
                                   javaClass,
                                   0,
                                   javaClass.length);
            resolveClass(cl);
            return cl;
        }

    }
    private static class AlternateLoader extends ClassLoader {

        protected Class findModClass(String name) throws ClassNotFoundException {
           byte[] classBytes = loadClassBytes(name);
           replace(classBytes, "Original", "ABCDEFGH");
           return defineClass(name, classBytes, 0, classBytes.length);
        }
        private void replace(byte[] classBytes, String find, String replaceWith) {
            byte[] findBytes = find.getBytes();
            byte[] replaceBytes = replaceWith.getBytes();
            for (int i=0; i<classBytes.length; i++) {
                boolean match = true;
                for (int j=0; j<findBytes.length; j++) {
                    if (classBytes[i+j] != findBytes[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    for (int j=0; j<findBytes.length; j++)
                        classBytes[i+j] = replaceBytes[j];
                    return;
                }
            }
        }
        public Class loadClass(String name) throws ClassNotFoundException {
            if (name.equals("org.jruby.test.TestHelper"))
                return findModClass(name);
            return super.loadClass(name);
        }
        private byte[] loadClassBytes(String name) throws ClassNotFoundException {
            InputStream stream = null;
            try {
                String fileName = name.replaceAll("\\.", "/");
                fileName += ".class";
                byte[] buf = new byte[1024];
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int bytesRead = 0;
                stream = getClass().getResourceAsStream("/" + fileName);
                while ((bytesRead = stream.read(buf)) != -1) {
                    bytes.write(buf, 0, bytesRead);
                }
                return bytes.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                throw new ClassNotFoundException(e.getMessage(),e);
            } finally {
                if (stream != null)
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
            }
        }
    }

    private static class TestHelperException extends RuntimeException {
        private static final long serialVersionUID = 3649034127816624007L;
    }
}
