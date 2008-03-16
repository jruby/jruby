package org.jruby.ext.posix;

public class SolarisPOSIX extends BaseNativePOSIX {
    public SolarisPOSIX(String libraryName, LibC libc, POSIXHandler handler) {
        super(libraryName, libc, handler);
    }

    public FileStat allocateStat() {
        return new SolarisFileStat(this);
    }
    
    @Override
    public int lchmod(String filename, int mode) {
        handler.unimplementedError("lchmod");
        
        return -1;
    }    
}
