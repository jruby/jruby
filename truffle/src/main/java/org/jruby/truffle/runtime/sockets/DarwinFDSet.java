/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.sockets;

import jnr.ffi.Pointer;
import jnr.ffi.provider.MemoryManager;

public class DarwinFDSet implements FDSet {

    private static final int INT32_SIZE = 4;

    private static final int DARWIN_FD_SETSIZE = 1024;
    private static final int DARWIN_NBBY = 8;
    private static final int DARWIN_NFDBITS = INT32_SIZE * DARWIN_NBBY;
    private static final int FD_BITS_SIZE = __DARWIN_howmany(DARWIN_FD_SETSIZE, DARWIN_NFDBITS);

    private static final MemoryManager memoryManager = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager();

    private final Pointer pointer;

    public DarwinFDSet() {
        pointer = memoryManager.allocateDirect(FD_BITS_SIZE * INT32_SIZE);
    }

    @Override
    public void set(int fd) {
        final int index = fd / DARWIN_NFDBITS;
        final int offset = index * INT32_SIZE;
        pointer.putInt(offset, pointer.getInt(offset) | 1 << (fd % DARWIN_NFDBITS));
    }

    @Override
    public boolean isSet(int fd) {
        final int index = fd / DARWIN_NFDBITS;
        final int offset = index * INT32_SIZE;
        return (pointer.getInt(offset) & (1 << (fd % DARWIN_NFDBITS))) != 0;
    }

    @Override
    public Pointer getPointer() {
        return pointer;
    }

    private static int __DARWIN_howmany(int x, int y) {
        return x % y == 0 ? x / y : (x / y) + 1;
    }

}
