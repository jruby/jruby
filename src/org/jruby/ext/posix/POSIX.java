package org.jruby.ext.posix;

import java.io.FileDescriptor;
import java.io.IOException;


public interface POSIX {
    // When we use JNA-3 we can get errno, Then these values should match proper machine values.
    // If by chance these are not the same values on all systems we will have to have concrete
    // impls provide these values and stop using an enum.
    public enum ERRORS { ENOENT, ENOTDIR, ENAMETOOLONG, EACCESS, ELOOP, EFAULT, EIO, EBADF };

    public int chmod(String filename, int mode);
    public int chown(String filename, int user, int group);
    public int fork();
    public FileStat fstat(FileDescriptor descriptor);
    public int getegid();
    public int geteuid();
    public int seteuid(int euid);
    public int getgid();
    public String getlogin();
    public int getpgid();
    public int getpgid(int pid);
    public int getpgrp();
    public int getpid();
    public int getppid();
    public int getpriority(int which, int who);
    public Passwd getpwent();
    public Passwd getpwuid(int which);
    public Passwd getpwnam(String which);
    public Group getgrgid(int which);
    public Group getgrnam(String which);
    public Group getgrent();
    public int endgrent();
    public int setgrent();
    public int endpwent();
    public int setpwent();
    public int getuid();
    public boolean isatty(FileDescriptor descriptor);
    public int kill(int pid, int signal);
    public int lchmod(String filename, int mode);
    public int lchown(String filename, int user, int group);
    public int link(String oldpath,String newpath);
    public FileStat lstat(String path);
    public int mkdir(String path, int mode);
    public String readlink(String path) throws IOException;
    public int setsid();
    public int setgid(int gid);
    public int setegid(int egid);
    public int setpgid(int pid, int pgid);
    public int setpgrp(int pid, int pgrp);
    public int setpriority(int which, int who, int prio);
    public int setuid(int uid);
    public FileStat stat(String path);
    public int symlink(String oldpath,String newpath);
    public int umask(int mask);
    public int waitpid(int pid, int[] status, int flags);
    public int wait(int[] status);
}
