/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.RubyFile;
import org.jruby.ir.IRScope;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JRubyFile;
import org.jruby.util.OneShotClassLoader;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.jruby.RubyFile.canonicalize;
import static org.jruby.util.JRubyFile.normalizeSeps;

/**
 * Load serialized IR from the .class file requested.
 */
public class CompiledScriptLoader {
    public static IRScope loadScriptFromFile(Ruby runtime, InputStream inStream, File resourcePath, String resourceName, boolean isAbsolute) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(inStream, 8192);
            String name = normalizeSeps(resourceName);
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

            Class clazz = oscl.defineClass(className, buf);

            File path = resourcePath;

            if(path != null && !isAbsolute) {
                // Note: We use RubyFile's canonicalize rather than Java's,
                // because Java's will follow symlinks and result in __FILE__
                // being set to the target of the symlink rather than the
                // filename provided.
                name = normalizeSeps(canonicalize(path.getPath()));
            }

            try {
                Method method = clazz.getMethod("loadIR", Ruby.class, String.class);
                return (IRScope)method.invoke(null, runtime, name);
            } catch (Exception e) {
                e.printStackTrace();
                // fall through
            }

            throw runtime.newLoadError("use `java_import' to load normal Java classes: "+className);
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
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
