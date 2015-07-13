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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.util.ByteList;

import java.util.EnumSet;

@CoreClass(name = "Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    public static class ByteArrayType extends BasicObjectType {

    }

    public static final ByteArrayType BYTE_ARRAY_TYPE = new ByteArrayType();

    private static final HiddenKey BYTES_IDENTIFIER = new HiddenKey("bytes");
    public static final Property BYTES_PROPERTY;
    private static final DynamicObjectFactory BYTE_ARRAY_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        BYTES_PROPERTY = Property.create(BYTES_IDENTIFIER, allocator.locationForType(ByteList.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        final Shape shape = RubyBasicObject.LAYOUT.createShape(BYTE_ARRAY_TYPE).addProperty(BYTES_PROPERTY);
        BYTE_ARRAY_FACTORY = shape.createFactory();
    }

    public static RubyBasicObject createByteArray(RubyClass rubyClass, ByteList bytes) {
        return new RubyBasicObject(rubyClass, BYTE_ARRAY_FACTORY.newInstance(bytes));
    }

    public static ByteList getBytes(RubyBasicObject byteArray) {
        assert RubyGuards.isRubiniusByteArray(byteArray);
        assert byteArray.getDynamicObject().getShape().hasProperty(BYTES_IDENTIFIER);
        return (ByteList) BYTES_PROPERTY.get(byteArray.getDynamicObject(), true);
    }

    @CoreMethod(names = "get_byte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getByte(RubyBasicObject bytes, int index) {
            return getBytes(bytes).get(index);
        }

    }

    @CoreMethod(names = "prepend", required = 1)
    public abstract static class PrependNode extends CoreMethodArrayArgumentsNode {

        public PrependNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public RubyBasicObject prepend(RubyBasicObject bytes, RubyBasicObject string) {
            final int prependLength = StringNodes.getByteList(string).getUnsafeBytes().length;
            final int originalLength = getBytes(bytes).getUnsafeBytes().length;
            final int newLength = prependLength + originalLength;
            final byte[] prependedBytes = new byte[newLength];
            System.arraycopy(StringNodes.getByteList(string).getUnsafeBytes(), 0, prependedBytes, 0, prependLength);
            System.arraycopy(getBytes(bytes).getUnsafeBytes(), 0, prependedBytes, prependLength, originalLength);
            return ByteArrayNodes.createByteArray(getContext().getCoreLibrary().getByteArrayClass(), new ByteList(prependedBytes));
        }

    }

    @CoreMethod(names = "set_byte", required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class SetByteNode extends CoreMethodArrayArgumentsNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setByte(RubyBasicObject bytes, int index, int value) {
            if (index < 0 || index >= getBytes(bytes).getRealSize()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexError("index out of bounds", this));
            }

            getBytes(bytes).set(index, value);
            return getBytes(bytes).get(index);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(RubyBasicObject bytes) {
            return getBytes(bytes).getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3, lowerFixnumParameters = {1, 2})
    public abstract static class LocateNode extends CoreMethodArrayArgumentsNode {

        public LocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object getByte(RubyBasicObject bytes, RubyBasicObject pattern, int start, int length) {
            final int index = new ByteList(getBytes(bytes), start, length).indexOf(StringNodes.getByteList(pattern));

            if (index == -1) {
                return nil();
            } else {
                return start + index + StringNodes.length(pattern);
            }
        }

    }

}
