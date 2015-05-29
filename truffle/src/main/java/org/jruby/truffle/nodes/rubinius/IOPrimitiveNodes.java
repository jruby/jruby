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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import jnr.constants.platform.Errno;
import jnr.constants.platform.Fcntl;
import jnr.ffi.byref.IntByReference;

import org.jruby.RubyEncoding;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.sockets.FDSet;
import org.jruby.truffle.runtime.sockets.FDSetFactory;
import org.jruby.truffle.runtime.sockets.FDSetFactoryFactory;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.ByteList;
import org.jruby.util.Dir;
import org.jruby.util.unsafe.UnsafeHolder;

import java.nio.ByteBuffer;
import java.util.EnumSet;

public abstract class IOPrimitiveNodes {

    private static int STDOUT = 1;

    private static final String IBUFFER_IDENTIFIER = "@ibuffer";
    private static final Property IBUFFER_PROPERTY;

    private static final String LINENO_IDENTIFIER = "@lineno";
    private static final Property LINENO_PROPERTY;

    private static final String DESCRIPTOR_IDENTIFIER = "@descriptor";
    private static final Property DESCRIPTOR_PROPERTY;

    private static final String MODE_IDENTIFIER = "@mode";
    private static final Property MODE_PROPERTY;

    private static final DynamicObjectFactory IO_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        IBUFFER_PROPERTY = Property.create(IBUFFER_IDENTIFIER, allocator.locationForType(RubyBasicObject.class, EnumSet.of(LocationModifier.NonNull)), 0);
        LINENO_PROPERTY = Property.create(LINENO_IDENTIFIER, allocator.locationForType(Integer.class, EnumSet.of(LocationModifier.NonNull)), 0);
        DESCRIPTOR_PROPERTY = Property.create(DESCRIPTOR_IDENTIFIER, allocator.locationForType(Integer.class, EnumSet.of(LocationModifier.NonNull)), 0);
        MODE_PROPERTY = Property.create(MODE_IDENTIFIER, allocator.locationForType(Integer.class, EnumSet.of(LocationModifier.NonNull)), 0);

