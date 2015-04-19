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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.ByteList;

public abstract class IOBufferPrimitiveNodes {

    private static final int IOBUFFER_SIZE = 32768;

    @RubiniusPrimitive(name = "iobuffer_allocate")
    public static abstract class IOBufferAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOBufferAllocatePrimitiveNode(IOBufferAllocatePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject allocate(VirtualFrame frame, RubyClass classToAllocate) {
            final RubyBasicObject ioBuffer = new RubyBasicObject(classToAllocate);
            rubyWithSelf(frame, ioBuffer, "@write_synced = true");
            rubyWithSelf(frame, ioBuffer, "@storage = storage", "storage",
                    new RubiniusByteArray(getContext().getCoreLibrary().getByteArrayClass(), new ByteList(IOBUFFER_SIZE)));
            rubyWithSelf(frame, ioBuffer, "@used = 0");
            rubyWithSelf(frame, ioBuffer, "@start = 0");
            rubyWithSelf(frame, ioBuffer, "@total = total", "total", IOBUFFER_SIZE);
            return ioBuffer;
        }

    }

    @RubiniusPrimitive(name = "iobuffer_unshift")
    public static abstract class IOBufferUnshiftPrimitiveNode extends RubiniusPrimitiveNode {

        public IOBufferUnshiftPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IOBufferUnshiftPrimitiveNode(IOBufferUnshiftPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public int unshift(VirtualFrame frame, RubyBasicObject ioBuffer, RubyString string, int startPosition) {
            int total_ = IOBUFFER_SIZE;

            rubyWithSelf(frame, ioBuffer, "@write_synced = false");
            int start_pos_native = startPosition;
            int str_size = string.getByteList().realSize();
            int total_sz = str_size - start_pos_native;
            int used_native = (int) rubyWithSelf(frame, ioBuffer, "@used");
            int available_space = total_ - used_native;

            if(total_sz > available_space) {
                total_sz = available_space;
            }

            ByteList storage = ((RubiniusByteArray) rubyWithSelf(frame, ioBuffer, "@storage")).getBytes();

            // Data is copied here - can we do something COW?
            System.arraycopy(string.getByteList().unsafeBytes(), start_pos_native, storage.getUnsafeBytes(), used_native, total_sz);

            rubyWithSelf(frame, ioBuffer, "@used = used", "used", used_native + total_sz);

            return total_sz;
        }

    }

}
