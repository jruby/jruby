package org.jruby.ext.posix;

import java.util.HashMap;

import org.jruby.ext.posix.util.Platform;
import org.jruby.util.SafePropertyAccessor;

import com.sun.jna.Native;

public class POSIXFactory {
    static LibC libc = null;
    
    public static POSIX getPOSIX(POSIXHandler handler, boolean useNativePOSIX) {        
        POSIX posix = null;
        
        // No 64 bit structures defined yet.
        if (useNativePOSIX && Platform.IS_32_BIT) {
            try {
                if (Platform.IS_MAC) {
                    posix = new MacOSPOSIX(loadMacOSLibC(), handler);
                } else if (Platform.IS_LINUX) {
                    posix = new LinuxPOSIX(loadLinuxLibC(), handler);
                } else if (Platform.IS_WINDOWS) {
                    posix = new WindowsPOSIX(loadWindowsLibC(), handler);
                } else if (Platform.IS_SOLARIS) {
                    posix = new SolarisPOSIX(loadSolarisLibC(), handler);
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
        return new JavaPOSIX(handler);
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
    
    public static LibC loadSolarisLibC() {
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
