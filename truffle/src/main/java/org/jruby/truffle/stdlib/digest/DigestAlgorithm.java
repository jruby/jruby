/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.digest;

enum DigestAlgorithm {
    MD5("MD5", 16),
    SHA1("SHA1", 20),
    SHA256("SHA-256", 32),
    SHA384("SHA-384", 48),
    SHA512("SHA-512", 64);

    private final String name;
    private final int length;

    DigestAlgorithm(String name, int length) {
        this.name = name;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }
}
