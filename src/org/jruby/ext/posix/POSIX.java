package org.jruby.ext.posix;


public interface POSIX {
    // When we use JNA-3 we can get errno, Then these values should match proper machine values.
    // If by chance these are not the same values on all systems we will have to have concrete
    // impls provide these values and stop using an enum.
    public enum ERRORS { ENOENT, ENOTDIR, ENAMETOOLONG, EACCESS, ELOOP, EFAULT, EIO, EBADF };

    public int chmod(String filename, int mode);
    public int chown(String filename, int user, int group);
    public int getegid();
    public int setegid(int egid);
    public int geteuid();
    public int seteuid(int euid);
    public int getgid();
    public int setgid(int gid);
    public int getpgid();
    public int getpgid(int pid);
    public int setpgid(int pid, int pgid);
    public int getpgrp();
    public int setpgrp(int pid, int pgrp);
    public int getppid();
    public int getpid();
    public int getuid();
    public int setsid();
    public int setuid(int uid);
    public int kill(int pid, int signal);
    public int lchmod(String filename, int mode);
    public int lchown(String filename, int user, int group);
    public int link(String oldpath,String newpath);
    public FileStat lstat(String path);
    public int mkdir(String path, int mode);
    public FileStat stat(String path);
    public int symlink(String oldpath,String newpath);
    public String readlink(String path);
    public int umask(int mask);
    public int fork();
    public int waitpid(int pid, int[] status, int flags);
    public int wait(int[] status);
    public int getpriority(int which, int who);
    public int setpriority(int which, int who, int prio);
}
