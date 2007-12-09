package org.jruby.ext.posix;

public class JavaPOSIX extends BasePOSIX {
    public JavaPOSIX(LibC libc, POSIXHandler handler) {
        super(libc, handler);
    }
    
    public FileStat allocateStat() {
        return new JavaFileStat(this, handler);
    }
}
