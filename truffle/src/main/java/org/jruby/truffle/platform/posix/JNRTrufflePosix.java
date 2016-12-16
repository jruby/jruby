/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.posix;

import com.kenai.jffi.Platform;
import com.kenai.jffi.Platform.OS;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.Signal;
import jnr.constants.platform.Sysconf;
import jnr.ffi.Pointer;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.Passwd;
import jnr.posix.SignalHandler;
import jnr.posix.SpawnFileAction;
import jnr.posix.Times;
import org.jruby.truffle.core.CoreLibrary;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.util.Collection;

public class JNRTrufflePosix implements TrufflePosix {

    private final POSIX posix;

    public JNRTrufflePosix(POSIX posix) {
        this.posix = posix;
    }

    protected POSIX getPosix() {
        return posix;
    }

    @TruffleBoundary
    @Override
    public byte[] crypt(byte[] key, byte[] salt) {
        return posix.crypt(key, salt);
    }

    @TruffleBoundary
    @Override
    public FileStat allocateStat() {
        return posix.allocateStat();
    }

    @TruffleBoundary
    @Override
    public int chmod(String filename, int mode) {
        return posix.chmod(filename, mode);
    }

    @TruffleBoundary
    @Override
    public int fchmod(int fd, int mode) {
        return posix.fchmod(fd, mode);
    }

    @TruffleBoundary
    @Override
    public int chown(String filename, int user, int group) {
        return posix.chown(filename, user, group);
    }

    @TruffleBoundary
    @Override
    public int fchown(int fd, int user, int group) {
        return posix.fchown(fd, user, group);
    }

    @TruffleBoundary
    @Override
    public int exec(String path, String... argv) {
        return posix.execv(path, argv);
    }

    @TruffleBoundary
    @Override
    public int exec(String path, String[] argv, String[] envp) {
        return posix.execve(path, argv, envp);
    }

    @TruffleBoundary
    @Override
    public int fork() {
        return posix.fork();
    }

    @TruffleBoundary
    @Override
    public int fstat(int fd, FileStat stat) {
        return posix.fstat(fd, stat);
    }

    @TruffleBoundary
    @Override
    public String getenv(String envName) {
        return posix.getenv(envName);
    }

    @TruffleBoundary
    @Override
    public int getegid() {
        return posix.getegid();
    }

    @TruffleBoundary
    @Override
    public int geteuid() {
        return posix.geteuid();
    }

    @TruffleBoundary
    @Override
    public int seteuid(int euid) {
        return posix.seteuid(euid);
    }

    @TruffleBoundary
    @Override
    public int getgid() {
        return posix.getgid();
    }

    @TruffleBoundary
    @Override
    public int getpgid(int pid) {
        return posix.getpgid(pid);
    }

    @TruffleBoundary
    @Override
    public int setpgid(int pid, int pgid) {
        return posix.setpgid(pid, pgid);
    }

    @TruffleBoundary
    @Override
    public int getpgrp() {
        return posix.getpgrp();
    }

    @TruffleBoundary
    @Override
    public int getpid() {
        return posix.getpid();
    }

    @TruffleBoundary
    @Override
    public int getppid() {
        return posix.getppid();
    }

    @TruffleBoundary
    @Override
    public int getpriority(int which, int who) {
        // getpriority can return -1 so errno has to be cleared.
        // it should be done as close as possible to the syscall
        // as JVM classloading could change errno.
        posix.errno(0);
        return posix.getpriority(which, who);
    }

    @TruffleBoundary
    @Override
    public Passwd getpwnam(String which) {
        return posix.getpwnam(which);
    }

    @TruffleBoundary
    @Override
    public int getuid() {
        return posix.getuid();
    }

    @TruffleBoundary
    @Override
    public int getrlimit(int resource, Pointer rlim) {
        return posix.getrlimit(resource, rlim);
    }