        IO_FACTORY = RubyBasicObject.EMPTY_SHAPE
                .addProperty(IBUFFER_PROPERTY)
                .addProperty(LINENO_PROPERTY)
                .addProperty(DESCRIPTOR_PROPERTY)
                .addProperty(MODE_PROPERTY)
                .createFactory();
    }

    public static class IOAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass, IO_FACTORY.newInstance(context.getCoreLibrary().getNilObject(), 0, 0, context.getCoreLibrary().getNilObject()));
        }
    }

    public static int getDescriptor(RubyBasicObject io) {
        assert io.getDynamicObject().getShape().hasProperty(DESCRIPTOR_IDENTIFIER);
        return (int) DESCRIPTOR_PROPERTY.get(io.getDynamicObject(), true);
    }

    public static void setDescriptor(RubyBasicObject io, int newDescriptor) {
        assert io.getDynamicObject().getShape().hasProperty(DESCRIPTOR_IDENTIFIER);

        try {
            DESCRIPTOR_PROPERTY.set(io.getDynamicObject(), newDescriptor, io.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static void setMode(RubyBasicObject io, int newMode) {
        assert io.getDynamicObject().getShape().hasProperty(MODE_IDENTIFIER);

        try {
            MODE_PROPERTY.set(io.getDynamicObject(), newMode, io.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @RubiniusPrimitive(name = "io_allocate")
    public static abstract class IOAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode newBufferNode;

        public IOAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newBufferNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public RubyBasicObject allocate(VirtualFrame frame, RubyClass classToAllocate) {
            final Object buffer = newBufferNode.call(frame, getContext().getCoreLibrary().getIOBufferClass(), "new", null);
            return new RubyBasicObject(classToAllocate, IO_FACTORY.newInstance(buffer, 0, 0, nil()));
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
        public boolean connectPipe(VirtualFrame frame, RubyBasicObject lhs, RubyBasicObject rhs) {
            final int[] fds = new int[2];

            if (posix().pipe(fds) == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix().errno(), this));
            }

            newOpenFd(fds[0]);
            newOpenFd(fds[1]);

            setDescriptor(lhs, fds[0]);
            setMode(lhs, RDONLY);

            setDescriptor(rhs, fds[1]);
            setMode(rhs, WRONLY);

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
            final int fd = getDescriptor(io);
            return posix().ftruncate(fd, length);
        }

    }

    @RubiniusPrimitive(name = "io_fnmatch", needsSelf = false)
    public static abstract class IOFNMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public IOFNMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
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
            final int fd = getDescriptor(file);
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

        @Child private CallDispatchHeadNode resetBufferingNode;

        public IOReopenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            resetBufferingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object reopen(VirtualFrame frame, RubyBasicObject file, RubyBasicObject io) {
            final int fd = getDescriptor(file);
            final int fdOther = getDescriptor(io);

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
            setMode(file, mode);

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

        @Specialization
        public Object reopenPath(VirtualFrame frame, RubyBasicObject file, RubyString path, int mode) {
            int fd = getDescriptor(file);
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
                    setDescriptor(file, otherFd);
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
            setMode(file, newMode);

            resetBufferingNode.call(frame, file, "reset_buffering", null);

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
            final int fd = getDescriptor(file);

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

        @Child private CallDispatchHeadNode ensureOpenNode;

        public IOClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            ensureOpenNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public int close(VirtualFrame frame, RubyBasicObject io) {
            ensureOpenNode.call(frame, io, "ensure_open", null);

            final int fd = getDescriptor(io);

            if (fd == -1) {
                return 0;
            }

            setDescriptor(io, -1);

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
            final int fd = getDescriptor(io);
            return posix().lseek(fd, amount, whence);
        }

    }

    @RubiniusPrimitive(name = "io_accept")
    public abstract static class AcceptNode extends RubiniusPrimitiveNode {

        public AcceptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int accept(VirtualFrame frame, RubyBasicObject io) {
            final int fd = getDescriptor(io);

            final IntByReference addressLength = new IntByReference(16);
            final long address = UnsafeHolder.U.allocateMemory(addressLength.intValue());

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
        public RubyString sysread(VirtualFrame frame, RubyBasicObject file, int length) {
            final int fd = getDescriptor(file);

            final ByteBuffer buffer = ByteBuffer.allocate(length);

            int toRead = length;

            while (toRead > 0) {
                getContext().getSafepointManager().poll(this);

                final int readIteration = posix().read(fd, buffer, toRead);
                buffer.position(readIteration);
                toRead -= readIteration;
            }

            return StringNodes.createString(getContext().getCoreLibrary().getStringClass(), buffer);
        }

    }

    @RubiniusPrimitive(name = "io_select", needsSelf = false)
    public static abstract class IOSelectPrimitiveNode extends RubiniusPrimitiveNode {

        private static final FDSetFactory fdSetFactory = FDSetFactoryFactory.create();

        public IOSelectPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isNil(writables)", "isNil(errorables)"})
        public Object select(RubyArray readables, RubyBasicObject writables, RubyBasicObject errorables, int timeout) {
            final Object[] readableObjects = ArrayNodes.slowToArray(readables);
            final int[] readableFds = getFileDescriptors(readables);

            final FDSet readableSet = fdSetFactory.create();

            for (int fd : readableFds) {
                readableSet.set(fd);
            }

            final int result = getContext().getThreadManager().runOnce(new ThreadManager.BlockingActionWithoutGlobalLock<Integer>() {
                @Override
                public Integer block() throws InterruptedException {
                    return nativeSockets().select(
                            max(readableFds) + 1,
                            readableSet.getPointer(),
                            PointerPrimitiveNodes.NULL_POINTER,
                            PointerPrimitiveNodes.NULL_POINTER,
                            PointerPrimitiveNodes.NULL_POINTER);
                }
            });

            if (result == -1) {
                return nil();
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                    getSetObjects(readableObjects, readableFds, readableSet),
                    ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass()),
                    ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass()));
        }

        private int[] getFileDescriptors(RubyArray fileDescriptorArray) {
            final Object[] objects = ArrayNodes.slowToArray(fileDescriptorArray);

            final int[] fileDescriptors = new int[objects.length];

            for (int n = 0; n < objects.length; n++) {
                if (!(objects[n] instanceof RubyBasicObject)) {
                    throw new UnsupportedOperationException();
                }

                fileDescriptors[n] = getDescriptor((RubyBasicObject) objects[n]);
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

        private RubyArray getSetObjects(Object[] objects, int[] fds, FDSet set) {
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

    }

}
