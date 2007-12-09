package org.jruby.ext.posix;

import java.util.HashMap;

import com.sun.jna.Native;

public class POSIXFactory {
    static LibC libc = null;
    
    public static POSIX getPOSIX(POSIXHandler handler, boolean useNativePOSIX) {
        boolean thirtyTwoBit = "32".equals(System.getProperty("sun.arch.data.model")) == true;
        
        // No 64 bit structures defined yet.
        if (useNativePOSIX && thirtyTwoBit) {
            try {
                String os = System.getProperty("os.name");
                if (os.startsWith("Mac OS")) {
                    return new MacOSPOSIX(loadMacOSLibC(), handler);
                } else if (os.startsWith("Linux")) {
                    return new LinuxPOSIX(loadLinuxLibC(), handler);
                } else if (os.startsWith("Windows")) {
                    return new WindowsPOSIX(loadWindowsLibC(), handler);
                }
            } catch (Exception e) {
            } // Loading error...not much to be done with it?
        }
        
        return getJavaPOSIX(handler);
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
