/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.platform.posix;

import jnr.ffi.Pointer;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.platform.FDSet;

public class PosixFDSet8Bytes implements FDSet {

    private final static int MAX_FDS = 1024;
    private final static int FIELD_SIZE_IN_BYTES = 8;
    private final static int FIELD_SIZE_IN_BITS = FIELD_SIZE_IN_BYTES * 8;

    private final Pointer bitmap;

    public PosixFDSet8Bytes() {
        bitmap = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().allocateDirect(MAX_FDS / 8);
    }

    public void set(int fd) {
        checkBounds(fd);

        final int offset = bitmapAddressOffset(fd);

        bitmap.putLong(offset, bitmap.getLong(offset) | bitmapElementMask(fd));
    }

    public boolean isSet(int fd) {
        checkBounds(fd);

        return (bitmap.getLong(bitmapAddressOffset(fd)) & bitmapElementMask(fd)) != 0;
    }

    public Pointer getPointer() {
        return bitmap;
    }

    private void checkBounds(int fd) {
        if (fd < 0 || fd >= MAX_FDS) {
            throw new IllegalArgumentException(StringUtils.format("Supplied file descriptor value must be > 0 and < %d", MAX_FDS));
        }
    }

    private int bitmapElementIndex(int fd) {
        return fd / FIELD_SIZE_IN_BITS;
    }

    private int bitmapAddressOffset(int fd) {
        return bitmapElementIndex(fd) * FIELD_SIZE_IN_BYTES;
    }

    private long bitmapElementMask(int fd) {
        return 1L << (fd % FIELD_SIZE_IN_BITS);
    }
}
