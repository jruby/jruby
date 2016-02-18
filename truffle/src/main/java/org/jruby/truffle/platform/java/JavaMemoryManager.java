/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.java;

import jnr.ffi.Pointer;
import jnr.ffi.provider.MemoryManager;
import jnr.ffi.provider.NullMemoryIO;

import java.nio.ByteBuffer;

public class JavaMemoryManager implements MemoryManager {

    @Override
    public Pointer allocate(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer allocateDirect(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer allocateDirect(int size, boolean clear) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer allocateTemporary(int size, boolean clear) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer newPointer(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer newPointer(long address) {
        if (address == 0) {
            return new NullMemoryIO(null);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Pointer newPointer(long address, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer newOpaquePointer(long address) {
        throw new UnsupportedOperationException();
    }

}
