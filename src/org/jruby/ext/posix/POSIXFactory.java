package org.jruby.ext.posix;

import java.util.HashMap;

import org.jruby.util.SafePropertyAccessor;

import com.sun.jna.Native;

public class POSIXFactory {
    static LibC libc = null;
    
    public static POSIX getPOSIX(POSIXHandler handler, boolean useNativePOSIX) {
        boolean thirtyTwoBit = "32".equals(SafePropertyAccessor.getProperty("sun.arch.data.model", "32")) == true;
        
        POSIX posix = null;
        
        // No 64 bit structures defined yet.
        if (useNativePOSIX && thirtyTwoBit) {
            try {
                String os = System.getProperty("os.name");
                if (os.startsWith("Mac OS") || os.startsWith("Darwin")) {
                    posix = new MacOSPOSIX(loadMacOSLibC(), handler);
                } else if (os.startsWith("Linux")) {
                    posix = new LinuxPOSIX(loadLinuxLibC(), handler);
                } else if (os.startsWith("Windows")) {
                    posix = new WindowsPOSIX(loadWindowsLibC(), handler);
                }
                
                if (SafePropertyAccessor.getBoolean("jruby.native.verbose")) {
                    if (posix != null) {
                        System.err.println("Successfully loaded native POSIX impl.");
                    } else {
                        System.err.println("Failed to load native POSIX impl; falling back on Java impl. Unsupported OS.");
                    }
                }
            } catch (Throwable t) {
                if (SafePropertyAccessor.getBoolean("jruby.native.verbose")) {
                    System.err.println("Failed to load native POSIX impl; falling back on Java impl. Stacktrace follows.");
                    t.printStackTrace();
                }
            }
        }
        
        if (posix == null) {
            posix = getJavaPOSIX(handler);
        }
        
        return posix;
    }
    
    public static POSIX getJavaPOSIX(POSIXHandler handler) {
        return new JavaPOSIX(new JavaLibC(handler), handler);
    }
    
    public static LibC loadLinuxLibC() {
        if (libc != null) return libc;
        
        libc = (LibC) Native.loadLibrary("c", LinuxLibC.class, new HashMap<Object, Object>());
        
        return libc;
    }
    
    public static LibC loadMacOSLibC() {
        if (libc != null) return libc;
        
        libc = (LibC) Native.loadLibrary("c", LibC.class, new HashMap<Object, Object>());
        
        return libc;
    }

    public static LibC loadWindowsLibC() {
        if (libc != null) return libc;
        
        HashMap<Object, Object> options = new HashMap<Object, Object>();
        options.put(com.sun.jna.Library.OPTION_FUNCTION_MAPPER, new WindowsLibCFunctionMapper());

        libc = (LibC) Native.loadLibrary("msvcrt", LibC.class, options);

        return libc;
    }
}
