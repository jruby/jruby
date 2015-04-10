/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.runtime;

/**
 * The encoding to be used for the string resulting from pack.
 * <p>
 * We use this enum as the range of encodings possible are very limited and
 * we want to abstracted between JRuby's pack and Truffle's pack.
 */
public enum PackEncoding {
    DEFAULT,
    ASCII_8BIT,
    US_ASCII,
    UTF_8;

    /**
     * Given the current encoding for a pack string, and something that requires
     * another encoding, give us the encoding that we should use for the result
     * of pack.
     */
    public PackEncoding unifyWith(PackEncoding other) {
        if (this == DEFAULT) {
            return other;
        }

        if (other == DEFAULT) {
            return this;
        }

        switch (this) {
            case ASCII_8BIT:
            case US_ASCII:
                switch (other) {
                    case ASCII_8BIT:
                    case US_ASCII:
                        return ASCII_8BIT;
                    case UTF_8:
                        return ASCII_8BIT;
                    default:
                        throw new UnsupportedOperationException();
                }
            case UTF_8:
                switch (other) {
                    case ASCII_8BIT:
                    case US_ASCII:
                        return ASCII_8BIT;
                    case UTF_8:
                        return UTF_8;
                    default:
                        throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
