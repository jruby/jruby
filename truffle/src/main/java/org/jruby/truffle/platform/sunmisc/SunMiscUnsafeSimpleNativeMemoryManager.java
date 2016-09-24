/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.platform.sunmisc;

import org.jruby.truffle.platform.NativePointer;
import org.jruby.truffle.platform.SimpleNativeMemoryManager;
import sun.misc.Unsafe;

public class SunMiscUnsafeSimpleNativeMemoryManager implements SimpleNativeMemoryManager {

    private final Unsafe unsafe;

    public SunMiscUnsafeSimpleNativeMemoryManager(Unsafe unsafe) {
        this.unsafe = unsafe;
    }

    @Override
    public NativePointer allocate(long size) {
        final long address = unsafe.allocateMemory(size);

        if (address == 0) {
            throw new OutOfMemoryError();
        }

        // TODO deallocation

        return new UnsafeNativePointer(address);
    }

    private class UnsafeNativePointer implements NativePointer {

        private final long address;

        public UnsafeNativePointer(long address) {
            this.address = address;
        }

        @Override
        public long getAddress() {
            return address;
        }

        @Override
        public byte readByte(long offset) {
            return unsafe.getByte(address + offset);
        }

        @Override
        public void writeByte(long offset, byte value) {
            unsafe.putByte(address + offset, value);
        }
    }

}
