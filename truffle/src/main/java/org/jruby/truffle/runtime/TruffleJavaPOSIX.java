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

import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.posix.FileStat;
import jnr.posix.LibC;
import jnr.posix.POSIX;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TruffleJavaPOSIX extends POSIXDelegator implements POSIX {

    private static final int STDIN = 0;
    private static final int STDOUT = 1;
    private static final int STDERR = 2;

    private final RubyContext context;

    private final AtomicInteger nextFileHandle = new AtomicInteger();
    private final Map<Integer, RandomAccessFile> fileHandles = new ConcurrentHashMap<>();

    public TruffleJavaPOSIX(RubyContext context, POSIX delegateTo) {
        super(delegateTo);
        this.context = context;
    }

    @Override
    public int fcntlInt(int fd, Fcntl fcntlConst, int arg) {
        if (fcntlConst.longValue() == Fcntl.F_GETFL.longValue()) {
            switch (fd) {
                case STDIN:
                    return OpenFlags.O_RDONLY.intValue();
                case STDOUT:
                case STDERR:
                    return OpenFlags.O_WRONLY.intValue();
            }
        }

        return super.fcntlInt(fd, fcntlConst, arg);
    }

    @Override
    public int getpid() {
        return context.hashCode();
    }

    @Override
    public LibC libc() {
        return JavaLibC.INSTANCE;
    }

    @Override
    public int open(CharSequence path, int flags, int perm) {
        if (perm != 0666) {
            return super.open(path, flags, perm);
        }

        final int fileHandle = nextFileHandle.getAndIncrement();

        if (fileHandle < 0) {
            throw new UnsupportedOperationException();
        }

        final int basicMode = flags & 3;
        final String mode;

        if (basicMode == OpenFlags.O_RDONLY.intValue()) {
            mode = "r";
        } else if (basicMode == OpenFlags.O_WRONLY.intValue()) {
            mode = "w";
        } else if (basicMode == OpenFlags.O_RDWR.intValue()) {
            mode = "rw";
        } else {
            return super.open(path, flags, perm);
        }

        final RandomAccessFile randomAccessFile;

        try {
            randomAccessFile = new RandomAccessFile(path.toString(), mode);
        } catch (FileNotFoundException e) {
            return -1;
        }

        fileHandles.put(fileHandle, randomAccessFile);

        return fileHandle;
    }

    @Override
    public int write(int fd, byte[] buf, int n) {
        return pwrite(fd, buf, n, 0);
    }

    @Override
    public int write(int fd, ByteBuffer buf, int n) {
        return pwrite(fd, buf.array(), n, buf.arrayOffset());
    }

    @Override
    public int pwrite(int fd, byte[] buf, int n, int offset) {
        if (fd == STDOUT || fd == STDERR) {
            final PrintStream stream;

            switch (fd) {
                case STDOUT:
                    stream = System.out;
                    break;
                case STDERR:
                    stream = System.err;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            stream.write(buf, offset, buf.length);

            return buf.length;
        }

        return super.pwrite(fd, buf, n, offset);
    }

    @Override
    public int getgid() {
        return 0;
    }

    @Override
    public FileStat allocateStat() {
        return new TruffleJavaFileStat(this, null);
    }

    @Override
    public String getenv(String envName) {
        final String javaValue = System.getenv(envName);

        if (javaValue != null) {
            return javaValue;
        }

        return super.getenv(envName);
    }
}
