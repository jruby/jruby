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

import jnr.constants.platform.Fcntl;
import jnr.constants.platform.Signal;
import jnr.constants.platform.Sysconf;
import jnr.ffi.Pointer;
import jnr.posix.*;
import jnr.posix.util.ProcessMaker;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

public abstract class POSIXDelegator implements POSIX {

    private final POSIX delegateTo;

    public POSIXDelegator(POSIX delegateTo) {
        this.delegateTo = delegateTo;
    }

    @Override
    public CharSequence crypt(CharSequence key, CharSequence salt) {
        return delegateTo.crypt(key, salt);
    }

    @Override
    public byte[] crypt(byte[] key, byte[] salt) {
        return delegateTo.crypt(key, salt);
    }

    @Override
    public FileStat allocateStat() {
        return delegateTo.allocateStat();
    }

    @Override
    public int chmod(String filename, int mode) {
        return delegateTo.chmod(filename, mode);
    }

    @Override
    public int fchmod(int fd, int mode) {
        return delegateTo.fchmod(fd, mode);
    }

    @Override
    public int chown(String filename, int user, int group) {
        return delegateTo.chown(filename, user, group);
    }

    @Override
    public int fchown(int fd, int user, int group) {
        return delegateTo.fchown(fd, user, group);
    }

    @Override
    public int exec(String path, String... argv) {
        return delegateTo.exec(path, argv);
    }

    @Override
    public int exec(String path, String[] argv, String[] envp) {
        return delegateTo.exec(path, argv, envp);
    }

    @Override
    public int execv(String path, String[] argv) {
        return delegateTo.execv(path, argv);
    }

    @Override
    public int execve(String path, String[] argv, String[] envp) {
        return delegateTo.execve(path, argv, envp);
    }

    @Override
    public int fork() {
        return delegateTo.fork();
    }

    @Override
    public FileStat fstat(FileDescriptor descriptor) {
        return delegateTo.fstat(descriptor);
    }

    @Override
    public FileStat fstat(int descriptor) {
        return delegateTo.fstat(descriptor);
    }

    @Override
    public int fstat(FileDescriptor descriptor, FileStat stat) {
        return delegateTo.fstat(descriptor, stat);
    }

    @Override
    public int fstat(int fd, FileStat stat) {
        return delegateTo.fstat(fd, stat);
    }

    @Override
    public Pointer environ() {
        return delegateTo.environ();
    }

    @Override
    public String getenv(String envName) {
        return delegateTo.getenv(envName);
    }

    @Override
    public int getegid() {
        return delegateTo.getegid();
    }

    @Override
    public int geteuid() {
        return delegateTo.geteuid();
    }

    @Override
    public int seteuid(int euid) {
        return delegateTo.seteuid(euid);
    }

    @Override
    public int getgid() {
        return delegateTo.getgid();
    }

    @Override
    public int getdtablesize() {
        return delegateTo.getdtablesize();
    }

    @Override
    public String getlogin() {
        return delegateTo.getlogin();
    }

    @Override
    public int getpgid() {
        return delegateTo.getpgid();
    }

    @Override
    public int getpgid(int pid) {
        return delegateTo.getpgid(pid);
    }

    @Override
    public int getpgrp() {
        return delegateTo.getpgrp();
    }

    @Override
    public int getpid() {
        return delegateTo.getpid();
    }

    @Override
    public int getppid() {
        return delegateTo.getppid();
    }

    @Override
    public int getpriority(int which, int who) {
        return delegateTo.getpriority(which, who);
    }

    @Override
    public Passwd getpwent() {
        return delegateTo.getpwent();
    }

    @Override
    public Passwd getpwuid(int which) {
        return delegateTo.getpwuid(which);
    }

    @Override
    public Passwd getpwnam(String which) {
        return delegateTo.getpwnam(which);
    }

    @Override
    public Group getgrgid(int which) {
        return delegateTo.getgrgid(which);
    }

    @Override
    public Group getgrnam(String which) {
        return delegateTo.getgrnam(which);
    }

    @Override
    public Group getgrent() {
        return delegateTo.getgrent();
    }

    @Override
    public int endgrent() {
        return delegateTo.endgrent();
    }

    @Override
    public int setgrent() {
        return delegateTo.setgrent();
    }

    @Override
    public int endpwent() {
        return delegateTo.endpwent();
    }

    @Override
    public int setpwent() {
        return delegateTo.setpwent();
    }

    @Override
    public int getuid() {
        return delegateTo.getuid();
    }

    @Override
    public int getrlimit(int resource, RLimit rlim) {
        return delegateTo.getrlimit(resource, rlim);
    }

    @Override
    public int getrlimit(int resource, Pointer rlim) {
        return delegateTo.getrlimit(resource, rlim);
    }

    @Override
    public RLimit getrlimit(int resource) {
        return delegateTo.getrlimit(resource);
    }

