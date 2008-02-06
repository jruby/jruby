package org.jruby.ext.posix;

import java.io.FileDescriptor;

public class LinuxPOSIX extends BaseNativePOSIX {
    private static int STAT_VERSION = 3;
    
    public LinuxPOSIX(LibC libc, POSIXHandler handler) {
        super(libc, handler);
    }

    @Override
    public FileStat allocateStat() {
        return new LinuxFileStat(this);
    }

    @Override
    public FileStat fstat(FileDescriptor fileDescriptor) {
        FileStat stat = allocateStat();
        int fd = helper.getfd(fileDescriptor);
        
        if (((LinuxLibC) libc).__fxstat(STAT_VERSION, fd, stat) < 0) handler.error(ERRORS.ENOENT, "" + fd);
        
        return stat;
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
