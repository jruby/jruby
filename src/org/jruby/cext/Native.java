/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008, 2009 Wayne Meissner
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


package org.jruby.cext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import com.kenai.jffi.Library;
import com.kenai.jffi.Platform;

/**
 * The {@link Native} class is used for interfacing with native extensions.
 * It's singleton instance is tied to the {@link Ruby} runtime in which it was
 * created and cannot be used in another runtime. The reason for this is, that
 * C extensions loaded into the process space cannot be isolated from other runtimes
 * and thus cannot be used twice within the same operating system process.
 */
final class Native {
    private static Native INSTANCE;
    private static Library shim = null; // keep a hard ref to avoid GC
    private static final String libName = "jruby-cext";
    private static final String jrubyHome = Ruby.getGlobalRuntime().getJRubyHome();

    private final Ruby runtime;

    static Native getInstance(Ruby runtime) {
        if (INSTANCE == null) {
            INSTANCE = new Native(runtime);
            INSTANCE.load(runtime);
            GC.init(INSTANCE);

        } else if (INSTANCE.runtime != runtime) {
            throw runtime.newRuntimeError("invalid runtime");
        }

        return INSTANCE;
    }

    private Native(Ruby runtime) {
        this.runtime = runtime;
    }

    /**
     * Loads and initializes the {@value #libName} native library. If it is shipped in a Jar,
     * it will be extracted to a temporary folder.
     */
    private void load(Ruby runtime) {
        if (shim != null) return;

        // Force the shim library to load into the global namespace
        shim = Library.openLibrary(System.mapLibraryName(libName), Library.NOW | Library.GLOBAL);
        if (shim == null) {
            File libFile = loadFromJrubyHome();
            if (libFile != null) { // If the jruby-cext library is not available, we die in the next condition
                shim = Library.openLibrary(libFile.getAbsolutePath(), Library.NOW | Library.GLOBAL);
            }
            if (shim == null) {
                throw new UnsatisfiedLinkError("failed to load shim library, error: " + Library.getLastError());
            }
            System.load(libFile.getAbsolutePath());
        } else {
            System.loadLibrary(libName);
        }

        // Register Qfalse, Qtrue, Qnil constants to avoid reverse lookups in native code
        GC.register(runtime.getFalse(), Handle.newHandle(runtime, runtime.getFalse(), getFalse()));
        GC.register(runtime.getTrue(), Handle.newHandle(runtime, runtime.getTrue(), getTrue()));
        GC.register(runtime.getNil(), Handle.newHandle(runtime, runtime.getNil(), getNil()));

        initNative(runtime);
    }

    /**
     * Tries loading the {@value #libName} library from the classpath, the JRuby Jar or the
     * jruby home in the file-system.
     */
    private File loadFromJrubyHome() {
        URL fileUrl = Native.class.getResource(getCextLibraryPath());
        if (fileUrl == null) {
            // the file is not in the classpath. use full path
            return new File(jrubyHome + getCextLibraryPath());
        } else if (fileUrl.getProtocol().equals("file")) {
            try {
                return new File(fileUrl.toURI());
            } catch (URISyntaxException e) {
                return new File(fileUrl.getPath());
            }
        } else if (fileUrl.getProtocol().equals("jar")) {
            return loadFromJar();
        }
        return null;
    }

    /**
     * Copies the {@value #libName} library to a temporary file to allow the operating
     * system routines to load it into the process space.
     */
    private File loadFromJar() {
        InputStream is = getCextLibraryStream();
        File dstFile = null;
        FileOutputStream os = null;

        try {
            dstFile = File.createTempFile(libName, null);
            dstFile.deleteOnExit();
            os = new FileOutputStream(dstFile);
            ReadableByteChannel srcChannel = Channels.newChannel(is);

            for (long pos = 0; is.available() > 0; ) {
                pos += os.getChannel().transferFrom(srcChannel, pos, Math.max(4096, is.available()));
            }
        } catch (IOException ex) {
            throw new UnsatisfiedLinkError(ex.getMessage());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                is.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return dstFile;
    }

    /**
     * Gets an <tt>InputStream</tt> representing the stub library image stored in
     * the jar file.
     *
     * @return A new <tt>InputStream</tt>
     */
    private static final InputStream getCextLibraryStream() {
        String path = getCextLibraryPath();
        InputStream is = Native.class.getResourceAsStream(path);

        if (is == null) {
            throw new UnsatisfiedLinkError("Could not locate jruby-cext (" + path + ") in jar file");
        }

        return is;
    }

    /**
     * Gets the path within the jar file of the jruby-cext native library.
     *
     * @return The path in the jar file.
     */
    private static final String getCextLibraryPath() {
        String prefix = Platform.getPlatform().getName() + "/";
        if (jrubyHome.startsWith("file:")) {
            // jrubyHome is in Jar file
            prefix = jrubyHome.substring(jrubyHome.indexOf("jar!") + "jar!".length()) + prefix;
        } else {
            // jrubyHome is in src tree
            prefix = "/lib/native/" + prefix;
        }
        return prefix + System.mapLibraryName(libName);
    }

    /**
     * General method to execute a C method with the given arity and arguments. Slow, general path.
     */
    public final IRubyObject callMethod(ThreadContext ctx, long fn, IRubyObject recv, int arity, IRubyObject[] args) {
        long[] largs = new long[args.length];
        for (int i = 0; i < largs.length; ++i) {
            largs[i] = Handle.nativeHandle(args[i]);
        }
        return callMethod(ctx, fn, Handle.nativeHandle(recv), arity, largs);
    }

    public final native long callInit(ThreadContext ctx, long init);
    private final native void initNative(Ruby runtime);

    final native IRubyObject callMethod(ThreadContext ctx, long fn, long recv, int arity, long[] args);
    final native IRubyObject callMethod0(long fn, long recv);
    final native IRubyObject callMethod1(long fn, long recv, long arg0);
    final native IRubyObject callMethod2(long fn, long recv, long arg0, long arg1);
    final native IRubyObject callMethod3(long fn, long recv, long arg0, long arg1, long arg2);

    final native long callFunction(long fn, long data);
    final native IRubyObject callProcMethod(long fn, long args_ary);

    static native void freeHandle(long handle);
    final native long newHandle(IRubyObject obj, int type);
    final native long newFixnumHandle(IRubyObject obj, long value);
    final native long newFloatHandle(IRubyObject obj, double value);
    final native long newIOHandle(IRubyObject obj, int fileno, int i);

    final native void gc();
    final native long getNil();
    final native long getTrue();
    final native long getFalse();
}