    @Override
    public int setrlimit(int resource, RLimit rlim) {
        return delegateTo.setrlimit(resource, rlim);
    }

    @Override
    public int setrlimit(int resource, Pointer rlim) {
        return delegateTo.setrlimit(resource, rlim);
    }

    @Override
    public int setrlimit(int resource, long rlimCur, long rlimMax) {
        return delegateTo.setrlimit(resource, rlimCur, rlimMax);
    }

    @Override
    public boolean isatty(FileDescriptor descriptor) {
        return delegateTo.isatty(descriptor);
    }

    @Override
    public int kill(int pid, int signal) {
        return delegateTo.kill(pid, signal);
    }

    @Override
    public SignalHandler signal(Signal sig, SignalHandler handler) {
        return delegateTo.signal(sig, handler);
    }

    @Override
    public int lchmod(String filename, int mode) {
        return delegateTo.lchmod(filename, mode);
    }

    @Override
    public int lchown(String filename, int user, int group) {
        return delegateTo.lchown(filename, user, group);
    }

    @Override
    public int link(String oldpath, String newpath) {
        return delegateTo.link(oldpath, newpath);
    }

    @Override
    public FileStat lstat(String path) {
        return delegateTo.lstat(path);
    }

    @Override
    public int lstat(String path, FileStat stat) {
        return delegateTo.lstat(path, stat);
    }

    @Override
    public int mkdir(String path, int mode) {
        return delegateTo.mkdir(path, mode);
    }

    @Override
    public int mkfifo(String path, int mode) {
        return delegateTo.mkfifo(path, mode);
    }

    @Override
    public String readlink(String path) throws IOException {
        return delegateTo.readlink(path);
    }

    @Override
    public int readlink(CharSequence path, byte[] buf, int bufsize) {
        return delegateTo.readlink(path, buf, bufsize);
    }

    @Override
    public int readlink(CharSequence path, ByteBuffer buf, int bufsize) {
        return delegateTo.readlink(path, buf, bufsize);
    }

    @Override
    public int readlink(CharSequence path, Pointer bufPtr, int bufsize) {
        return delegateTo.readlink(path, bufPtr, bufsize);
    }

    @Override
    public int rmdir(String path) {
        return delegateTo.rmdir(path);
    }

    @Override
    public int setenv(String envName, String envValue, int overwrite) {
        return delegateTo.setenv(envName, envValue, overwrite);
    }

    @Override
    public int setsid() {
        return delegateTo.setsid();
    }

    @Override
    public int setgid(int gid) {
        return delegateTo.setgid(gid);
    }

    @Override
    public int setegid(int egid) {
        return delegateTo.setegid(egid);
    }

    @Override
    public int setpgid(int pid, int pgid) {
        return delegateTo.setpgid(pid, pgid);
    }

    @Override
    public int setpgrp(int pid, int pgrp) {
        return delegateTo.setpgrp(pid, pgrp);
    }

    @Override
    public int setpriority(int which, int who, int prio) {
        return delegateTo.setpriority(which, who, prio);
    }

    @Override
    public int setuid(int uid) {
        return delegateTo.setuid(uid);
    }

    @Override
    public FileStat stat(String path) {
        return delegateTo.stat(path);
    }

    @Override
    public int stat(String path, FileStat stat) {
        return delegateTo.stat(path, stat);
    }

    @Override
    public int symlink(String oldpath, String newpath) {
        return delegateTo.symlink(oldpath, newpath);
    }

    @Override
    public int umask(int mask) {
        return delegateTo.umask(mask);
    }

    @Override
    public int unsetenv(String envName) {
        return delegateTo.unsetenv(envName);
    }

    @Override
    public int utimes(String path, long[] atimeval, long[] mtimeval) {
        return delegateTo.utimes(path, atimeval, mtimeval);
    }

    @Override
    public int utimes(String path, Pointer times) {
        return delegateTo.utimes(path, times);
    }

    @Override
    public int futimes(int fd, long[] atimeval, long[] mtimeval) {
        return delegateTo.futimes(fd, atimeval, mtimeval);
    }

    @Override
    public int waitpid(int pid, int[] status, int flags) {
        return delegateTo.waitpid(pid, status, flags);
    }

    @Override
    public int waitpid(long pid, int[] status, int flags) {
        return delegateTo.waitpid(pid, status, flags);
    }

    @Override
    public int wait(int[] status) {
        return delegateTo.wait(status);
    }

    @Override
    public int errno() {
        return delegateTo.errno();
    }

    @Override
    public void errno(int value) {
        delegateTo.errno(value);
    }

    @Override
    public int chdir(String path) {
        return delegateTo.chdir(path);
    }

    @Override
    public boolean isNative() {
        return delegateTo.isNative();
    }

