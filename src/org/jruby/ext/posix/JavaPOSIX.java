package org.jruby.ext.posix;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class JavaPOSIX implements POSIX {
    POSIXHandler handler;
    JavaLibCHelper helper;

    public JavaPOSIX(POSIXHandler handler) {
        this.handler = handler;
        helper = new JavaLibCHelper(handler);
    }
    
    public FileStat allocateStat() {
        return new JavaFileStat(this, handler);
    }

    public int chmod(String filename, int mode) {
        return helper.chmod(filename, mode);
    }

    public int chown(String filename, int user, int group) {
        return helper.chown(filename, user, group);
    }
    
    public FileStat fstat(FileDescriptor descriptor) {
        handler.unimplementedError("fstat unimplemented");
        
        return null;
    }

    public int getegid() {
        return unimplementedInt("getegid");
    }
    
    public int geteuid() {
        return unimplementedInt("geteuid");
    }
    
    public int getgid() {
        return unimplementedInt("getgid");
    }
    
    public String getlogin() {
        return helper.getlogin();
    }

    public int getpgid() {
        return unimplementedInt("getpgid");
    }

    public int getpgrp() {
        return unimplementedInt("getpgrp");
    }

    public int getpid() {
        return helper.getpid();
    }

    public int getppid() {
        return unimplementedInt("getppid");
    }

    public Passwd getpwent() {
        return helper.getpwent();
    }

    public Passwd getpwuid(int which) {
        handler.unimplementedError("getpwuid unimplemented");
        return null;
    }

    public Passwd getpwnam(String which) {
        handler.unimplementedError("getpwnam unimplemented");
        return null;
    }

    public Group getgrent() {
        handler.unimplementedError("getgrent unimplemented");
        return null;
    }

    public int setpwent() {
        return helper.setpwent();
    }

    public int endpwent() {
        return helper.endpwent();
    }

    public int getuid() {
        return unimplementedInt("getuid");
    }
    
    public int fork() {
        return -1;
    }

    public boolean isatty(FileDescriptor fd) {
        return (fd == FileDescriptor.in
                || fd == FileDescriptor.out
                || fd == FileDescriptor.err);
    }

    public int kill(int pid, int signal) {
        return unimplementedInt("kill");    // FIXME: Can be implemented
    }

    public int lchmod(String filename, int mode) {
        return unimplementedInt("lchmod");    // FIXME: Can be implemented
    }

    public int lchown(String filename, int user, int group) {
        return unimplementedInt("lchown");     // FIXME: Can be implemented
    }

    public int link(String oldpath, String newpath) {
        return helper.link(oldpath, newpath);
    }

    public FileStat lstat(String path) {
        FileStat stat = allocateStat();

        if (helper.lstat(path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }

    public int mkdir(String path, int mode) {
        return helper.mkdir(path, mode);
    }

    public String readlink(String path) {
        // TODO: this should not be hardcoded to 256 bytes
        ByteBuffer buffer = ByteBuffer.allocateDirect(256);
        int result = helper.readlink(path, buffer, buffer.capacity());
        
        if (result == -1) return null;
        
        buffer.position(0);
        buffer.limit(result);
        return Charset.forName("ASCII").decode(buffer).toString();
    }

    public FileStat stat(String path) {
        FileStat stat = allocateStat(); 

        if (helper.stat(path, stat) < 0) handler.error(ERRORS.ENOENT, path);
        
        return stat;
    }

    public int symlink(String oldpath, String newpath) {
        return helper.symlink(oldpath, newpath);
    }

    public int setegid(int egid) {
        return unimplementedInt("setegid");
    }

    public int seteuid(int euid) {
        return unimplementedInt("seteuid");
    }

    public int setgid(int gid) {
        return unimplementedInt("setgid");
    }

    public int getpgid(int pid) {
        return unimplementedInt("getpgid");
    }

    public int setpgid(int pid, int pgid) {
        return unimplementedInt("setpgid");
    }

    public int setpgrp(int pid, int pgrp) {
        return unimplementedInt("setpgrp");
    }

    public int setsid() {
        return unimplementedInt("setsid");
    }

    public int setuid(int uid) {
        return unimplementedInt("setuid");
    }

    public int umask(int mask) {
        // TODO: We can possibly maintain an internal mask and try and apply it to individual
        // libc methods.  
        return 0;
    }
    
    public int wait(int[] status) {
        return unimplementedInt("wait");
    }
    
    public int waitpid(int pid, int[] status, int flags) {
        return unimplementedInt("waitpid");
    }

    public int getpriority(int which, int who) {
        return unimplementedInt("getpriority");
    }
    
    public int setpriority(int which, int who, int prio) {
        return unimplementedInt("setpriority");
    }

    private int unimplementedInt(String message) {
        handler.unimplementedError(message);
        
        return -1;
    }
}
