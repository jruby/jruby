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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.ffi.Pointer;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.sockets.FDSet;
import org.jruby.truffle.runtime.sockets.FDSetFactory;
import org.jruby.truffle.runtime.sockets.FDSetFactoryFactory;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.ByteList;
import org.jruby.util.Dir;
import org.jruby.util.StringSupport;
import org.jruby.util.unsafe.UnsafeHolder;

import java.nio.ByteBuffer;

public abstract class IOPrimitiveNodes {

    private static int STDOUT = 1;

    @RubiniusPrimitive(name = "io_allocate")
    public static abstract class IOAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode newBufferNode;

        public IOAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newBufferNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public DynamicObject allocate(VirtualFrame frame, DynamicObject classToAllocate) {
            final DynamicObject buffer = (DynamicObject) newBufferNode.call(frame, getContext().getCoreLibrary().getInternalBufferClass(), "new", null);
            return Layouts.IO.createIO(Layouts.CLASS.getInstanceFactory(classToAllocate), buffer, 0, 0, 0);
        }

    }

    @RubiniusPrimitive(name = "io_connect_pipe", needsSelf = false)
    public static abstract class IOConnectPipeNode extends RubiniusPrimitiveNode {

        private final int RDONLY;
        private final int WRONLY;

        public IOConnectPipeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            RDONLY = (int) context.getRubiniusConfiguration().get("rbx.platform.file.O_RDONLY");
            WRONLY = (int) context.getRubiniusConfiguration().get("rbx.platform.file.O_WRONLY");
        }

        @Specialization
        public boolean connectPipe(VirtualFrame frame, DynamicObject lhs, DynamicObject rhs) {
            final int[] fds = new int[2];

            if (posix().pipe(fds) == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            newOpenFd(fds[0]);
            newOpenFd(fds[1]);

            Layouts.IO.setDescriptor(lhs, fds[0]);
            Layouts.IO.setMode(lhs, RDONLY);

            Layouts.IO.setDescriptor(rhs, fds[1]);
            Layouts.IO.setMode(rhs, WRONLY);

            return true;
        }

        @TruffleBoundary
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

        @Specialization(guards = "isRubyString(path)")
        public int open(DynamicObject path, int mode, int permission) {
            return posix().open(StringOperations.getString(path), mode, permission);
        }

    }


    @RubiniusPrimitive(name = "io_truncate", needsSelf = false)
    public static abstract class IOTruncatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOTruncatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(path)")
        public int truncate(DynamicObject path, int length) {
            return truncate(path, (long) length);
        }

        @Specialization(guards = "isRubyString(path)")
        public int truncate(DynamicObject path, long length) {
            final int result = posix().truncate(StringOperations.getString(path), length);
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
        public int ftruncate(VirtualFrame frame, DynamicObject io, int length) {
            return ftruncate(frame, io, (long) length);
        }

        @Specialization
        public int ftruncate(VirtualFrame frame, DynamicObject io, long length) {
            final int fd = Layouts.IO.getDescriptor(io);
            return posix().ftruncate(fd, length);
        }

    }

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false)
    public static abstract class IOFNMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public IOFNMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(pattern)", "isRubyString(path)"})
        public boolean fnmatch(DynamicObject pattern, DynamicObject path, int flags) {
            final ByteList patternBytes = Layouts.STRING.getByteList(pattern);
            final ByteList pathBytes = Layouts.STRING.getByteList(path);
            return Dir.fnmatch(patternBytes.getUnsafeBytes(),
                    patternBytes.getBegin(),
                    patternBytes.getBegin() + patternBytes.getRealSize(),
                    pathBytes.getUnsafeBytes(),
                    pathBytes.getBegin(),
                    pathBytes.getBegin() + pathBytes.getRealSize(),
                    flags) != Dir.FNM_NOMATCH;
        }

    }

    @RubiniusPrimitive(name = "io_ensure_open")
    public static abstract class IOEnsureOpenPrimitiveNode extends RubiniusPrimitiveNode {

        public IOEnsureOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject ensureOpen(VirtualFrame frame, DynamicObject file) {
            // TODO BJF 13-May-2015 Handle nil case
            final int fd = Layouts.IO.getDescriptor(file);
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

    @RubiniusPrimitive(name = "io_read_if_available")
    public static abstract class IOReadIfAvailableNode extends RubiniusPrimitiveNode {

        private static final FDSetFactory fdSetFactory = FDSetFactoryFactory.create();

        public IOReadIfAvailableNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object readIfAvailable(DynamicObject file, int numberOfBytes) {
            // Taken from Rubinius's IO::read_if_available.

            if (numberOfBytes == 0) {
                return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(), StringSupport.CR_UNKNOWN, null);
            }

            final int fd = Layouts.IO.getDescriptor(file);

            final FDSet fdSet = fdSetFactory.create();
            fdSet.set(fd);

            final Pointer timeout = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().allocateDirect(8 * 2); // Needs to be two longs.
            timeout.putLong(0, 0);
            timeout.putLong(8, 0);

            final int res = nativeSockets().select(fd + 1, fdSet.getPointer(), PointerNodes.NULL_POINTER, PointerNodes.NULL_POINTER, timeout);

            if (res == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(Errno.EAGAIN.intValue(), this));
            }

            if (res < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(getContext().getPosix().errno(), this));
            }

            final byte[] bytes = new byte[numberOfBytes];
            final int bytesRead = getContext().getPosix().read(fd, bytes, numberOfBytes);

            if (bytesRead == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(getContext().getPosix().errno(), this));
            }

            if (bytesRead == 0) {
                return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(), StringSupport.CR_UNKNOWN, null);
            }

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(bytes), StringSupport.CR_UNKNOWN, null);
        }

    }

    @RubiniusPrimitive(name = "io_reopen")
    public static abstract class IOReopenPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary
        private void performReopen(DynamicObject file, DynamicObject io) {
            final int fd = Layouts.IO.getDescriptor(file);
            final int fdOther = Layouts.IO.getDescriptor(io);

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
            Layouts.IO.setMode(file, mode);
        }

        @Specialization
        public Object reopen(VirtualFrame frame, DynamicObject file, DynamicObject io) {
            performReopen(file, io);

            resetBufferingNode.call(frame, io, "reset_buffering", null);

            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_reopen_path")
    public static abstract class IOReopenPathPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPathPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @TruffleBoundary
        public void performReopenPath(DynamicObject file, DynamicObject path, int mode) {
            int fd = Layouts.IO.getDescriptor(file);
            final String pathString = StringOperations.getString(path);

            int otherFd = posix().open(pathString, mode, 666);
            if (otherFd < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            final int result = posix().dup2(otherFd, fd);
            if (result == -1) {
                final int errno = posix().errno();
                if (errno == Errno.EBADF.intValue()) {
                    Layouts.IO.setDescriptor(file, otherFd);
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
            Layouts.IO.setMode(file, newMode);
        }

        @Specialization(guards = "isRubyString(path)")
        public Object reopenPath(VirtualFrame frame, DynamicObject file, DynamicObject path, int mode) {
            performReopenPath(file, path, mode);

            resetBufferingNode.call(frame, file, "reset_buffering", null);

            return nil();
        }

    }

    @RubiniusPrimitive(name = "io_write")
    public static abstract class IOWritePrimitiveNode extends RubiniusPrimitiveNode {

        public IOWritePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(string)")
        public int write(DynamicObject file, DynamicObject string) {
            final int fd = Layouts.IO.getDescriptor(file);

            final ByteList byteList = Layouts.STRING.getByteList(string);

            if (getContext().getDebugStandardOut() != null && fd == STDOUT) {
                getContext().getDebugStandardOut().write(byteList.unsafeBytes(), byteList.begin(), byteList.length());
                return byteList.length();
            }

            // TODO (eregon, 11 May 2015): review consistency under concurrent modification
            final ByteBuffer buffer = ByteBuffer.wrap(byteList.unsafeBytes(), byteList.begin(), byteList.length());

            int total = 0;

            while (buffer.hasRemaining()) {
                getContext().getSafepointManager().poll(this);

                int written = posix().write(fd, buffer, buffer.remaining());

                if (written == -1) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
                }

                buffer.position(buffer.position() + written);

                total += written;
            }

            return total;
        }

    }

    @RubiniusPrimitive(name = "io_close")
    public static abstract class IOClosePrimitiveNode extends RubiniusPrimitiveNode {

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
        public int seek(VirtualFrame frame, DynamicObject io, int amount, int whence) {
            final int fd = Layouts.IO.getDescriptor(io);
            return posix().lseek(fd, amount, whence);
        }

    }

    @RubiniusPrimitive(name = "io_accept")
    public abstract static class AcceptNode extends RubiniusPrimitiveNode {

        public AcceptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int accept(DynamicObject io) {
            final int fd = Layouts.IO.getDescriptor(io);

            final int[] addressLength = {16};
            final long address = UnsafeHolder.U.allocateMemory(addressLength[0]);

            final int newFd;

            try {
                newFd = nativeSockets().accept(fd, getMemoryManager().newPointer(address), addressLength);
            } finally {
                UnsafeHolder.U.freeMemory(address);
            }

            if (newFd == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            return newFd;
        }

    }

    @RubiniusPrimitive(name = "io_sysread")
    public static abstract class IOSysReadPrimitiveNode extends RubiniusPrimitiveNode {

        public IOSysReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject sysread(VirtualFrame frame, DynamicObject file, int length) {
            final int fd = Layouts.IO.getDescriptor(file);

            final ByteBuffer buffer = ByteBuffer.allocate(length);

            int toRead = length;

            while (toRead > 0) {
                getContext().getSafepointManager().poll(this);

                final int readIteration = posix().read(fd, buffer, toRead);
                buffer.position(readIteration);
                toRead -= readIteration;
            }

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(buffer.array()), StringSupport.CR_UNKNOWN, null);
        }

    }

    @RubiniusPrimitive(name = "io_select", needsSelf = false)
    public static abstract class IOSelectPrimitiveNode extends RubiniusPrimitiveNode {

        private static final FDSetFactory fdSetFactory = FDSetFactoryFactory.create();

        public IOSelectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyArray(readables)", "isNil(writables)", "isNil(errorables)"})
        public Object select(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeout) {
            final Object[] readableObjects = ArrayOperations.toObjectArray(readables);
            final int[] readableFds = getFileDescriptors(readables);

            final FDSet readableSet = fdSetFactory.create();

            for (int fd : readableFds) {
                readableSet.set(fd);
            }

            final int result = getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingAction<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    return nativeSockets().select(
                            max(readableFds) + 1,
                            readableSet.getPointer(),
                            PointerNodes.NULL_POINTER,
                            PointerNodes.NULL_POINTER,
                            PointerNodes.NULL_POINTER);
                }
            });

            if (result == -1) {
                return nil();
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{
                    getSetObjects(readableObjects, readableFds, readableSet),
                    Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0),
                    Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0)},
                    3);
        }

        @TruffleBoundary
        @Specialization(guards = { "isNil(readables)", "isRubyArray(writables)", "isNil(errorables)" })
        public Object selectNilReadables(DynamicObject readables, DynamicObject writables, DynamicObject errorables, int timeout) {
            final Object[] writableObjects = ArrayOperations.toObjectArray(writables);
            final int[] writableFds = getFileDescriptors(writables);

            final FDSet writableSet = fdSetFactory.create();

            for (int fd : writableFds) {
                writableSet.set(fd);
            }

            final int result = getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingAction<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    return nativeSockets().select(
                            max(writableFds) + 1,
                            PointerNodes.NULL_POINTER,
                            writableSet.getPointer(),
                            PointerNodes.NULL_POINTER,
                            PointerNodes.NULL_POINTER);
                }
            });

            if (result == -1) {
                return nil();
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{
                            Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0),
                            getSetObjects(writableObjects, writableFds, writableSet),
                            Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0)},
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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), setObjects, setFdsCount);
        }

    }

}
