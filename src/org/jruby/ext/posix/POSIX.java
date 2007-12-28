package org.jruby.ext.posix;

public interface POSIX {
    // When we use JNA-3 we can get errno, Then these values should match proper machine values.
    // If by chance these are not the same values on all systems we will have to have concrete
    // impls provide these values and stop using an enum.
    public enum ERRORS { ENOENT, ENOTDIR, ENAMETOOLONG, EACCESS, ELOOP, EFAULT, EIO, EBADF };

    public int chmod(String filename, int mode);
    public int chown(String filename, int user, int group);
    public int getegid();
    public int geteuid();
    public int getgid();
    public int getpgid();
    public int getpgrp();
    public int getppid();
    public int getpid();
    public int getuid();
    public int kill(int pid, int signal);
    public int lchmod(String filename, int mode);
    public int lchown(String filename, int user, int group);
    public int link(String oldpath,String newpath);
    public FileStat lstat(String path);
    public FileStat stat(String path);
    public int symlink(String oldpath,String newpath);
    public int umask(int mask);
}
