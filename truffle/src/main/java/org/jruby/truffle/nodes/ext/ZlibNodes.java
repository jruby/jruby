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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.ByteList;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@CoreClass(name = "Truffle::Zlib")
public abstract class ZlibNodes {

    @CoreMethod(names = "crc32", isModuleFunction = true, optional = 2)
    public abstract static class CRC32Node extends CoreMethodArrayArgumentsNode {

        public CRC32Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int crc32(NotProvided message, NotProvided initial) {
            return 0;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public long crc32(RubyBasicObject message, NotProvided initial) {
            final ByteList bytes = StringNodes.getByteList(message);
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            return crc32.getValue();
        }

        @Specialization(guards = "isRubyString(message)")
        public long crc32(RubyBasicObject message, int initial) {
            return crc32(message, (long) initial);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public long crc32(RubyBasicObject message, long initial) {
            final ByteList bytes = StringNodes.getByteList(message);
            final CRC32 crc32 = new CRC32();
            crc32.update(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            return JZlib.crc32_combine(initial, crc32.getValue(), bytes.length());
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(message)", "isRubyBignum(initial)"})
        public long crc32(RubyBasicObject message, RubyBasicObject initial) {
            throw new RaiseException(getContext().getCoreLibrary().rangeError("bignum too big to convert into `unsigned long'", this));
        }

    }

    private static final int BUFFER_SIZE = 1024;

    @CoreMethod(names = "deflate", isModuleFunction = true, required = 2)
    public abstract static class DeflateNode extends CoreMethodArrayArgumentsNode {

        public DeflateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public RubyBasicObject deflate(RubyBasicObject message, int level) {
            final Deflater deflater = new Deflater(level);

            final ByteList messageBytes = StringNodes.getByteList(message);
            deflater.setInput(messageBytes.unsafeBytes(), messageBytes.begin(), messageBytes.length());

            final ByteList outputBytes = new ByteList(BUFFER_SIZE);

            deflater.finish();

            while (!deflater.finished()) {
                outputBytes.ensure(outputBytes.length() + BUFFER_SIZE);
                final int count = deflater.deflate(outputBytes.unsafeBytes(), outputBytes.begin() + outputBytes.length(), BUFFER_SIZE);
                outputBytes.setRealSize(outputBytes.realSize() + count);
            }

            deflater.end();

            return createString(outputBytes);
        }

    }

    @CoreMethod(names = "inflate", isModuleFunction = true, required = 1)
    public abstract static class InflateNode extends CoreMethodArrayArgumentsNode {

        public InflateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(message)")
        public RubyBasicObject inflate(RubyBasicObject message) {
            final Inflater inflater = new Inflater();

            final ByteList messageBytes = StringNodes.getByteList(message);
            inflater.setInput(messageBytes.unsafeBytes(), messageBytes.begin(), messageBytes.length());

            final ByteList outputBytes = new ByteList(BUFFER_SIZE);

            while (!inflater.finished()) {
                outputBytes.ensure(outputBytes.length() + BUFFER_SIZE);

                final int count;

                try {
                    count = inflater.inflate(outputBytes.unsafeBytes(), outputBytes.begin() + outputBytes.length(), BUFFER_SIZE);
                } catch (DataFormatException e) {
                    throw new RuntimeException(e);
                }

                outputBytes.setRealSize(outputBytes.getRealSize() + count);
            }

            inflater.end();

            return createString(outputBytes);
        }

    }

}
