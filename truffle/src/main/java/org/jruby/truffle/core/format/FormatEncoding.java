/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;

public enum FormatEncoding {

    DEFAULT(ASCIIEncoding.INSTANCE),
    ASCII_8BIT(ASCIIEncoding.INSTANCE),
    US_ASCII(USASCIIEncoding.INSTANCE),
    UTF_8(UTF8Encoding.INSTANCE);

    private final Encoding encoding;

    FormatEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    public Encoding getEncodingForLength(int length) {
        if (length == 0) {
            return USASCIIEncoding.INSTANCE;
        } else {
            return encoding;
        }
    }

    /**
     * Given the current encoding for a pack string, and something that requires
     * another encoding, give us the encoding that we should use for the result
     * of pack.
     */
    public FormatEncoding unifyWith(FormatEncoding other) {
        if (this == DEFAULT) {
            return other;
        }

        if (other == DEFAULT) {
            return this;
        }

        switch (other) {
            case ASCII_8BIT:
            case US_ASCII:
                return ASCII_8BIT;
            case UTF_8:
                switch (this) {
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
