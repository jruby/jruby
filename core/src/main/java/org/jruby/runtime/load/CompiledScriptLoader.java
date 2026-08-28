/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.ir.IRScope;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.objectweb.asm.ClassReader;

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
        String name = getFilenameFromPathAndName(resourcePath, resourceName, isAbsolute);
        try {
            Class clazz = loadCompiledScriptFromClass(runtime, inStream);

            try {
                Method method = clazz.getMethod("loadIR", Ruby.class, String.class);
                return (IRScope)method.invoke(null, runtime, name);
            } catch (Exception e) {
                if (runtime.getDebug().isTrue()) {
                    e.printStackTrace();
                }
                throw runtime.newLoadError(name + " is not compiled Ruby; use java_import to load normal classes");
            }
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (LinkageError le) {
            if (runtime.getDebug().isTrue()) {
                le.printStackTrace();
            }
            throw runtime.newLoadError("Linkage error loading compiled script; you may need to recompile '" + name + "': " + le);
        } finally {
            try {
                inStream.close();
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }
    }

    private static Class loadCompiledScriptFromClass(Ruby runtime, InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        buf = baos.toByteArray();
        JRubyClassLoader jcl = runtime.getJRubyClassLoader();
        OneShotClassLoader oscl = new OneShotClassLoader(jcl);

        ClassReader cr = new ClassReader(buf);
        String className = cr.getClassName().replace('/', '.');

        return oscl.defineClass(className, buf);
    }

    public static String getFilenameFromPathAndName(File resourcePath, String resourceName, boolean isAbsolute) {
        // Note: We use RubyFile's canonicalize rather than Java's for relative paths because Java's will follow
        // symlinks and result in __FILE__ being set to the target of the symlink rather than the filename provided.
        return normalizeSeps(resourcePath != null && !isAbsolute ? canonicalize(resourcePath.getPath()) : resourceName);
    }
}
