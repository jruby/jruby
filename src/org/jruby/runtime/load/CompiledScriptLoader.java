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
import org.jruby.util.ClassCache.OneShotClassLoader;
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
            in = new BufferedInputStream(inStream, 8192);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8196];
            int read = 0;
            while ((read = in.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            buf = baos.toByteArray();
            JRubyClassLoader jcl = runtime.getJRubyClassLoader();
            OneShotClassLoader oscl = new OneShotClassLoader(jcl);
            
            ClassReader cr = new ClassReader(buf);
            String className = cr.getClassName().replace('/', '.');

            Class clazz = clazz = oscl.defineClass(className, buf);

            // if it's a compiled JRuby script, instantiate and run it
            if (Script.class.isAssignableFrom(clazz)) {
                return (Script)clazz.newInstance();
            } else {
                throw runtime.newLoadError("use `java_import' to load normal Java classes");
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
    }
}
