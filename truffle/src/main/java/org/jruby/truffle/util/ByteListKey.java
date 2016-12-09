/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.util;

import java.util.Arrays;

public class ByteListKey {

    private final ByteList bytes;
    private final int hash;

    public ByteListKey(ByteList bytes) {
        this.bytes = bytes;
        hash = Arrays.hashCode(bytes.bytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ByteListKey that = (ByteListKey) o;

        return bytes.equals(that.bytes);
    }

    @Override
    public int hashCode() {
        return hash;
    }

}
