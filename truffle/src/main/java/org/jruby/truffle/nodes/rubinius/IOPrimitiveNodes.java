/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transliterated from C++ code in Rubinius.
 *
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;

import org.jruby.RubyEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;
import org.jruby.util.Dir;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class IOPrimitiveNodes {

    private static int STDOUT = 1;

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


    @RubiniusPrimitive(name = "io_truncate", needsSelf = false)
    public static abstract class IOTruncatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOTruncatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int truncate(RubyString path, int length) {
            return truncate(path, (long) length);
        }

        @Specialization
        public int truncate(RubyString path, long length) {
            final String pathString = RubyEncoding.decodeUTF8(path.getByteList().getUnsafeBytes(), path.getByteList().getBegin(), path.getByteList().getRealSize());
            final int result = posix().truncate(pathString, length);
            if (result == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }
            return result;
        }

    }

    @RubiniusPrimitive(name = "io_ftruncate")
    public static abstract class IOFTruncatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOFTruncatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int ftruncate(VirtualFrame frame, RubyBasicObject io, int length) {
            return ftruncate(frame, io, (long) length);
        }

        @Specialization
        public int ftruncate(VirtualFrame frame, RubyBasicObject io, long length) {
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");
            return posix().ftruncate(fd, length);
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
        public RubyBasicObject ensureOpen(VirtualFrame frame, RubyBasicObject file) {
            // TODO BJF 13-May-2015 Handle nil case
            final int fd = (int) rubyWithSelf(frame, file, "@descriptor");
            if(fd == -1){
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().ioError("closed stream",this));
            } else if (fd == -2){
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().ioError("shutdown stream",this));
            }
            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_reopen")
    public static abstract class IOReopenPrimitiveNode extends RubiniusPrimitiveNode {

        public IOReopenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object reopen(VirtualFrame frame, RubyBasicObject file, RubyBasicObject io) {
            final int fd = (int) rubyWithSelf(frame, file, "@descriptor");
            final int fdOther = (int) rubyWithSelf(frame, io, "@descriptor");

            final int result = posix().dup2(fd, fdOther);
            if (result == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            final int mode = posix().fcntl(fd, Fcntl.F_GETFL);
            if (mode < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }
            rubyWithSelf(frame, file, "@mode = mode", "mode", mode);

            rubyWithSelf(frame, io, "reset_buffering");

            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_reopen_path")
    public static abstract class IOReopenPathPrimitiveNode extends RubiniusPrimitiveNode {

        public IOReopenPathPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object reopenPath(VirtualFrame frame, RubyBasicObject file, RubyString path, int mode) {
            int fd = (int) rubyWithSelf(frame, file, "@descriptor");
            final String pathString = path.toString();

            int otherFd = posix().open(pathString, mode, 666);
            if (otherFd < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            final int result = posix().dup2(otherFd, fd);
            if (result == -1) {
                final int errno = posix().errno();
                if (errno == Errno.EBADF.intValue()) {
                    rubyWithSelf(frame, file, "@descriptor = desc", "desc", otherFd);
                    fd = otherFd;
                } else {
                    if (otherFd > 0) {
                        posix().close(otherFd);
                    }
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().errnoError(errno, this));
                }

            } else {
                posix().close(otherFd);
            }


            final int newMode = posix().fcntl(fd, Fcntl.F_GETFL);
            if (newMode < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }
            rubyWithSelf(frame, file, "@mode = mode", "mode", newMode);

            rubyWithSelf(frame, file, "reset_buffering");

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

            if (getContext().getDebugStandardOut() != null && fd == STDOUT) {
                getContext().getDebugStandardOut().write(string.getByteList().unsafeBytes(), string.getByteList().begin(), string.getByteList().length());
                return string.getByteList().length();
            }

            // We have to copy here as write starts at byte[0], and the ByteList may not

            final ByteList byteList = string.getByteList();

            // TODO (eregon, 11 May 2015): review consistency under concurrent modification
            final ByteBuffer buffer = ByteBuffer.wrap(byteList.unsafeBytes(), byteList.begin(), byteList.length());

            int total = 0;

            while (buffer.hasRemaining()) {
                getContext().getSafepointManager().poll(this);

                int written = posix().write(fd, buffer, buffer.remaining());

                if (written == -1) {
                    throw new UnsupportedOperationException();
                }

                buffer.position(buffer.position() + written);

                total += written;
            }

            return total;
        }

    }

    @RubiniusPrimitive(name = "io_close")
    public static abstract class IOClosePrimitiveNode extends RubiniusPrimitiveNode {

        public IOClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int close(VirtualFrame frame, RubyBasicObject io) {
            rubyWithSelf(frame, io, "ensure_open");
            final int fd = (int) rubyWithSelf(frame, io, "@descriptor");

            if (fd == -1) {
                return 0;
            }

            rubyWithSelf(frame, io, "@descriptor = -1");

            if (fd < 3) {
                return 0;
            }

            final int result = posix().close(fd);

            // TODO BJF 13-May-2015 Implement more error handling from Rubinius
            if (result == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }
            return 0;
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
