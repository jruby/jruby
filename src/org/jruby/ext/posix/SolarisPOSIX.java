package org.jruby.ext.posix;

public class SolarisPOSIX extends BaseNativePOSIX {
    public SolarisPOSIX(LibC libc, POSIXHandler handler) {
        super(libc, handler);
    }

    public FileStat allocateStat() {
        return new SolarisFileStat(this);
    }
}
