/*
 * TestHelper.java - No description
 * Created on 15. March 2002, 9:00
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Helper class, used for testing calls to java from ruby code.
 *
 * @author  Benoit Cerrina
 * @version $Revision$
 **/
public class TestHelper {
	private String privateField = "pfValue";
    public String localVariable1;

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
        public String dispatchObject(Object iObject)
        {
        	if (iObject == null) {
				return null;
			}
			return iObject.toString();
        }
    }

    
    public static Class loadAlternateClass() throws ClassNotFoundException {
        AlternateLoader loader = new AlternateLoader();
        Class klass = loader.loadClass("org.jruby.util.TestHelper");
        return klass;
    }
    
    /**
     * Used by JVM bytecode compiler tests to run compiled code
     */
    public static IRubyObject loadAndCall(IRubyObject self, String name, byte[] javaClass, String methodName)
            throws Throwable
    {
        Loader loader = new Loader();
        Class c = loader.loadClass(name, javaClass);
        Method method = c.getMethod(methodName, new Class[] { Ruby.class, IRubyObject.class });
        Ruby runtime = self.getRuntime();
        try {

            runtime.getCurrentContext().pushClass(self.getType());
            return (IRubyObject) method.invoke(null, new Object[] { runtime, self });
        } catch (InvocationTargetException e) {
            throw unrollException(e);
        } finally {
            runtime.getCurrentContext().popClass();
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
            if (name.equals("org.jruby.util.TestHelper"))
                return findModClass(name);
            return super.loadClass(name);
        }
        private byte[] loadClassBytes(String name) throws ClassNotFoundException {
            FileInputStream stream = null;
            try {
                String fileName = name.replaceAll("\\.", "/");
                fileName += ".class";
                File file = new File("build/classes/jruby", fileName);
                byte[] bytes = new byte[(int) file.length()];
                stream = new FileInputStream(file);
                stream.read(bytes);
                return bytes;
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

}
