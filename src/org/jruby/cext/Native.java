/*
 * Copyright (C) 2008, 2009 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.cext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.kenai.jffi.Library;
import com.kenai.jffi.Platform;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


final class Native {
    private static Native INSTANCE;
    private static Library shim = null; // keep a hard ref to avoid GC
    private static final String libName = "jruby-cext";

    private final Ruby runtime;

    static Native getInstance(Ruby runtime) {
        if (INSTANCE == null) {
            INSTANCE = new Native(runtime);
            INSTANCE.load(runtime);
        
        } else if (INSTANCE.runtime != runtime) {
            throw runtime.newRuntimeError("invalid runtime");
        }

        return INSTANCE;
    }

    private Native(Ruby runtime) {
        this.runtime = runtime;
    }

    private void load(Ruby runtime) {
        if (shim != null) return;

        // Force the shim library to load into the global namespace
        shim = Library.openLibrary(System.mapLibraryName(libName), Library.NOW | Library.GLOBAL);
        if (shim == null) {
            File libFile = loadOutsideLibraryPath();
            shim = Library.openLibrary(libFile.getAbsolutePath(), Library.NOW | Library.GLOBAL);
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
    
    private File loadOutsideLibraryPath() {
        URL fileUrl = Native.class.getResource(getCextLibraryPath());
        if (fileUrl.getProtocol().equals("jar")) {
            return loadFromJar();
        } else {
            try {
                return new File(fileUrl.toURI());
            } catch (URISyntaxException e) {
                throw new UnsatisfiedLinkError(e.getLocalizedMessage());
            }
        }
    }

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

        // On MacOS, the stub might be named .dylib or .jnilib - cater for both
        if (is == null && Platform.getPlatform().getOS() == Platform.OS.DARWIN) {
            is = Init.class.getResourceAsStream(path.replaceAll("dylib", "jnilib"));
        }
        if (is == null) {
            throw new UnsatisfiedLinkError("Could not locate jruby-cext ("
                    + path + ") in jar file");
        }

        return is;
    }

    /**
     * Gets the path within the jar file of the jruby-cext native library.
     *
     * @return The path in the jar file.
     */
    private static final String getCextLibraryPath() {
        //return "/cext/" + Platform.getPlatform().getName() + "/"+ System.mapLibraryName(libName);
        return "/cext/build/"+ System.mapLibraryName(libName);
    }

    private final native void initNative(Ruby runtime);
    
    public final native long callInit(ThreadContext ctx, long init);


    public final IRubyObject callMethod(ThreadContext ctx, long fn, IRubyObject recv, int arity, IRubyObject[] args) {
        long[] largs = new long[args.length];
        for (int i = 0; i < largs.length; ++i) {
            largs[i] = Handle.nativeHandleLocked(args[i]);
        }
        return callMethod(ctx, fn, Handle.nativeHandleLocked(recv), arity, largs);
    }

    public final native IRubyObject callMethod(ThreadContext ctx, long fn, long recv, int arity, long[] args);
    final native IRubyObject callMethod0(long fn, long recv);
    final native IRubyObject callMethod1(long fn, long recv, long arg0);
    final native IRubyObject callMethod2(long fn, long recv, long arg0, long arg1);
    final native IRubyObject callMethod3(long fn, long recv, long arg0, long arg1, long arg2);

    public final native long newHandle(IRubyObject obj);
    public final native long newFixnumHandle(IRubyObject obj, long value);
    public final native void freeHandle(long handle);
    public final native void markHandle(long handle);
    public final native void unmarkHandle(long handle);

    private final native int getNil();
    private final native int getTrue();
    private final native int getFalse();
}
