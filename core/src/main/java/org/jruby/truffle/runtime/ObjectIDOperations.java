/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

public abstract class ObjectIDOperations {

    public static boolean isFalse(long id) {
        return id == 0;
    }

    public static boolean isTrue(long id) {
        return id == 2;
    }

    public static boolean isNil(long id) {
        return id == 4;
    }

    public static boolean isFixnum(long id) {
        return id % 2 != 0;
    }

    public static long toFixnum(long id) {
        return (id - 1) / 2;
    }

    public static long fixnumToID(long fixnum) {
        return fixnum * 2 + 1;
    }
}
