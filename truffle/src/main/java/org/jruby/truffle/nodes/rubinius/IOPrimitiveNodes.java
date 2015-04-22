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
import org.jruby.truffle.runtime.DebugOperations;
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
            rubyWithSelf(frame, object, "@lineno = 0");
            return object;
        }

    }

    @RubiniusPrimitive(name = "io_open", needsSelf = false)
    public static abstract class IOOpenPrimitiveNode extends RubiniusPrimitiveNode {

        public IOOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOOpenPrimitiveNode(IOOpenPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public int open(RubyString path, int mode, int permission) {
            return getContext().getPosix().open(path.getByteList(), mode, permission);
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

            // We have to copy here as write starts at byte[0], and the ByteList may not

            byte[] bytes = string.getByteList().bytes();

            while (bytes.length > 0) {
                getContext().getSafepointManager().poll(this);

                int written = getContext().getPosix().write(fd, bytes, bytes.length);

                if (written == -1) {
                    throw new UnsupportedOperationException();
                }

                // Have to copy here again for the same reason!

                bytes = Arrays.copyOfRange(bytes, written, bytes.length);
            }

            return bytes.length;
        }

    }

    @RubiniusPrimitive(name = "io_close")
    public static abstract class IOClosePrimitiveNode extends RubiniusPrimitiveNode {

        public IOClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOClosePrimitiveNode(IOClosePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public int close(VirtualFrame frame, RubyBasicObject io) {
            // In Rubinius this does a lot more, but we'll stick with this for now
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");
            return getContext().getPosix().close(fd);
        }

    }

    @RubiniusPrimitive(name = "io_seek")
    public static abstract class IOSeekPrimitiveNode extends RubiniusPrimitiveNode {

        public IOSeekPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOSeekPrimitiveNode(IOSeekPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public int seek(VirtualFrame frame, RubyBasicObject io, int amount, int whence) {
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");
            return getContext().getPosix().lseek(fd, amount, whence);
        }

    }

}
