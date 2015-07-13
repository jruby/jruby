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

import com.kenai.jffi.MemoryIO;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.Pointer;
import org.jruby.truffle.nodes.core.PointerGuards;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.rubinius.RubiniusTypes;
import org.jruby.util.ByteList;
import org.jruby.util.unsafe.UnsafeHolder;

public abstract class PointerPrimitiveNodes {

    @RubiniusPrimitive(name = "pointer_malloc")
    public static abstract class PointerMallocPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerMallocPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject malloc(RubyClass pointerClass, int size) {
            return malloc(pointerClass, (long) size);
        }

        @Specialization
        public RubyBasicObject malloc(RubyClass pointerClass, long size) {
            return PointerNodes.createPointer(pointerClass, getMemoryManager().newPointer(UnsafeHolder.U.allocateMemory(size)));
        }

    }

    @RubiniusPrimitive(name = "pointer_free")
    public static abstract class PointerFreePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerFreePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject free(RubyBasicObject pointer) {
            UnsafeHolder.U.freeMemory(PointerNodes.getPointer(pointer).address());
            return pointer;
        }

    }

    @RubiniusPrimitive(name = "pointer_set_address")
    public static abstract class PointerSetAddressPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long setAddress(RubyBasicObject pointer, int address) {
            return setAddress(pointer, (long) address);
        }

        @Specialization
        public long setAddress(RubyBasicObject pointer, long address) {
            PointerNodes.setPointer(pointer, getMemoryManager().newPointer(address));
            return address;
        }

    }

    @RubiniusPrimitive(name = "pointer_add")
    public static abstract class PointerAddPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAddPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject add(RubyBasicObject a, int b) {
            return add(a, (long) b);
        }

        @Specialization
        public RubyBasicObject add(RubyBasicObject a, long b) {
            return PointerNodes.createPointer(a.getLogicalClass(), getMemoryManager().newPointer(PointerNodes.getPointer(a).address() + b));
        }

    }

    @RubiniusPrimitive(name = "pointer_read_int")
    public static abstract class PointerReadIntPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSigned(signed)")
        public int readInt(RubyBasicObject pointer, boolean signed) {
            return PointerNodes.getPointer(pointer).getInt(0);
        }

        protected boolean isSigned(boolean signed) {
            return signed;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_string", lowerFixnumParameters = 0)
    public static abstract class PointerReadStringPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadStringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject readString(RubyBasicObject pointer, int length) {
            final byte[] bytes = new byte[length];
            PointerNodes.getPointer(pointer).get(0, bytes, 0, length);
            return createString(bytes);
        }

    }

    @RubiniusPrimitive(name = "pointer_set_autorelease")
    public static abstract class PointerSetAutoreleasePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAutoreleasePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean setAutorelease(RubyBasicObject pointer, boolean autorelease) {
            // TODO CS 24-April-2015 let memory leak
            return autorelease;
        }

    }

    @RubiniusPrimitive(name = "pointer_set_at_offset", lowerFixnumParameters = {0, 1})
    @ImportStatic(RubiniusTypes.class)
    public static abstract class PointerSetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "type == TYPE_INT")
        public int setAtOffsetInt(RubyBasicObject pointer, int offset, int type, int value) {
            PointerNodes.getPointer(pointer).putInt(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_LONG")
        public long setAtOffsetLong(RubyBasicObject pointer, int offset, int type, long value) {
            PointerNodes.getPointer(pointer).putLong(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_ULONG")
        public long setAtOffsetULong(RubyBasicObject pointer, int offset, int type, long value) {
            PointerNodes.getPointer(pointer).putLong(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_ULL")
        public long setAtOffsetULL(RubyBasicObject pointer, int offset, int type, long value) {
            PointerNodes.getPointer(pointer).putLongLong(offset, value);
            return value;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_pointer")
    public static abstract class PointerReadPointerPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadPointerPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject readPointer(RubyBasicObject pointer) {
            return PointerNodes.createPointer(pointer.getLogicalClass(), PointerNodes.getPointer(pointer).getPointer(0));
        }

    }

    @RubiniusPrimitive(name = "pointer_address")
    public static abstract class PointerAddressPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long address(RubyBasicObject pointer) {
            return PointerNodes.getPointer(pointer).address();
        }

    }

    @RubiniusPrimitive(name = "pointer_get_at_offset")
    @ImportStatic(RubiniusTypes.class)
    public static abstract class PointerGetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerGetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "type == TYPE_CHAR")
        public int getAtOffsetChar(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getByte(offset);
        }

        @Specialization(guards = "type == TYPE_UCHAR")
        public int getAtOffsetUChar(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getByte(offset);
        }

        @Specialization(guards = "type == TYPE_INT")
        public int getAtOffsetInt(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getInt(offset);
        }

        @Specialization(guards = "type == TYPE_SHORT")
        public int getAtOffsetShort(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getShort(offset);
        }

        @Specialization(guards = "type == TYPE_USHORT")
        public int getAtOffsetUShort(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getShort(offset);
        }

        @Specialization(guards = "type == TYPE_LONG")
        public long getAtOffsetLong(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getLong(offset);
        }

        @Specialization(guards = "type == TYPE_ULONG")
        public long getAtOffsetULong(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getLong(offset);
        }

        @Specialization(guards = "type == TYPE_ULL")
        public long getAtOffsetULL(RubyBasicObject pointer, int offset, int type) {
            return PointerNodes.getPointer(pointer).getLongLong(offset);
        }

        @Specialization(guards = "type == TYPE_STRING")
        public RubyBasicObject getAtOffsetString(RubyBasicObject pointer, int offset, int type) {
            return createString(PointerNodes.getPointer(pointer).getString(offset));
        }

        @Specialization(guards = "type == TYPE_PTR")
        public RubyBasicObject getAtOffsetPointer(RubyBasicObject pointer, int offset, int type) {
            final Pointer readPointer = PointerNodes.getPointer(pointer).getPointer(offset);

            if (readPointer == null) {
                return nil();
            } else {
                return PointerNodes.createPointer(pointer.getLogicalClass(), readPointer);
            }
        }

    }

    @RubiniusPrimitive(name = "pointer_write_string")
    public static abstract class PointerWriteStringPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerWriteStringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public RubyBasicObject address(RubyBasicObject pointer, RubyBasicObject string, int maxLength) {
            final ByteList bytes = StringNodes.getByteList(string);
            final int length = Math.min(bytes.length(), maxLength);
            PointerNodes.getPointer(pointer).put(0, bytes.unsafeBytes(), bytes.begin(), length);
            return pointer;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_string_to_null")
    @ImportStatic(PointerGuards.class)
    public static abstract class PointerReadStringToNullPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadStringToNullPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullPointer(pointer)")
        public RubyBasicObject readNullPointer(RubyBasicObject pointer) {
            return StringNodes.createEmptyString(getContext().getCoreLibrary().getStringClass());
        }

        @Specialization(guards = "!isNullPointer(pointer)")
        public RubyBasicObject readStringToNull(RubyBasicObject pointer) {
            return createString(MemoryIO.getInstance().getZeroTerminatedByteArray(PointerNodes.getPointer(pointer).address()));
        }

    }

    @RubiniusPrimitive(name = "pointer_write_int")
    public static abstract class PointerWriteIntPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerWriteIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject address(RubyBasicObject pointer, int value) {
            PointerNodes.getPointer(pointer).putInt(0, value);
            return pointer;
        }

    }

}
