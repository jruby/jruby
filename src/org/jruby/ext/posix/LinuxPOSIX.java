package org.jruby.ext.posix;

public class LinuxPOSIX extends BasePOSIX {
    private static int STAT_VERSION = 3;
    
    public LinuxPOSIX(LibC libc, POSIXHandler handler) {
        super(libc, handler);
    }

    @Override
    public FileStat allocateStat() {
        return new LinuxFileStat(this);
    }

    @Override
    public FileStat lstat(String path) {
        FileStat stat = allocateStat();

        if (((LinuxLibC) libc).__lxstat(STAT_VERSION, path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }

    @Override
    public FileStat stat(String path) {
        FileStat stat = allocateStat(); 

        if (((LinuxLibC) libc).__xstat(STAT_VERSION, path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }
}
