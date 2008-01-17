package org.jruby.ext.posix;

public class MacOSPOSIX extends BaseNativePOSIX {
    public MacOSPOSIX(LibC libc, POSIXHandler handler) {
        super(libc, handler);
    }

    public FileStat allocateStat() {
        return new MacOSFileStat(this);
    }
}
