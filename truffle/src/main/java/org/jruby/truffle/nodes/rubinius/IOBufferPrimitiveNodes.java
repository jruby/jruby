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
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.util.EnumSet;

public abstract class IOBufferPrimitiveNodes {

    private static final int IOBUFFER_SIZE = 32768;
    private static final int STACK_BUF_SZ = 8192;

    private static final String WRITE_SYNCED_IDENTIFIER = "@write_synced";
    private static final Property WRITE_SYNCED_PROPERTY;

    private static final String STORAGE_IDENTIFIER = "@storage";
    private static final Property STORAGE_PROPERTY;

    private static final String USED_IDENTIFIER = "@used";
    private static final Property USED_PROPERTY;

    private static final String START_IDENTIFIER = "@start";
    private static final Property START_PROPERTY;

    private static final String TOTAL_IDENTIFIER = "@total";
    private static final Property TOTAL_PROPERTY;

    private static final DynamicObjectFactory IO_BUFFER_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        WRITE_SYNCED_PROPERTY = Property.create(WRITE_SYNCED_IDENTIFIER, allocator.locationForType(boolean.class), 0);
        STORAGE_PROPERTY = Property.create(STORAGE_IDENTIFIER, allocator.locationForType(RubyBasicObject.class, EnumSet.of(LocationModifier.NonNull)), 0);
        USED_PROPERTY = Property.create(USED_IDENTIFIER, allocator.locationForType(int.class), 0);
        START_PROPERTY = Property.create(START_IDENTIFIER, allocator.locationForType(int.class), 0);
        TOTAL_PROPERTY = Property.create(TOTAL_IDENTIFIER, allocator.locationForType(int.class), 0);

        IO_BUFFER_FACTORY = RubyBasicObject.EMPTY_SHAPE
                .addProperty(WRITE_SYNCED_PROPERTY)
                .addProperty(STORAGE_PROPERTY)
                .addProperty(USED_PROPERTY)
                .addProperty(START_PROPERTY)
                .addProperty(TOTAL_PROPERTY)
                .createFactory();
    }

    public static void setWriteSynced(RubyBasicObject io, boolean writeSynced) {
        assert io.getDynamicObject().getShape().hasProperty(WRITE_SYNCED_IDENTIFIER);

        try {
            WRITE_SYNCED_PROPERTY.set(io.getDynamicObject(), writeSynced, io.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static RubyBasicObject getStorage(RubyBasicObject io) {
        assert io.getDynamicObject().getShape().hasProperty(STORAGE_IDENTIFIER);
        return (RubyBasicObject) STORAGE_PROPERTY.get(io.getDynamicObject(), true);
    }

    private static int getUsed(RubyBasicObject io) {
        assert io.getDynamicObject().getShape().hasProperty(USED_IDENTIFIER);
        return (int) USED_PROPERTY.get(io.getDynamicObject(), true);
    }

    public static void setUsed(RubyBasicObject io, int used) {
        assert io.getDynamicObject().getShape().hasProperty(USED_IDENTIFIER);

        try {
            USED_PROPERTY.set(io.getDynamicObject(), used, io.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static int getTotal(RubyBasicObject io) {
        assert io.getDynamicObject().getShape().hasProperty(TOTAL_IDENTIFIER);
        return (int) TOTAL_PROPERTY.get(io.getDynamicObject(), true);
    }

    @RubiniusPrimitive(name = "iobuffer_allocate")
    public static abstract class IOBufferAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject allocate(VirtualFrame frame, RubyClass classToAllocate) {
            return new RubyBasicObject(classToAllocate, IO_BUFFER_FACTORY.newInstance(
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
                        throw new RaiseException(new RubyException(getContext().getCoreLibrary().getErrnoClass(Errno.valueOf(errno))));
                    }
                } else {
                    break;
                }
            }

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

        private int left(VirtualFrame frame, RubyBasicObject ioBuffer) {
            final int total = getTotal(ioBuffer);
            final int used = getUsed(ioBuffer);
            return total - used;
        }

    }

}
