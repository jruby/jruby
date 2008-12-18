/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.load;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jruby.Ruby;
import org.jruby.ast.executable.Script;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassReader;

/**
 *
 * @author headius
 */
public class CompiledScriptLoader {
    public static Script loadScriptFromFile(Ruby runtime, InputStream inStream, String resourceName) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(inStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8196];
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            buf = baos.toByteArray();
            JRubyClassLoader jcl = runtime.getJRubyClassLoader();
            ClassReader cr = new ClassReader(buf);
            String className = cr.getClassName().replace('/', '.');

            Class clazz = null;
            try {
                clazz = jcl.loadClass(className);
            } catch (ClassNotFoundException cnfe) {
                clazz = jcl.defineClass(className, buf);
            }

            // if it's a compiled JRuby script, instantiate and run it
            if (Script.class.isAssignableFrom(clazz)) {
                return (Script)clazz.newInstance();
            }
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InstantiationException ie) {
            if (runtime.getDebug().isTrue()) {
                ie.printStackTrace();
            }
            throw runtime.newLoadError("Error loading compiled script '" + resourceName + "': " + ie);
        } catch (IllegalAccessException iae) {
            if (runtime.getDebug().isTrue()) {
                iae.printStackTrace();
            }
            throw runtime.newLoadError("Error loading compiled script '" + resourceName + "': " + iae);
        } catch (LinkageError le) {
            if (runtime.getDebug().isTrue()) {
                le.printStackTrace();
            }
            throw runtime.newLoadError("Linkage error loading compiled script; you may need to recompile '" + resourceName + "': " + le);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }
        return null;
    }
}
