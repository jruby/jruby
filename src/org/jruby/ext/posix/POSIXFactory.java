package org.jruby.ext.posix;

import java.util.HashMap;
import org.jruby.ext.posix.util.Platform;
import com.sun.jna.Native;

public class POSIXFactory {
    static final String LIBC = "c";
    static LibC libc = null;
    
    public static POSIX getPOSIX(POSIXHandler handler, boolean useNativePOSIX) {
        POSIX posix = null;
        
        // No 64 bit structures defined yet.
        if (useNativePOSIX && Platform.IS_32_BIT) {
            try {
                if (Platform.IS_MAC) {
                    posix = loadMacOSPOSIX(handler);
                } else if (Platform.IS_LINUX) {
                    posix = loadLinuxPOSIX(handler);
                } else if (Platform.IS_WINDOWS) {
                    posix = loadWindowsPOSIX(handler);
                } else if (Platform.IS_SOLARIS) {
                    posix = loadSolarisPOSIX(handler);
                }
                
                // ENEBO: Should printing be done through a handler+log method?
                if (handler.isVerbose()) {
                    if (posix != null) {
                        System.err.println("Successfully loaded native POSIX impl.");
                    } else {
                        System.err.println("Failed to load native POSIX impl; falling back on Java impl. Unsupported OS.");
                    }
                }
            } catch (Throwable t) {
                if (handler.isVerbose()) {
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
    
    public static POSIX loadLinuxPOSIX(POSIXHandler handler) {
        return new LinuxPOSIX(LIBC, loadLibC(LIBC, LinuxLibC.class, new HashMap<Object, Object>()), handler);        
    }
    
    public static POSIX loadMacOSPOSIX(POSIXHandler handler) {
        return new MacOSPOSIX(LIBC, loadLibC(LIBC, LibC.class, new HashMap<Object, Object>()), handler);        
    }
    
    public static POSIX loadSolarisPOSIX(POSIXHandler handler) {
        return new SolarisPOSIX(LIBC, loadLibC(LIBC, LibC.class, new HashMap<Object, Object>()), handler);        
    }
    
    public static POSIX loadWindowsPOSIX(POSIXHandler handler) {
        String name = "msvcrt";

        HashMap<Object, Object> options = new HashMap<Object, Object>();
        options.put(com.sun.jna.Library.OPTION_FUNCTION_MAPPER, new WindowsLibCFunctionMapper());
        
        return new WindowsPOSIX(name, loadLibC(name, LibC.class, options), handler);        
    }
    
    public static LibC loadLibC(String libraryName, Class<?> libCClass, HashMap<Object, Object> options) {
        if (libc != null) return libc;
        
        libc = (LibC) Native.loadLibrary(libraryName, libCClass, options);
        
        return libc;
    }
}
