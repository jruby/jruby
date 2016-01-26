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

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

public class TruffleJavaPOSIX extends POSIXDelegator implements POSIX {

    private static final int STDIN = 0;
    private static final int STDOUT = 1;
    private static final int STDERR = 2;

    private final RubyContext context;

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

}