    @TruffleBoundary
    @Override
    public int setrlimit(int resource, Pointer rlim) {
        return posix.setrlimit(resource, rlim);
    }

    @TruffleBoundary
    @Override
    public boolean isatty(FileDescriptor descriptor) {
        return posix.isatty(descriptor);
    }

    @TruffleBoundary
    @Override
    public int kill(int pid, int signal) {
        return posix.kill(pid, signal);
    }

    @TruffleBoundary
    @Override
    public int kill(long pid, int signal) {
        return posix.kill(pid, signal);
    }

    @TruffleBoundary
    @Override
    public SignalHandler signal(Signal sig, SignalHandler handler) {
        return posix.signal(sig, handler);
    }

    @TruffleBoundary
    @Override
    public int lchmod(String filename, int mode) {
        return posix.lchmod(filename, mode);
    }

    @TruffleBoundary
    @Override
    public int link(String oldpath, String newpath) {
        return posix.link(oldpath, newpath);
    }

    @TruffleBoundary
    public FileStat lstat(String path) {
        return posix.lstat(path);
    }

    @TruffleBoundary
    @Override
    public int lstat(String path, FileStat stat) {
        return posix.lstat(path, stat);
    }

    @TruffleBoundary
    @Override
    public int mkdir(String path, int mode) {
        return posix.mkdir(path, mode);
    }

    @TruffleBoundary
    @Override
    public int readlink(CharSequence path, Pointer bufPtr, int bufsize) {
        return posix.readlink(path, bufPtr, bufsize);
    }

    @TruffleBoundary
    @Override
    public int rmdir(String path) {
        return posix.rmdir(path);
    }

    @TruffleBoundary
    @Override
    public int setenv(String envName, String envValue, int overwrite) {
        return posix.setenv(envName, envValue, overwrite);
    }

    @TruffleBoundary
    @Override
    public int setsid() {
        return posix.setsid();
    }

    @TruffleBoundary
    @Override
    public int setgid(int gid) {
        return posix.setgid(gid);
    }

    @TruffleBoundary
    @Override
    public int setpriority(int which, int who, int prio) {
        return posix.setpriority(which, who, prio);
    }

    @TruffleBoundary
    @Override
    public int setuid(int uid) {
        return posix.setuid(uid);
    }

    @TruffleBoundary
    @Override
    public FileStat stat(String path) {
        return posix.stat(path);
    }

    @TruffleBoundary
    @Override
    public int stat(String path, FileStat stat) {
        return posix.stat(path, stat);
    }

    @TruffleBoundary
    @Override
    public int symlink(String oldpath, String newpath) {
        return posix.symlink(oldpath, newpath);
    }

    @TruffleBoundary
    @Override
    public int umask(int mask) {
        return posix.umask(mask);
    }

    @TruffleBoundary
    @Override
    public int unsetenv(String envName) {
        return posix.unsetenv(envName);
    }

    @TruffleBoundary
    @Override
    public int utimes(String path, Pointer times) {
        return posix.utimes(path, times);
    }

    @TruffleBoundary
    @Override
    public int waitpid(int pid, int[] status, int flags) {
        return posix.waitpid(pid, status, flags);
    }

    @TruffleBoundary
    @Override
    public int waitpid(long pid, int[] status, int flags) {
        return posix.waitpid(pid, status, flags);
    }

    @TruffleBoundary
    @Override
    public int wait(int[] status) {
        return posix.wait(status);
    }

    @TruffleBoundary
    @Override
    public int errno() {
        return posix.errno();
    }

    @TruffleBoundary
    @Override
    public void errno(int value) {
        posix.errno(value);
    }

    @TruffleBoundary
    @Override
    public int chdir(String path) {
        return posix.chdir(path);
    }

    @TruffleBoundary
    @Override
    public long sysconf(Sysconf name) {
        return posix.sysconf(name);
    }

    @TruffleBoundary
    @Override
    public Times times() {
        return posix.times();
    }