    @Override
    public LibC libc() {
        return delegateTo.libc();
    }

    @Override
    public ProcessMaker newProcessMaker(String... command) {
        return delegateTo.newProcessMaker(command);
    }

    @Override
    public ProcessMaker newProcessMaker() {
        return delegateTo.newProcessMaker();
    }

    @Override
    public long sysconf(Sysconf name) {
        return delegateTo.sysconf(name);
    }

    @Override
    public Times times() {
        return delegateTo.times();
    }

    @Override
    public long posix_spawnp(String path, Collection<? extends SpawnFileAction> fileActions, Collection<? extends CharSequence> argv, Collection<? extends CharSequence> envp) {
        return delegateTo.posix_spawnp(path, fileActions, argv, envp);
    }

    @Override
    public long posix_spawnp(String path, Collection<? extends SpawnFileAction> fileActions, Collection<? extends SpawnAttribute> spawnAttributes, Collection<? extends CharSequence> argv, Collection<? extends CharSequence> envp) {
        return delegateTo.posix_spawnp(path, fileActions, spawnAttributes, argv, envp);
    }

    @Override
    public int flock(int fd, int operation) {
        return delegateTo.flock(fd, operation);
    }

    @Override
    public int dup(int fd) {
        return delegateTo.dup(fd);
    }

    @Override
    public int dup2(int oldFd, int newFd) {
        return delegateTo.dup2(oldFd, newFd);
    }

    @Override
    public int fcntlInt(int fd, Fcntl fcntlConst, int arg) {
        return delegateTo.fcntlInt(fd, fcntlConst, arg);
    }

    @Override
    public int fcntl(int fd, Fcntl fcntlConst) {
        return delegateTo.fcntl(fd, fcntlConst);
    }

    @Override
    public int access(CharSequence path, int amode) {
        return delegateTo.access(path, amode);
    }

    @Override
    public int close(int fd) {
        return delegateTo.close(fd);
    }

    @Override
    public int unlink(CharSequence path) {
        return delegateTo.unlink(path);
    }

    @Override
    public int open(CharSequence path, int flags, int perm) {
        return delegateTo.open(path, flags, perm);
    }

    @Override
    public int write(int fd, byte[] buf, int n) {
        return delegateTo.write(fd, buf, n);
    }

    @Override
    public int read(int fd, byte[] buf, int n) {
        return delegateTo.read(fd, buf, n);
    }

    @Override
    public int pwrite(int fd, byte[] buf, int n, int offset) {
        return delegateTo.pwrite(fd, buf, n, offset);
    }

    @Override
    public int pread(int fd, byte[] buf, int n, int offset) {
        return delegateTo.pread(fd, buf, n, offset);
    }

    @Override
    public int write(int fd, ByteBuffer buf, int n) {
        return delegateTo.write(fd, buf, n);
    }

    @Override
    public int read(int fd, ByteBuffer buf, int n) {
        return delegateTo.read(fd, buf, n);
    }

    @Override
    public int pwrite(int fd, ByteBuffer buf, int n, int offset) {
        return delegateTo.pwrite(fd, buf, n, offset);
    }

    @Override
    public int pread(int fd, ByteBuffer buf, int n, int offset) {
        return delegateTo.pread(fd, buf, n, offset);
    }

    @Override
    public int lseek(int fd, long offset, int whence) {
        return delegateTo.lseek(fd, offset, whence);
    }

    @Override
    public int pipe(int[] fds) {
        return delegateTo.pipe(fds);
    }

    @Override
    public int truncate(CharSequence path, long length) {
        return delegateTo.truncate(path, length);
    }

    @Override
    public int ftruncate(int fd, long offset) {
        return delegateTo.ftruncate(fd, offset);
    }

    @Override
    public int rename(CharSequence oldName, CharSequence newName) {
        return delegateTo.rename(oldName, newName);
    }

    @Override
    public String getcwd() {
        return delegateTo.getcwd();
    }

    @Override
    public int socketpair(int domain, int type, int protocol, int[] fds) {
        return delegateTo.socketpair(domain, type, protocol, fds);
    }

    @Override
    public int sendmsg(int socket, MsgHdr message, int flags) {
        return delegateTo.sendmsg(socket, message, flags);
    }

    @Override
    public int recvmsg(int socket, MsgHdr message, int flags) {
        return delegateTo.recvmsg(socket, message, flags);
    }

    @Override
    public MsgHdr allocateMsgHdr() {
        return delegateTo.allocateMsgHdr();
    }

    @Override
    public int fcntl(int fd, Fcntl fcntlConst, int... arg) {
        return delegateTo.fcntl(fd, fcntlConst, arg);
    }

    @Override
    public int fsync(int fd) {
        return delegateTo.fsync(fd);
    }

    @Override
    public int fdatasync(int fd) {
        return delegateTo.fdatasync(fd);
    }
}
