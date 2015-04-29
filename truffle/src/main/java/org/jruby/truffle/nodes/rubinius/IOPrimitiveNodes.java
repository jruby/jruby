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
import jnr.constants.platform.Fcntl;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
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

        @Specialization
        public RubyBasicObject allocate(VirtualFrame frame, RubyClass classToAllocate) {
            final RubyBasicObject object = new RubyBasicObject(classToAllocate);
            rubyWithSelf(frame, object, "@ibuffer = IO::InternalBuffer.new");
            rubyWithSelf(frame, object, "@lineno = 0");
            return object;
        }

    }

    @RubiniusPrimitive(name = "io_connect_pipe", needsSelf = false)
    public static abstract class IOConnectPipeNode extends RubiniusPrimitiveNode {

        public IOConnectPipeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean connectPipe(VirtualFrame frame, RubyBasicObject lhs, RubyBasicObject rhs) {
            final int[] fds = new int[2];

            if (posix().pipe(fds) == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            newOpenFd(fds[0]);
            newOpenFd(fds[1]);

            rubyWithSelf(frame, lhs, "@descriptor = fd", "fd", fds[0]);
            rubyWithSelf(frame, lhs, "@mode = File::Constants::RDONLY");

            rubyWithSelf(frame, rhs, "@descriptor = fd", "fd", fds[1]);
            rubyWithSelf(frame, rhs, "@mode = File::Constants::WRONLY");

            return true;
        }

        private void newOpenFd(int newFd) {
            final int FD_CLOEXEC = 1;

            if (newFd > 2) {
                int flags = posix().fcntl(newFd, Fcntl.F_GETFD);

                if (flags == -1) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
                }

                flags = posix().fcntlInt(newFd, Fcntl.F_SETFD, flags | FD_CLOEXEC);

                if (flags == -1) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
                }
            }
        }
    }

    @RubiniusPrimitive(name = "io_open", needsSelf = false)
    public static abstract class IOOpenPrimitiveNode extends RubiniusPrimitiveNode {

        public IOOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int open(RubyString path, int mode, int permission) {
            return posix().open(path.getByteList(), mode, permission);
        }

    }

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false)
    public static abstract class IOFNMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public IOFNMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization
        public int write(VirtualFrame frame, RubyBasicObject file, RubyString string) {
            final int fd = (int) rubyWithSelf(frame, file, "@descriptor");

            // We have to copy here as write starts at byte[0], and the ByteList may not

            byte[] bytes = string.getByteList().bytes();

            while (bytes.length > 0) {
                getContext().getSafepointManager().poll(this);

                int written = posix().write(fd, bytes, bytes.length);

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

        @Specialization
        public int close(VirtualFrame frame, RubyBasicObject io) {
            // In Rubinius this does a lot more, but we'll stick with this for now
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");
            return posix().close(fd);
        }

    }

    @RubiniusPrimitive(name = "io_seek", lowerFixnumParameters = {0, 1})
    public static abstract class IOSeekPrimitiveNode extends RubiniusPrimitiveNode {

        public IOSeekPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int seek(VirtualFrame frame, RubyBasicObject io, int amount, int whence) {
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");
            return posix().lseek(fd, amount, whence);
        }

    }

}