    @TruffleBoundary
    @Override
    public int posix_spawnp(String path, Collection<? extends SpawnFileAction> fileActions, Collection<? extends CharSequence> argv, Collection<? extends CharSequence> envp) {
        final long pid = posix.posix_spawnp(path, fileActions, argv, envp);
        // posix_spawnp() is declared as int return value, but jnr-posix declares as long.
        if (Platform.getPlatform().getOS() == OS.SOLARIS) {
            // Solaris/SPARCv9 has the int value in the wrong half.
            // Due to big endian, we need to take the other half.
            return (int) (pid >> 32);
        } else {
            return CoreLibrary.long2int(pid);
        }
    }

    @TruffleBoundary
    @Override
    public int flock(int fd, int operation) {
        return posix.flock(fd, operation);
    }

    @TruffleBoundary
    @Override
    public int dup(int fd) {
        return posix.dup(fd);
    }

    @TruffleBoundary
    @Override
    public int dup2(int oldFd, int newFd) {
        return posix.dup2(oldFd, newFd);
    }

    @TruffleBoundary
    @Override
    public int fcntlInt(int fd, Fcntl fcntlConst, int arg) {
        return posix.fcntlInt(fd, fcntlConst, arg);
    }

    @TruffleBoundary
    @Override
    public int fcntl(int fd, Fcntl fcntlConst) {
        return posix.fcntl(fd, fcntlConst);
    }

    @TruffleBoundary
    @Override
    public int access(CharSequence path, int amode) {
        return posix.access(path, amode);
    }

    @TruffleBoundary
    @Override
    public int close(int fd) {
        return posix.close(fd);
    }

    @TruffleBoundary
    @Override
    public int unlink(CharSequence path) {
        return posix.unlink(path);
    }

    @TruffleBoundary
    @Override
    public int open(CharSequence path, int flags, int perm) {
        return posix.open(path, flags, perm);
    }

    @TruffleBoundary
    @Override
    public int write(int fd, byte[] buf, int n) {
        return posix.write(fd, buf, n);
    }

    @TruffleBoundary
    @Override
    public int read(int fd, byte[] buf, int n) {
        return posix.read(fd, buf, n);
    }

    @TruffleBoundary
    @Override
    public int write(int fd, ByteBuffer buf, int n) {
        return posix.write(fd, buf, n);
    }

    @TruffleBoundary
    @Override
    public int read(int fd, ByteBuffer buf, int n) {
        return posix.read(fd, buf, n);
    }

    @TruffleBoundary
    @Override
    public int lseek(int fd, long offset, int whence) {
        return posix.lseek(fd, offset, whence);
    }

    @TruffleBoundary
    @Override
    public int pipe(int[] fds) {
        return posix.pipe(fds);
    }

    @TruffleBoundary
    @Override
    public int truncate(CharSequence path, long length) {
        return posix.truncate(path, length);
    }

    @TruffleBoundary
    @Override
    public int ftruncate(int fd, long offset) {
        return posix.ftruncate(fd, offset);
    }

    @TruffleBoundary
    @Override
    public int rename(CharSequence oldName, CharSequence newName) {
        return posix.rename(oldName, newName);
    }

    @TruffleBoundary
    @Override
    public String getcwd() {
        return posix.getcwd();
    }

    @TruffleBoundary
    @Override
    public int fsync(int fd) {
        return posix.fsync(fd);
    }

    @TruffleBoundary
    @Override
    public int isatty(int fd) {
        return posix.libc().isatty(fd);
    }

    @TruffleBoundary
    @Override
    public int mkfifo(String path, int mode) {
        return posix.mkfifo(path, mode);
    }

    @TruffleBoundary
    @Override
    public long[] getgroups() {
        return posix.getgroups();
    }

    @TruffleBoundary
    @Override
    public String nl_langinfo(int item) {
        return posix.nl_langinfo(item);
    }
}
