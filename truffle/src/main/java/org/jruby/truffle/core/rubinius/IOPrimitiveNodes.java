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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.posix.DefaultNativeTimeval;
import jnr.posix.Timeval;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.rope.BytesVisitor;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.platform.posix.FDSet;
import org.jruby.util.ByteList;
import org.jruby.util.Dir;
import org.jruby.util.unsafe.UnsafeHolder;

import java.nio.ByteBuffer;

import static org.jruby.truffle.core.string.StringOperations.rope;

public abstract class IOPrimitiveNodes {

    public static abstract class IORubiniusPrimitiveArrayArgumentsNode extends RubiniusPrimitiveArrayArgumentsNode {

        public IORubiniusPrimitiveArrayArgumentsNode() {
        }

        public IORubiniusPrimitiveArrayArgumentsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected int ensureSuccessful(int result) {
            assert result >= -1;
            if (result == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().errnoError(posix().errno(), this));
            }
            return result;
        }
    }

    private static int STDOUT = 1;

    @RubiniusPrimitive(name = "io_allocate", unsafe = UnsafeGroup.IO)
    public static abstract class IOAllocatePrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode newBufferNode;
        @Child private AllocateObjectNode allocateNode;

        public IOAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            newBufferNode = DispatchHeadNodeFactory.createMethodCall(context);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(VirtualFrame frame, DynamicObject classToAllocate) {
            final DynamicObject buffer = (DynamicObject) newBufferNode.call(frame, coreLibrary().getInternalBufferClass(), "new", null);
            return allocateNode.allocate(classToAllocate, buffer, 0, 0, 0);
        }

    }

    @RubiniusPrimitive(name = "io_connect_pipe", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOConnectPipeNode extends IORubiniusPrimitiveArrayArgumentsNode {

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

        @TruffleBoundary
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

    @RubiniusPrimitive(name = "io_open", needsSelf = false, lowerFixnumParameters = { 1, 2 }, unsafe = UnsafeGroup.IO)
    public static abstract class IOOpenPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyString(path)")
        public int open(DynamicObject path, int mode, int permission) {
            return ensureSuccessful(posix().open(StringOperations.getString(getContext(), path), mode, permission));
        }

    }

    @RubiniusPrimitive(name = "io_truncate", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOTruncatePrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyString(path)")
        public int truncate(DynamicObject path, long length) {
            return ensureSuccessful(posix().truncate(StringOperations.getString(getContext(), path), length));
        }

    }

    @RubiniusPrimitive(name = "io_ftruncate", unsafe = UnsafeGroup.IO)
    public static abstract class IOFTruncatePrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public int ftruncate(VirtualFrame frame, DynamicObject io, long length) {
            final int fd = Layouts.IO.getDescriptor(io);
            return ensureSuccessful(posix().ftruncate(fd, length));
        }

    }

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false, unsafe = UnsafeGroup.IO)
    public static abstract class IOFNMatchPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

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

    @RubiniusPrimitive(name = "io_ensure_open", unsafe = UnsafeGroup.IO)
    public static abstract class IOEnsureOpenPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject ensureOpen(VirtualFrame frame, DynamicObject file) {
            // TODO BJF 13-May-2015 Handle nil case
            final int fd = Layouts.IO.getDescriptor(file);
            if (fd == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().ioError("closed stream", this));
            } else if (fd == -2) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().ioError("shutdown stream", this));
            }
            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_read_if_available", lowerFixnumParameters = 0, unsafe = UnsafeGroup.IO)
    public static abstract class IOReadIfAvailableNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object readIfAvailable(DynamicObject file, int numberOfBytes) {
            // Taken from Rubinius's IO::read_if_available.

            if (numberOfBytes == 0) {
                return createString(RopeConstants.EMPTY_ASCII_8BIT_ROPE);
            }

            final int fd = Layouts.IO.getDescriptor(file);

            final FDSet fdSet = new FDSet();
            fdSet.set(fd);

            final Timeval timeoutObject = new DefaultNativeTimeval(jnr.ffi.Runtime.getSystemRuntime());
            timeoutObject.setTime(new long[]{ 0, 0 });

            final int res = ensureSuccessful(nativeSockets().select(fd + 1, fdSet.getPointer(),
                    PointerPrimitiveNodes.NULL_POINTER, PointerPrimitiveNodes.NULL_POINTER, timeoutObject));

            if (res == 0) {
                throw new RaiseException(
                        Layouts.CLASS.getInstanceFactory(coreLibrary().getEagainWaitReadable()).newInstance(
                            coreStrings().RESOURCE_TEMP_UNAVAIL.createInstance(),
                            Errno.EAGAIN.intValue()));
            }

            final byte[] bytes = new byte[numberOfBytes];
            final int bytesRead = ensureSuccessful(posix().read(fd, bytes, numberOfBytes));

            if (bytesRead == 0) { // EOF
                return nil();
            }

            return createString(new ByteList(bytes, 0, bytesRead, false));
        }

    }

    @RubiniusPrimitive(name = "io_reopen", unsafe = UnsafeGroup.IO)
    public static abstract class IOReopenPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary
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

            resetBufferingNode.call(frame, io, "reset_buffering", null);

            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_reopen_path", lowerFixnumParameters = 1, unsafe = UnsafeGroup.IO)
    public static abstract class IOReopenPathPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPathPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary
        public void performReopenPath(DynamicObject file, DynamicObject path, int mode) {
            int fd = Layouts.IO.getDescriptor(file);
            final String pathString = StringOperations.getString(getContext(), path);

            int otherFd = ensureSuccessful(posix().open(pathString, mode, 666));

            final int result = posix().dup2(otherFd, fd);
            if (result == -1) {
                final int errno = posix().errno();
                if (errno == Errno.EBADF.intValue()) {
                    Layouts.IO.setDescriptor(file, otherFd);
                    fd = otherFd;
                } else {
                    if (otherFd > 0) {
                        ensureSuccessful(posix().close(otherFd));
                    }
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().errnoError(errno, this));
                }

            } else {
                ensureSuccessful(posix().close(otherFd));
            }


            final int newMode = ensureSuccessful(posix().fcntl(fd, Fcntl.F_GETFL));
            Layouts.IO.setMode(file, newMode);
        }

        @Specialization(guards = "isRubyString(path)")
        public Object reopenPath(VirtualFrame frame, DynamicObject file, DynamicObject path, int mode) {
            performReopenPath(file, path, mode);

            resetBufferingNode.call(frame, file, "reset_buffering", null);

            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_write", unsafe = UnsafeGroup.IO)
    public static abstract class IOWritePrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public int write(DynamicObject file, DynamicObject string) {
            final int fd = Layouts.IO.getDescriptor(file);

            final Rope rope = rope(string);

            if (getContext().getDebugStandardOut() != null && fd == STDOUT) {
                getContext().getDebugStandardOut().write(rope.getBytes(), 0, rope.byteLength());
                return rope.byteLength();
            }

            RopeOperations.visitBytes(rope, new BytesVisitor() {

                @Override
                public void accept(byte[] bytes, int offset, int length) {
                    final ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);

                    while (buffer.hasRemaining()) {
                        getContext().getSafepointManager().poll(IOWritePrimitiveNode.this);

                        int written = ensureSuccessful(posix().write(fd, buffer, buffer.remaining()));
                        buffer.position(buffer.position() + written);
                    }
                }

            });

            return rope.byteLength();
        }

    }

    @RubiniusPrimitive(name = "io_close", unsafe = UnsafeGroup.IO)
    public static abstract class IOClosePrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode ensureOpenNode;

        public IOClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            ensureOpenNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public int close(VirtualFrame frame, DynamicObject io) {
            ensureOpenNode.call(frame, io, "ensure_open", null);

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

    @RubiniusPrimitive(name = "io_seek", lowerFixnumParameters = { 0, 1 }, unsafe = UnsafeGroup.IO)
    public static abstract class IOSeekPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public int seek(VirtualFrame frame, DynamicObject io, int amount, int whence) {
            final int fd = Layouts.IO.getDescriptor(io);
            // TODO (pitr-ch 15-Apr-2016): should it have ensureSuccessful too?
            return posix().lseek(fd, amount, whence);
        }

    }

    @RubiniusPrimitive(name = "io_accept", unsafe = UnsafeGroup.IO)
    public abstract static class AcceptNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @SuppressWarnings("restriction")
        @TruffleBoundary
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

    @RubiniusPrimitive(name = "io_sysread", unsafe = UnsafeGroup.IO)
    public static abstract class IOSysReadPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject sysread(VirtualFrame frame, DynamicObject file, int length) {
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

    @RubiniusPrimitive(name = "io_select", needsSelf = false, lowerFixnumParameters = 3, unsafe = UnsafeGroup.IO)
    public static abstract class IOSelectPrimitiveNode extends IORubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubyArray(readables)", "isNil(writables)", "isNil(errorables)", "isNil(noTimeout)" })
        public Object select(DynamicObject readables, DynamicObject writables, DynamicObject errorables, DynamicObject noTimeout) {
            Object result;
            do {
                result = select(readables, writables, errorables, Integer.MAX_VALUE);
            } while (result == nil());
            return result;
        }

        @TruffleBoundary
        @Specialization(guards = { "isRubyArray(readables)", "isNil(writables)", "isNil(errorables)" })
        public Object select(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeoutMicros) {
            final Object[] readableObjects = ArrayOperations.toObjectArray(readables);
            final int[] readableFds = getFileDescriptors(readables);
            final int nfds = max(readableFds) + 1;

            final FDSet readableSet = new FDSet();

            final ThreadManager.ResultOrTimeout<Integer> result = getContext().getThreadManager().runUntilTimeout(this, timeoutMicros, new ThreadManager.BlockingTimeoutAction<Integer>() {
                @Override
                public Integer block(Timeval timeoutToUse) throws InterruptedException {
                    // Set each fd each time since they are removed if the fd was not available
                    for (int fd : readableFds) {
                        readableSet.set(fd);
                    }
                    final int result = callSelect(nfds, readableSet, timeoutToUse);

                    if (result == 0) {
                        return null;
                    }

                    return result;
                }

                private int callSelect(int nfds, FDSet readableSet, Timeval timeoutToUse) {
                    return nativeSockets().select(
                            nfds,
                            readableSet.getPointer(),
                            PointerPrimitiveNodes.NULL_POINTER,
                            PointerPrimitiveNodes.NULL_POINTER,
                            timeoutToUse);
                }
            });

            if (result instanceof ThreadManager.TimedOut) {
                return nil();
            }

            final int resultCode = ensureSuccessful(((ThreadManager.ResultWithinTime<Integer>) result).getValue());

            if (resultCode == 0) {
                return nil();
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{
                            getSetObjects(readableObjects, readableFds, readableSet),
                            Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0),
                            Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0) },
                    3);
        }

        @TruffleBoundary
        @Specialization(guards = { "isNil(readables)", "isRubyArray(writables)", "isNil(errorables)" })
        public Object selectNilReadables(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeout) {
            final Object[] writableObjects = ArrayOperations.toObjectArray(writables);
            final int[] writableFds = getFileDescriptors(writables);
            final int nfds = max(writableFds) + 1;

            final FDSet writableSet = new FDSet();


            final int result = getContext().getThreadManager().runUntilResult(this, new ThreadManager.BlockingAction<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    // Set each fd each time since they are removed if the fd was not available
                    for (int fd : writableFds) {
                        writableSet.set(fd);
                    }
                    return callSelect(nfds, writableSet);
                }

                private int callSelect(int nfds, FDSet writableSet) {
                    return nativeSockets().select(
                            nfds,
                            PointerPrimitiveNodes.NULL_POINTER,
                            writableSet.getPointer(),
                            PointerPrimitiveNodes.NULL_POINTER,
                            null);
                }
            });

            ensureSuccessful(result);

            assert result != 0;

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{
                            Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0),
                            getSetObjects(writableObjects, writableFds, writableSet),
                            Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0) },
                    3);
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

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), setObjects, setFdsCount);
        }

    }

}
