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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.FileStat;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.unsafe.UnsafeHolder;
import sun.misc.Unsafe;

public abstract class PointerPrimitiveNodes {

    public static final HiddenKey ADDRESS_IDENTIFIER = new HiddenKey("address");

    public static abstract class WriteAddressPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeAddressNode;

        public WriteAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeAddressNode = new WriteHeadObjectFieldNode(PointerPrimitiveNodes.ADDRESS_IDENTIFIER);
        }

        public long writeAddress(RubyBasicObject pointer, long address) {
            writeAddressNode.execute(pointer, address);
            return address;
        }

    }

    public static abstract class ReadAddressPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadHeadObjectFieldNode readAddressNode;

        public ReadAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readAddressNode = new ReadHeadObjectFieldNode(PointerPrimitiveNodes.ADDRESS_IDENTIFIER);
        }

        public long getAddress(RubyBasicObject pointer) {
            try {
                return readAddressNode.executeLong(pointer);
            } catch (UnexpectedResultException e) {
                throw new UnsupportedOperationException();
            }
        }

    }

    @RubiniusPrimitive(name = "pointer_malloc")
    public static abstract class PointerMallocPrimitiveNode extends WriteAddressPrimitiveNode {

        public PointerMallocPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject malloc(RubyClass pointerClass, int size) {
            return malloc(pointerClass, (long) size);
        }

        @Specialization
        public RubyBasicObject malloc(RubyClass pointerClass, long size) {
            final RubyBasicObject pointer = new RubyBasicObject(pointerClass);
            writeAddress(pointer, UnsafeHolder.U.allocateMemory(size));
            return pointer;
        }

    }

    @RubiniusPrimitive(name = "pointer_free")
    public static abstract class PointerFreePrimitiveNode extends ReadAddressPrimitiveNode {

        public PointerFreePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long free(RubyBasicObject pointer) {
            final long address = getAddress(pointer);
            UnsafeHolder.U.freeMemory(address);
            return address;
        }

    }

    @RubiniusPrimitive(name = "pointer_set_address")
    public static abstract class PointerSetAddressPrimitiveNode extends WriteAddressPrimitiveNode {

        public PointerSetAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long setAddress(RubyBasicObject pointer, int address) {
            return setAddress(pointer, (long) address);
        }

        @Specialization
        public long setAddress(RubyBasicObject pointer, long address) {
            return writeAddress(pointer, address);
        }

    }

    @RubiniusPrimitive(name = "pointer_add")
    public static abstract class PointerAddPrimitiveNode extends WriteAddressPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeAddressNode;
        @Child private ReadHeadObjectFieldNode readAddressNode;

        public PointerAddPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeAddressNode = new WriteHeadObjectFieldNode(ADDRESS_IDENTIFIER);
            readAddressNode = new ReadHeadObjectFieldNode(PointerPrimitiveNodes.ADDRESS_IDENTIFIER);
        }

        @Specialization
        public RubyBasicObject add(RubyBasicObject a, int b) {
            return add(a, (long) b);
        }

        @Specialization
        public RubyBasicObject add(RubyBasicObject a, long b) {
            final RubyBasicObject result = new RubyBasicObject(a.getLogicalClass());
            writeAddress(result, getAddress(a) + b);
            return result;
        }

        public long writeAddress(RubyBasicObject pointer, long address) {
            writeAddressNode.execute(pointer, address);
            return address;
        }

        public long getAddress(RubyBasicObject pointer) {
            try {
                return readAddressNode.executeLong(pointer);
            } catch (UnexpectedResultException e) {
                throw new UnsupportedOperationException();
            }
        }

    }

    @RubiniusPrimitive(name = "pointer_read_int")
    public static abstract class PointerReadIntPrimitiveNode extends ReadAddressPrimitiveNode {

        public PointerReadIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSigned(signed)")
        public long readInt(RubyBasicObject pointer, boolean signed) {
            return UnsafeHolder.U.getInt(getAddress(pointer));
        }

        protected boolean isSigned(boolean signed) {
            return signed;
        }

    }

}
