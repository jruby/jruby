/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.Dir;

import java.util.Arrays;

public abstract class IOPrimitiveNodes {

    @RubiniusPrimitive(name = "io_allocate")
    public static abstract class IOAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOAllocatePrimitiveNode(IOAllocatePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject allocate(VirtualFrame frame, RubyClass classToAllocate) {
            final RubyBasicObject object = new RubyBasicObject(classToAllocate);
            rubyWithSelf(frame, object, "@ibuffer = IO::InternalBuffer.new");
            return object;
        }

    }

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false)
    public static abstract class IOFNMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public IOFNMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOFNMatchPrimitiveNode(IOFNMatchPrimitiveNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public boolean fnmatch(RubyString pattern, RubyString path, int flags) {
            return Dir.fnmatch(pattern.getByteList().getUnsafeBytes(),
                    pattern.getByteList().getBegin(),
                    pattern.getByteList().getBegin() + pattern.getByteList().getRealSize(),
                    path.getByteList().getUnsafeBytes(),
                    path.getByteList().getBegin(),
                    path.getByteList().getBegin() + path.getByteList().getRealSize(),
                    flags) != Dir.FNM_NOMATCH;
        }

    }

    @RubiniusPrimitive(name = "io_ensure_open")
    public static abstract class IOEnsureOpenPrimitiveNode extends RubiniusPrimitiveNode {

        public IOEnsureOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOEnsureOpenPrimitiveNode(IOEnsureOpenPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass ensureOpen(RubyBasicObject file) {
            // TODO CS 18-Apr-15
            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_write")
    public static abstract class IOWritePrimitiveNode extends RubiniusPrimitiveNode {

        public IOWritePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOWritePrimitiveNode(IOWritePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public int write(VirtualFrame frame, RubyBasicObject file, RubyString string) {
            final int fd = (int) rubyWithSelf(frame, file, "@descriptor");

            byte[] bytes = string.getByteList().bytes();

            while (bytes.length > 0) {
                int written = getContext().getPosix().write(fd, bytes, bytes.length);

                if (written == -1) {
                    throw new UnsupportedOperationException();
                }

                if (written < bytes.length) {
                    bytes = Arrays.copyOfRange(bytes, written, bytes.length);
                }
            }

            return bytes.length;
        }

    }

}
