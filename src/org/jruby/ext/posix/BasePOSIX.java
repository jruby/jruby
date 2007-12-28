package org.jruby.ext.posix;

public abstract class BasePOSIX implements POSIX {
    protected LibC libc;
    protected POSIXHandler handler;
    
    public BasePOSIX(LibC libc, POSIXHandler handler) {
        this.libc = libc;
        this.handler = handler;
    }

    public int chmod(String filename, int mode) {
        return libc.chmod(filename, mode);
    }

    public int chown(String filename, int user, int group) {
        return libc.chown(filename, user, group);
    }

    public int getegid() {
        return libc.getegid();
    }

    public int geteuid() {
        return libc.geteuid();
    }

    public int getgid() {
        return libc.getgid();
    }

    public int getpgid() {
        return libc.getpgid();
    }

    public int getpgrp() {
        return libc.getpgrp();
    }

    public int getpid() {
        return libc.getpid();
    }

    public int getppid() {
        return libc.getppid();
    }

    public int getuid() {
        return libc.getuid();
    }

    public int kill(int pid, int signal) {
        return libc.kill(pid, signal);
    }

    public int lchmod(String filename, int mode) {
        return libc.lchmod(filename, mode);
    }

    public int lchown(String filename, int user, int group) {
        return libc.lchown(filename, user, group);
    }

    public int link(String oldpath, String newpath) {
        return libc.link(oldpath, newpath);
    }
    
    public abstract FileStat allocateStat();

    public FileStat lstat(String path) {
        FileStat stat = allocateStat();

        if (libc.lstat(path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }

    public FileStat stat(String path) {
        FileStat stat = allocateStat(); 

        if (libc.stat(path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }

    public int symlink(String oldpath, String newpath) {
        return libc.symlink(oldpath, newpath);
    }
    
    public int umask(int mask) {
        return libc.umask(mask);
    }
}
