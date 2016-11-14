/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.posix.DefaultNativeTimeval;
import jnr.posix.Timeval;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.array.ArrayGuards;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.rope.BytesVisitor;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.core.thread.ThreadManager.ResultWithinTime;
import org.jruby.truffle.extra.ffi.PointerPrimitiveNodes;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.platform.FDSet;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.util.ByteList;
import org.jruby.util.Dir;
import org.jruby.util.unsafe.UnsafeHolder;

import java.nio.ByteBuffer;

import static org.jruby.truffle.core.string.StringOperations.rope;

public abstract class IOPrimitiveNodes {

    public static abstract class IOPrimitiveArrayArgumentsNode extends PrimitiveArrayArgumentsNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        public IOPrimitiveArrayArgumentsNode() {
        }

        public IOPrimitiveArrayArgumentsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected int ensureSuccessful(int result, int errno, String extra) {
            assert result >= -1;
            if (result == -1) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().errnoError(errno, extra, this));
            }
            return result;
        }

        protected int ensureSuccessful(int result) {
            return ensureSuccessful(result, posix().errno(), "");
        }

        protected int ensureSuccessful(int result, String extra) {
            return ensureSuccessful(result, posix().errno(), " - " + extra);
        }
    }

    private static final int STDOUT = 1;

    @Primitive(name = "io_allocate", unsafe = UnsafeGroup.IO)
    public static abstract class IOAllocatePrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode newBufferNode;
        @Child private AllocateObjectNode allocateNode;

        public IOAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            newBufferNode = DispatchHeadNodeFactory.createMethodCall(context);
            allocateNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(VirtualFrame frame, DynamicObject classToAllocate) {
            final DynamicObject buffer = (DynamicObject) newBufferNode.call(frame, coreLibrary().getInternalBufferClass(), "new");
            return allocateNode.allocate(classToAllocate, buffer, 0, 0, 0);
        }

    }

    @Primitive(name = "io_connect_pipe", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOConnectPipeNode extends IOPrimitiveArrayArgumentsNode {

        @CompilationFinal private int RDONLY = -1;
        @CompilationFinal private int WRONLY = -1;

        @Specialization
        public boolean connectPipe(DynamicObject lhs, DynamicObject rhs) {
            final int[] fds = new int[2];

            ensureSuccessful(posix().pipe(fds));

            newOpenFd(fds[0]);
            newOpenFd(fds[1]);

            Layouts.IO.setDescriptor(lhs, fds[0]);
            Layouts.IO.setMode(lhs, getRDONLY());

            Layouts.IO.setDescriptor(rhs, fds[1]);
            Layouts.IO.setMode(rhs, getWRONLY());

            return true;
        }

        @TruffleBoundary(throwsControlFlowException = true)
        private void newOpenFd(int newFd) {
            final int FD_CLOEXEC = 1;

            if (newFd > 2) {
                final int flags = ensureSuccessful(posix().fcntl(newFd, Fcntl.F_GETFD));
                ensureSuccessful(posix().fcntlInt(newFd, Fcntl.F_SETFD, flags | FD_CLOEXEC));
            }
        }

        private int getRDONLY() {
            if (RDONLY == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                RDONLY = (int) getContext().getNativePlatform().getRubiniusConfiguration().get("rbx.platform.file.O_RDONLY");
            }

            return RDONLY;
        }

        private int getWRONLY() {
            if (WRONLY == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                WRONLY = (int) getContext().getNativePlatform().getRubiniusConfiguration().get("rbx.platform.file.O_WRONLY");
            }

            return WRONLY;
        }

    }

    @Primitive(name = "io_open", needsSelf = false, lowerFixnum = {2, 3}, unsafe = UnsafeGroup.IO)
    public static abstract class IOOpenPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyString(path)")
        public int open(DynamicObject path, int mode, int permission) {
            String pathString = StringOperations.getString(path);
            int fd = posix().open(pathString, mode, permission);
            if (fd == -1) {
                ensureSuccessful(fd, pathString);
            }
            return fd;
        }

    }

    @Primitive(name = "io_truncate", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOTruncatePrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyString(path)")
        public int truncate(DynamicObject path, long length) {
            return ensureSuccessful(posix().truncate(StringOperations.getString(path), length));
        }

    }

    @Primitive(name = "io_ftruncate", unsafe = UnsafeGroup.IO)
    public static abstract class IOFTruncatePrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Specialization
        public int ftruncate(DynamicObject io, long length) {
            final int fd = Layouts.IO.getDescriptor(io);
            return ensureSuccessful(posix().ftruncate(fd, length));
        }

    }

    @Primitive(name = "io_fnmatch", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOFNMatchPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubyString(pattern)", "isRubyString(path)" })
        public boolean fnmatch(DynamicObject pattern, DynamicObject path, int flags) {
            final Rope patternRope = rope(pattern);
            final Rope pathRope = rope(path);

            return Dir.fnmatch(patternRope.getBytes(),
                    0,
                    patternRope.byteLength(),
                    pathRope.getBytes(),
                    0,
                    pathRope.byteLength(),
                    flags) != Dir.FNM_NOMATCH;
        }

    }

    @Primitive(name = "io_ensure_open", unsafe = UnsafeGroup.IO)
    public static abstract class IOEnsureOpenPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject ensureOpen(VirtualFrame frame, DynamicObject file,
                @Cached("create()") BranchProfile errorProfile) {
            // TODO BJF 13-May-2015 Handle nil case
            final int fd = Layouts.IO.getDescriptor(file);
            if (fd == -1) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().ioError("closed stream", this));
            } else if (fd == -2) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().ioError("shutdown stream", this));
            }
            return nil();
        }

    }


    @Primitive(name = "io_socket_read", lowerFixnum = {1, 2, 3, 4}, unsafe = UnsafeGroup.IO)
    public static abstract class IOSocketReadNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public Object socketRead(DynamicObject io, int length, int flags, int type) {
            final int sockfd = Layouts.IO.getDescriptor(io);

            if (type != 0) {
                throw new UnsupportedOperationException();
            }

            final ByteBuffer buffer = ByteBuffer.allocate(length);
            final int bytesRead = getContext().getThreadManager().runUntilResult(this, () -> ensureSuccessful(nativeSockets().recvfrom(sockfd, buffer, length, flags, PointerPrimitiveNodes.NULL_POINTER, PointerPrimitiveNodes.NULL_POINTER)));
            buffer.position(bytesRead);

            return createString(new ByteList(buffer.array(), buffer.arrayOffset(), buffer.position(), false));
        }

    }

    @Primitive(name = "io_read_if_available", lowerFixnum = 1, unsafe = UnsafeGroup.IO)
    public static abstract class IOReadIfAvailableNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public Object readIfAvailable(DynamicObject file, int numberOfBytes) {
            // Taken from Rubinius's IO::read_if_available.

            if (numberOfBytes == 0) {
                return createString(RopeConstants.EMPTY_ASCII_8BIT_ROPE);
            }

            final int fd = Layouts.IO.getDescriptor(file);

            final FDSet fdSet = getContext().getNativePlatform().createFDSet();
            fdSet.set(fd);

            final Timeval timeoutObject = new DefaultNativeTimeval(jnr.ffi.Runtime.getSystemRuntime());
            timeoutObject.setTime(new long[]{ 0, 0 });

            final int res = ensureSuccessful(nativeSockets().select(fd + 1, fdSet.getPointer(),
                    PointerPrimitiveNodes.NULL_POINTER, PointerPrimitiveNodes.NULL_POINTER, timeoutObject));

            if (res == 0) {
                throw new RaiseException(coreExceptions().eAGAINWaitReadable(this));
            }

            final byte[] bytes = new byte[numberOfBytes];
            final int bytesRead = ensureSuccessful(posix().read(fd, bytes, numberOfBytes));

            if (bytesRead == 0) { // EOF
                return nil();
            }

            return createString(new ByteList(bytes, 0, bytesRead, false));
        }

    }

    @Primitive(name = "io_reopen", unsafe = UnsafeGroup.IO)
    public static abstract class IOReopenPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        private void performReopen(DynamicObject self, DynamicObject target) {
            final int fdSelf = Layouts.IO.getDescriptor(self);
            final int fdTarget = Layouts.IO.getDescriptor(target);

            ensureSuccessful(posix().dup2(fdTarget, fdSelf));

            final int newSelfMode = ensureSuccessful(posix().fcntl(fdSelf, Fcntl.F_GETFL));
            Layouts.IO.setMode(self, newSelfMode);
        }

        @Specialization
        public Object reopen(VirtualFrame frame, DynamicObject file, DynamicObject io) {
            performReopen(file, io);

            resetBufferingNode.call(frame, io, "reset_buffering");

            return nil();
        }

    }

    @Primitive(name = "io_reopen_path", lowerFixnum = 2, unsafe = UnsafeGroup.IO)
    public static abstract class IOReopenPathPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPathPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        public void performReopenPath(DynamicObject self, DynamicObject path, int mode) {
            final int fdSelf = Layouts.IO.getDescriptor(self);
            final int newFdSelf;
            final String targetPathString = StringOperations.getString(path);

            int fdTarget = ensureSuccessful(posix().open(targetPathString, mode, 0_666));

            final int result = posix().dup2(fdTarget, fdSelf);
            if (result == -1) {
                final int errno = posix().errno();
                if (errno == Errno.EBADF.intValue()) {
                    Layouts.IO.setDescriptor(self, fdTarget);
                    newFdSelf = fdTarget;
                } else {
                    if (fdTarget > 0) {
                        ensureSuccessful(posix().close(fdTarget));
                    }
                    ensureSuccessful(result, errno, targetPathString); // throws
                    return;
                }
            } else {
                ensureSuccessful(posix().close(fdTarget));
                newFdSelf = fdSelf;
            }

            final int newSelfMode = ensureSuccessful(posix().fcntl(newFdSelf, Fcntl.F_GETFL));
            Layouts.IO.setMode(self, newSelfMode);
        }

        @Specialization(guards = "isRubyString(path)")
        public Object reopenPath(VirtualFrame frame, DynamicObject file, DynamicObject path, int mode) {
            performReopenPath(file, path, mode);

            resetBufferingNode.call(frame, file, "reset_buffering");

            return nil();
        }

    }

    @Primitive(name = "io_write", unsafe = UnsafeGroup.IO)
    public static abstract class IOWritePrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyString(string)")
        public int write(DynamicObject file, DynamicObject string) {
            final int fd = Layouts.IO.getDescriptor(file);

            final Rope rope = rope(string);

            if (getContext().getDebugStandardOut() != null && fd == STDOUT) {
                getContext().getDebugStandardOut().write(rope.getBytes(), 0, rope.byteLength());
                return rope.byteLength();
            }

            RopeOperations.visitBytes(rope, (bytes, offset, length) -> {
                final ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);

                while (buffer.hasRemaining()) {
                    getContext().getSafepointManager().poll(IOWritePrimitiveNode.this);

                    int written = ensureSuccessful(posix().write(fd, buffer, buffer.remaining()));
                    buffer.position(buffer.position() + written);
                }
            });

            return rope.byteLength();
        }

    }

    @Primitive(name = "io_write_nonblock", unsafe = UnsafeGroup.IO)
    public static abstract class IOWriteNonBlockPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        static class StopWriting extends ControlFlowException {
            private static final long serialVersionUID = 1096318435617097172L;

            final int bytesWritten;

            public StopWriting(int bytesWritten) {
                this.bytesWritten = bytesWritten;
            }
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyString(string)")
        public int writeNonBlock(DynamicObject io, DynamicObject string) {
            setNonBlocking(io);

            final int fd = Layouts.IO.getDescriptor(io);
            final Rope rope = rope(string);

            if (getContext().getDebugStandardOut() != null && fd == STDOUT) {
                // TODO (eregon, 13 May 2015): this looks like it would block
                getContext().getDebugStandardOut().write(rope.getBytes(), 0, rope.byteLength());
                return rope.byteLength();
            }

            final IOWriteNonBlockPrimitiveNode currentNode = this;

            try {
                RopeOperations.visitBytes(rope, new BytesVisitor() {

                    int totalWritten = 0;

                    @Override
                    public void accept(byte[] bytes, int offset, int length) {
                        final ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);

                        while (buffer.hasRemaining()) {
                            getContext().getSafepointManager().poll(currentNode);

                            final int result = posix().write(fd, buffer, buffer.remaining());
                            if (result <= 0) {
                                int errno = posix().errno();
                                if (errno == Errno.EAGAIN.intValue() || errno == Errno.EWOULDBLOCK.intValue()) {
                                    throw new RaiseException(coreExceptions().eAGAINWaitWritable(currentNode));
                                } else {
                                    ensureSuccessful(result);
                                }
                            } else {
                                totalWritten += result;
                            }

                            if (result < buffer.remaining()) {
                                throw new StopWriting(totalWritten);
                            }

                            buffer.position(buffer.position() + result);
                        }
                    }

                });
            } catch (StopWriting e) {
                return e.bytesWritten;
            }

            return rope.byteLength();
        }

        protected void setNonBlocking(DynamicObject io) {
            final int fd = Layouts.IO.getDescriptor(io);
            int flags = ensureSuccessful(posix().fcntl(fd, Fcntl.F_GETFL));

            if ((flags & OpenFlags.O_NONBLOCK.intValue()) == 0) {
                flags |= OpenFlags.O_NONBLOCK.intValue();
                ensureSuccessful(posix().fcntlInt(fd, Fcntl.F_SETFL, flags));
                Layouts.IO.setMode(io, flags);
            }
        }

    }

    @Primitive(name = "io_close", unsafe = UnsafeGroup.IO)
    public static abstract class IOClosePrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode ensureOpenNode;

        public IOClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            ensureOpenNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public int close(VirtualFrame frame, DynamicObject io) {
            ensureOpenNode.call(frame, io, "ensure_open");

            final int fd = Layouts.IO.getDescriptor(io);

            if (fd == -1) {
                return 0;
            }

            int newDescriptor = -1;
            Layouts.IO.setDescriptor(io, newDescriptor);

            if (fd < 3) {
                return 0;
            }

            ensureSuccessful(posix().close(fd));

            return 0;
        }

    }

    @Primitive(name = "io_seek", lowerFixnum = { 1, 2 }, unsafe = UnsafeGroup.IO)
    public static abstract class IOSeekPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @Specialization
        public int seek(DynamicObject io, int amount, int whence) {
            final int fd = Layouts.IO.getDescriptor(io);
            return ensureSuccessful(posix().lseek(fd, amount, whence));
        }

    }

    @Primitive(name = "io_accept", unsafe = UnsafeGroup.IO)
    public abstract static class AcceptNode extends IOPrimitiveArrayArgumentsNode {

        @SuppressWarnings("restriction")
        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public int accept(DynamicObject io) {
            final int fd = Layouts.IO.getDescriptor(io);

            final int[] addressLength = { 16 };
            final long address = UnsafeHolder.U.allocateMemory(addressLength[0]);

            final int newFd;

            try {
                newFd = ensureSuccessful(nativeSockets().accept(fd, memoryManager().newPointer(address), addressLength));
            } finally {
                UnsafeHolder.U.freeMemory(address);
            }

            return newFd;
        }

    }

    @Primitive(name = "io_sysread", unsafe = UnsafeGroup.IO, lowerFixnum = 1)
    public static abstract class IOSysReadPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public DynamicObject sysread(DynamicObject file, int length) {
            final int fd = Layouts.IO.getDescriptor(file);

            final ByteBuffer buffer = ByteBuffer.allocate(length);

            int toRead = length;

            while (toRead > 0) {
                getContext().getSafepointManager().poll(this);

                final int bytesRead = ensureSuccessful(posix().read(fd, buffer, toRead));

                if (bytesRead == 0) { // EOF
                    if (toRead == length) { // if EOF at first iteration
                        return nil();
                    } else {
                        break;
                    }
                }

                buffer.position(bytesRead);
                toRead -= bytesRead;
            }

            return createString(new ByteList(buffer.array(), buffer.arrayOffset(), buffer.position(), false));
        }

    }

    @Primitive(name = "io_select", needsSelf = false, lowerFixnum = 4, unsafe = UnsafeGroup.IO)
    public static abstract class IOSelectPrimitiveNode extends IOPrimitiveArrayArgumentsNode {

        public abstract Object executeSelect(DynamicObject readables, DynamicObject writables, DynamicObject errorables, Object Timeout);

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isNil(noTimeout)")
        public Object select(DynamicObject readables, DynamicObject writables, DynamicObject errorables, DynamicObject noTimeout) {
            Object result;
            do {
                result = executeSelect(readables, writables, errorables, Integer.MAX_VALUE);
            } while (result == nil());
            return result;
        }

        @Specialization(guards = { "isRubyArray(readables)", "isNilOrEmpty(writables)", "isNilOrEmpty(errorables)" })
        public Object selectReadables(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeoutMicros) {
            return selectOneSet(readables, timeoutMicros, 1);
        }

        @Specialization(guards = { "isNilOrEmpty(readables)", "isRubyArray(writables)", "isNilOrEmpty(errorables)" })
        public Object selectWritables(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeoutMicros) {
            return selectOneSet(writables, timeoutMicros, 2);
        }

        @Specialization(guards = { "isNilOrEmpty(readables)", "isNilOrEmpty(writables)", "isRubyArray(errorables)" })
        public Object selectErrorables(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeoutMicros) {
            return selectOneSet(errorables, timeoutMicros, 3);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        private Object selectOneSet(DynamicObject setToSelect, final int timeoutMicros, int setNb) {
            assert setNb >= 1 && setNb <= 3;
            final Object[] readableObjects = ArrayOperations.toObjectArray(setToSelect);
            final int[] fds = getFileDescriptors(setToSelect);
            final int nfds = max(fds) + 1;

            final FDSet fdSet = getContext().getNativePlatform().createFDSet();

            final ThreadManager.ResultOrTimeout<Integer> resultOrTimeout = getContext().getThreadManager().runUntilTimeout(this, timeoutMicros, new ThreadManager.BlockingTimeoutAction<Integer>() {
                @Override
                public Integer block(Timeval timeoutToUse) throws InterruptedException {
                    // Set each fd each time since they are removed if the fd was not available
                    for (int fd : fds) {
                        fdSet.set(fd);
                    }
                    final int result = callSelect(nfds, fdSet, timeoutToUse);

                    if (result == 0 && timeoutMicros != 0) {
                        // interrupted, try again
                        return null;
                    } else {
                        // result == 0: nothing was ready
                        // result >  0: some were ready
                        return result;
                    }
                }

                private int callSelect(int nfds, FDSet fdSet, Timeval timeoutToUse) {
                    return nativeSockets().select(
                            nfds,
                            setNb == 1 ? fdSet.getPointer() : PointerPrimitiveNodes.NULL_POINTER,
                            setNb == 2 ? fdSet.getPointer() : PointerPrimitiveNodes.NULL_POINTER,
                            setNb == 3 ? fdSet.getPointer() : PointerPrimitiveNodes.NULL_POINTER,
                            timeoutToUse);
                }
            });

            if (resultOrTimeout instanceof ThreadManager.TimedOut) {
                return nil();
            }

            final ResultWithinTime<Integer> result = (ThreadManager.ResultWithinTime<Integer>) resultOrTimeout;
            final int resultCode = ensureSuccessful(result.getValue());

            if (resultCode == 0) {
                return nil();
            }

            return createArray(new Object[] {
                    setNb == 1 ? getSetObjects(readableObjects, fds, fdSet) : createEmptyArray(),
                    setNb == 2 ? getSetObjects(readableObjects, fds, fdSet) : createEmptyArray(),
                    setNb == 3 ? getSetObjects(readableObjects, fds, fdSet) : createEmptyArray()
            }, 3);
        }

        public DynamicObject createEmptyArray() {
            return createArray(null, 0);
        }

        private int[] getFileDescriptors(DynamicObject fileDescriptorArray) {
            assert RubyGuards.isRubyArray(fileDescriptorArray);

            final Object[] objects = ArrayOperations.toObjectArray(fileDescriptorArray);

            final int[] fileDescriptors = new int[objects.length];

            for (int n = 0; n < objects.length; n++) {
                if (!(objects[n] instanceof DynamicObject)) {
                    throw new UnsupportedOperationException();
                }

                fileDescriptors[n] = Layouts.IO.getDescriptor((DynamicObject) objects[n]);
            }

            return fileDescriptors;
        }

        private static int max(int[] values) {
            assert values.length > 0;

            int max = Integer.MIN_VALUE;

            for (int n = 0; n < values.length; n++) {
                max = Math.max(max, values[n]);
            }

            return max;
        }

        private DynamicObject getSetObjects(Object[] objects, int[] fds, FDSet set) {
            final Object[] setObjects = new Object[objects.length];
            int setFdsCount = 0;

            for (int n = 0; n < objects.length; n++) {
                if (set.isSet(fds[n])) {
                    setObjects[setFdsCount] = objects[n];
                    setFdsCount++;
                }
            }

            return createArray(setObjects, setFdsCount);
        }

        protected boolean isNilOrEmpty(DynamicObject fds) {
            return isNil(fds) || (RubyGuards.isRubyArray(fds) && ArrayGuards.isEmptyArray(fds));
        }

    }

}
