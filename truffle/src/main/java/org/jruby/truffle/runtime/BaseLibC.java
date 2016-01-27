/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import jnr.constants.platform.Sysconf;
import jnr.ffi.Pointer;
import jnr.ffi.Variable;
import jnr.ffi.annotations.Direct;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Transient;
import jnr.posix.*;

import java.nio.ByteBuffer;

public abstract class BaseLibC implements LibC {
    
    @Override
    public CharSequence crypt(CharSequence key, CharSequence salt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer crypt(byte[] key, byte[] salt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int chmod(CharSequence filename, int mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fchmod(int fd, int mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int chown(CharSequence filename, int user, int group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fchown(int fd, int user, int group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fstat(int fd, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fstat64(int fd, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getenv(CharSequence envName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getegid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setegid(int egid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int geteuid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int seteuid(int euid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getgid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getlogin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setgid(int gid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getpgid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getpgid(int pid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setpgid(int pid, int pgid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getpgrp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setpgrp(int pid, int pgrp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getppid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getpid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativePasswd getpwent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativePasswd getpwuid(int which) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativePasswd getpwnam(CharSequence which) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativeGroup getgrent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativeGroup getgrgid(int which) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NativeGroup getgrnam(CharSequence which) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setpwent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int endpwent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setgrent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int endgrent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getuid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setsid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setuid(int uid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getrlimit(int resource, @Out RLimit rlim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getrlimit(int resource, Pointer rlim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setrlimit(int resource, @In RLimit rlim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setrlimit(int resource, Pointer rlim) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int kill(int pid, int signal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int dup(int fd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int dup2(int oldFd, int newFd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fcntl(int fd, int fnctl, int arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fcntl(int fd, int fnctl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fcntl(int fd, int fnctl, int... arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int access(CharSequence path, int amode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getdtablesize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long signal(int sig, LibCSignalHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lchmod(CharSequence filename, int mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lchown(CharSequence filename, int user, int group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int link(CharSequence oldpath, CharSequence newpath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lstat(CharSequence path, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lstat64(CharSequence path, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int mkdir(CharSequence path, int mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int rmdir(CharSequence path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int stat(CharSequence path, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int stat64(CharSequence path, @Out @Transient FileStat stat) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int symlink(CharSequence oldpath, CharSequence newpath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readlink(CharSequence oldpath, @Out ByteBuffer buffer, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readlink(CharSequence path, @Out byte[] buffer, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readlink(CharSequence path, Pointer bufPtr, int bufsize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setenv(CharSequence envName, CharSequence envValue, int overwrite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int umask(int mask) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int unsetenv(CharSequence envName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int utimes(CharSequence path, @In Timeval[] times) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int utimes(String path, @In Pointer times) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int futimes(int fd, @In Timeval[] times) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fork() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int waitpid(long pid, @Out int[] status, int options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int wait(@Out int[] status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getpriority(int which, int who) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setpriority(int which, int who, int prio) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int isatty(int fd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(int fd, @Out ByteBuffer dst, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(int fd, @In ByteBuffer src, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(int fd, @Out byte[] dst, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(int fd, @In byte[] src, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int pread(int fd, @Out ByteBuffer src, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int pread(int fd, @Out byte[] src, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int pwrite(int fd, @In ByteBuffer src, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int pwrite(int fd, @In byte[] src, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lseek(int fd, long offset, int whence) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int close(int fd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int execv(CharSequence path, @In CharSequence... argv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int execve(CharSequence path, @In CharSequence[] argv, @In CharSequence[] envp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int chdir(CharSequence path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long sysconf(Sysconf name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long times(@Out @Transient NativeTimes tms) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int flock(int fd, int mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int unlink(CharSequence path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int open(CharSequence path, int flags, int perm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int pipe(@Out int[] fds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int truncate(CharSequence path, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int ftruncate(int fd, long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int rename(CharSequence oldName, CharSequence newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getcwd(byte[] cwd, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fsync(int fd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int fdatasync(int fd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int socketpair(int domain, int type, int protocol, @Out int[] fds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int sendmsg(int socket, @In MsgHdr message, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int recvmsg(int socket, @Direct MsgHdr message, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Variable<Long> environ() {
        throw new UnsupportedOperationException();
    }
    
}
