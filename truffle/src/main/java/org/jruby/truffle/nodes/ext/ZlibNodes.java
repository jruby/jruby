/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.ext;

import com.jcraft.jzlib.JZlib;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ext.digest.BubbleBabble;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.zip.CRC32;

@CoreClass(name = "Truffle::Zlib")
public abstract class ZlibNodes {

    @CoreMethod(names = "crc32", isModuleFunction = true, optional = 2)
    public abstract static class CRC32Node extends CoreMethodArrayArgumentsNode {

        public CRC32Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int crc32(UndefinedPlaceholder message, UndefinedPlaceholder initial) {
            return 0;
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public long crc32(RubyString message, UndefinedPlaceholder initial) {
            final ByteList bytes = message.getByteList();
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            return crc32.getValue();
        }

        @Specialization
        public long crc32(RubyString message, int initial) {
            return crc32(message, (long) initial);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public long crc32(RubyString message, long initial) {
            final ByteList bytes = message.getByteList();
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            return JZlib.crc32_combine(initial, crc32.getValue(), bytes.length());
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyBignum(initial)")
        public long crc32(RubyString message, RubyBasicObject initial) {
            throw new RaiseException(getContext().getCoreLibrary().rangeError("bignum too big to convert into `unsigned long'", this));
        }

    }

}
