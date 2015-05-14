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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import jnr.ffi.Pointer;

import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.unsafe.UnsafeHolder;

import java.util.EnumSet;

public abstract class PointerPrimitiveNodes {

    public static final Pointer NULL_POINTER = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().newOpaquePointer(0);

    private static final HiddenKey POINTER_IDENTIFIER = new HiddenKey("pointer");
    private static final Property POINTER_PROPERTY;
    private static final DynamicObjectFactory POINTER_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        POINTER_PROPERTY = Property.create(POINTER_IDENTIFIER, allocator.locationForType(Pointer.class, EnumSet.of(LocationModifier.NonNull)), 0);
        POINTER_FACTORY = RubyBasicObject.EMPTY_SHAPE.addProperty(POINTER_PROPERTY).createFactory();
    }

    public static class PointerAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return PointerPrimitiveNodes.createPointer(rubyClass, jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().newOpaquePointer(0));
        }
    }

    public static RubyBasicObject createPointer(RubyClass rubyClass, Pointer pointer) {
        assert pointer != null;
        return new RubyBasicObject(rubyClass, POINTER_FACTORY.newInstance(pointer));
    }

    public static void setPointer(RubyBasicObject pointer, Pointer newPointer) {
        assert newPointer != null;
        assert pointer.getDynamicObject().getShape().hasProperty(POINTER_IDENTIFIER);

        try {
            POINTER_PROPERTY.set(pointer.getDynamicObject(), newPointer, pointer.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static Pointer getPointer(RubyBasicObject pointer) {
        assert pointer.getDynamicObject().getShape().hasProperty(POINTER_IDENTIFIER);
        return (Pointer) POINTER_PROPERTY.get(pointer.getDynamicObject(), true);
    }

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
            return createPointer(pointerClass, getMemoryManager().newPointer(UnsafeHolder.U.allocateMemory(size)));
        }

    }

    @RubiniusPrimitive(name = "pointer_free")
    public static abstract class PointerFreePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerFreePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject free(RubyBasicObject pointer) {
            UnsafeHolder.U.freeMemory(getPointer(pointer).address());
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
            setPointer(pointer, getMemoryManager().newPointer(address));
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
            return createPointer(a.getLogicalClass(), getMemoryManager().newPointer(getPointer(a).address() + b));
        }

    }

    @RubiniusPrimitive(name = "pointer_read_int")
    public static abstract class PointerReadIntPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSigned(signed)")
        public int readInt(RubyBasicObject pointer, boolean signed) {
            return getPointer(pointer).getInt(0);
        }

        protected boolean isSigned(boolean signed) {
            return signed;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_string")
    public static abstract class PointerReadStringPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadStringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString readString(RubyBasicObject pointer, int length) {
            final byte[] bytes = new byte[length];
            getPointer(pointer).get(0, bytes, 0, length);
            return getContext().makeString(bytes);
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

    @RubiniusPrimitive(name = "pointer_set_at_offset", lowerFixnumParameters = {0, 2})
    public static abstract class PointerSetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int setAtOffset(RubyBasicObject pointer, int offset, int type, int value) {
            assert type == 5;
            // TODO CS 13-May-15 what does the type parameter do?
            getPointer(pointer).putInt(offset, value);
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
            return createPointer(pointer.getLogicalClass(), nullOrPointer(getPointer(pointer).getPointer(0)));
        }

    }

    @RubiniusPrimitive(name = "pointer_address")
    public static abstract class PointerAddressPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long address(RubyBasicObject pointer) {
            return getPointer(pointer).address();
        }

    }

    @RubiniusPrimitive(name = "pointer_get_at_offset")
    public static abstract class PointerGetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerGetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getAtOffset(RubyBasicObject pointer, int offset, int type) {
            assert type == 5;
            // TODO CS 13-May-15 not sure about int/long here
            return getPointer(pointer).getInt(offset);
        }

    }

    private static Pointer nullOrPointer(Pointer pointer) {
        if (pointer == null) {
            return NULL_POINTER;
        } else {
            return pointer;
        }
    }

}
