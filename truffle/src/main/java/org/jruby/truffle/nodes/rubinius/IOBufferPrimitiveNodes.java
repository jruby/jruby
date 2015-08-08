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
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ExceptionNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.om.dsl.api.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.ByteList;

import java.util.EnumSet;

public abstract class IOBufferPrimitiveNodes {

    private static final int IOBUFFER_SIZE = 32768;
    private static final int STACK_BUF_SZ = 8192;

    @org.jruby.truffle.om.dsl.api.Layout
    public interface IOBufferLayout extends BasicObjectNodes.BasicObjectLayout {

        String WRITE_SYNCED_IDENTIFIER = "@write_synced";
        String STORAGE_IDENTIFIER = "@storage";
        String USED_IDENTIFIER = "@used";
        String START_IDENTIFIER = "@start";
        String TOTAL_IDENTIFIER = "@total";

        DynamicObjectFactory createIOBufferShape(RubyBasicObject logicalClass, RubyBasicObject metaClass);

        DynamicObject createIOBuffer(DynamicObjectFactory factory, boolean writeSynced, RubyBasicObject storage, int used, int start, int total);

        boolean getWriteSynced(DynamicObject object);
        void setWriteSynced(DynamicObject object, boolean value);

        RubyBasicObject getStorage(DynamicObject object);
        void setStorage(DynamicObject object, RubyBasicObject value);

        int getUsed(DynamicObject object);
        void setUsed(DynamicObject object, int value);

        int getStart(DynamicObject object);
        void setStart(DynamicObject object, int value);

        int getTotal(DynamicObject object);
        void setTotal(DynamicObject object, int value);

    }

    public static final IOBufferLayout IO_BUFFER_LAYOUT = IOBufferLayoutImpl.INSTANCE;

    public static void setWriteSynced(RubyBasicObject io, boolean writeSynced) {
        IO_BUFFER_LAYOUT.setWriteSynced(io.dynamicObject, writeSynced);
    }

    private static RubyBasicObject getStorage(RubyBasicObject io) {
        return IO_BUFFER_LAYOUT.getStorage(io.dynamicObject);
    }

    private static int getUsed(RubyBasicObject io) {
        return IO_BUFFER_LAYOUT.getUsed(io.dynamicObject);
    }

    public static void setUsed(RubyBasicObject io, int used) {
        IO_BUFFER_LAYOUT.setUsed(io.dynamicObject, used);
    }

    private static int getTotal(RubyBasicObject io) {
        return IO_BUFFER_LAYOUT.getTotal(io.dynamicObject);
    }

    @RubiniusPrimitive(name = "iobuffer_allocate")
    public static abstract class IOBufferAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject allocate(RubyBasicObject classToAllocate) {
            return BasicObjectNodes.createRubyBasicObject(classToAllocate, IO_BUFFER_LAYOUT.createIOBuffer(
                    ModuleNodes.getModel(classToAllocate).getFactory(),
                    true,
                    ByteArrayNodes.createByteArray(getContext().getCoreLibrary().getByteArrayClass(), new ByteList(IOBUFFER_SIZE)),
                    0,
                    0,
                    IOBUFFER_SIZE));
        }

    }

    @RubiniusPrimitive(name = "iobuffer_unshift", lowerFixnumParameters = 1)
    public static abstract class IOBufferUnshiftPrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferUnshiftPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public int unshift(VirtualFrame frame, RubyBasicObject ioBuffer, RubyBasicObject string, int startPosition) {
            setWriteSynced(ioBuffer, false);

            final ByteList byteList = StringNodes.getByteList(string);
            int stringSize = byteList.realSize() - startPosition;
            final int usedSpace = getUsed(ioBuffer);
            final int availableSpace = IOBUFFER_SIZE - usedSpace;

            if (stringSize > availableSpace) {
                stringSize = availableSpace;
            }

            ByteList storage = ByteArrayNodes.getBytes(getStorage(ioBuffer));

            // Data is copied here - can we do something COW?
            System.arraycopy(byteList.unsafeBytes(), byteList.begin() + startPosition, storage.getUnsafeBytes(), storage.begin() + usedSpace, stringSize);

            setUsed(ioBuffer, usedSpace + stringSize);

            return stringSize;
        }

    }

    @RubiniusPrimitive(name = "iobuffer_fill")
    public static abstract class IOBufferFillPrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferFillPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int fill(VirtualFrame frame, RubyBasicObject ioBuffer, RubyBasicObject io) {
            final int fd = IOPrimitiveNodes.getDescriptor(io);

            // TODO CS 21-Apr-15 allocating this buffer for each read is crazy
            final byte[] readBuffer = new byte[STACK_BUF_SZ];
            int count = STACK_BUF_SZ;

            if (left(frame, ioBuffer) < count) {
                count = left(frame, ioBuffer);
            }

            int bytesRead = performFill(fd, readBuffer, count);

            if (bytesRead > 0) {
                // Detect if another thread has updated the buffer
                // and now there isn't enough room for this data.
                if (bytesRead > left(frame, ioBuffer)) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().internalError("IO buffer overrun", this));
                }
                final int used = getUsed(ioBuffer);
                final ByteList storage = ByteArrayNodes.getBytes(getStorage(ioBuffer));
                System.arraycopy(readBuffer, 0, storage.getUnsafeBytes(), storage.getBegin() + used, bytesRead);
                storage.setRealSize(used + bytesRead);
                setUsed(ioBuffer, used + bytesRead);
            }

            return bytesRead;
        }

        @TruffleBoundary
        private int performFill(int fd, byte[] readBuffer, int count) {
            int bytesRead;
            while (true) {
                bytesRead = posix().read(fd, readBuffer, count);

                if (bytesRead == -1) {
                    final int errno = posix().errno();

                    if (errno == Errno.ECONNRESET.intValue() || errno == Errno.ETIMEDOUT.intValue()) {
                        // Treat as seeing eof
                        bytesRead = 0;
                        break;
                    } else if (errno == Errno.EAGAIN.intValue() || errno == Errno.EINTR.intValue()) {
                        //if (!state -> check_async(calling_environment))
                        //    return NULL;
                        //io -> ensure_open(state);
                        getContext().getSafepointManager().poll(this);
                        continue;
                    } else {
                        CompilerDirectives.transferToInterpreter();
                        throw new RaiseException(ExceptionNodes.createRubyException(getContext().getCoreLibrary().getErrnoClass(Errno.valueOf(errno))));
                    }
                } else {
                    break;
                }
            }
            return bytesRead;
        }

        private int left(VirtualFrame frame, RubyBasicObject ioBuffer) {
            final int total = getTotal(ioBuffer);
            final int used = getUsed(ioBuffer);
            return total - used;
        }

    }

}
