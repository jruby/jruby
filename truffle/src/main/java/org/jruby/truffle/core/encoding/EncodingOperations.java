/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transposed from org.jruby.runtime.encoding.EncodingService,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;

public abstract class EncodingOperations {

    public static Encoding getEncoding(DynamicObject rubyEncoding) {
        Encoding encoding = Layouts.ENCODING.getEncoding(rubyEncoding);

        if (encoding == null) {
            // Bounded by the number of encodings
            CompilerDirectives.transferToInterpreterAndInvalidate();

            final Rope name = StringOperations.rope(Layouts.ENCODING.getName(rubyEncoding));
            encoding = loadEncoding(name);
            Layouts.ENCODING.setEncoding(rubyEncoding, encoding);
        }

        return encoding;
    }

    @TruffleBoundary
    private static EncodingDB.Entry findEncodingEntry(Rope name) {
        return EncodingDB.getEncodings().get(name.getBytes(), 0, name.byteLength());
    }

    @TruffleBoundary
    private static EncodingDB.Entry findAliasEntry(Rope name) {
        return EncodingDB.getAliases().get(name.getBytes(), 0, name.byteLength());
    }

    private static EncodingDB.Entry findEncodingOrAliasEntry(Rope name) {
        final EncodingDB.Entry e = findEncodingEntry(name);
        return e != null ? e : findAliasEntry(name);
    }

    @TruffleBoundary
    private static Encoding loadEncoding(Rope name) {
        final EncodingDB.Entry entry = findEncodingOrAliasEntry(name);

        if (entry == null) {
            return null;
        }

        return entry.getEncoding();
    }

}
