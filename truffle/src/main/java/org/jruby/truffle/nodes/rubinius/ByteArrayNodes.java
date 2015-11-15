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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.core.StringNodesFactory;
import org.jruby.truffle.nodes.core.UnaryCoreMethodNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;

@CoreClass(name = "Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    public static DynamicObject createByteArray(DynamicObjectFactory factory, ByteList bytes) {
        return Layouts.BYTE_ARRAY.createByteArray(factory, bytes);
    }

    @CoreMethod(names = "get_byte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getByte(DynamicObject bytes, int index) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index) & 0xff;
        }

    }

    @CoreMethod(names = "prepend", required = 1)
    public abstract static class PrependNode extends CoreMethodArrayArgumentsNode {

        public PrependNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject prepend(DynamicObject bytes, DynamicObject string) {
            final int prependLength = StringOperations.getByteList(string).getUnsafeBytes().length;
            final int originalLength = Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes().length;
            final int newLength = prependLength + originalLength;
            final byte[] prependedBytes = new byte[newLength];
            System.arraycopy(StringOperations.getByteList(string).getUnsafeBytes(), 0, prependedBytes, 0, prependLength);
            System.arraycopy(Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes(), 0, prependedBytes, prependLength, originalLength);
            return ByteArrayNodes.createByteArray(getContext().getCoreLibrary().getByteArrayFactory(), new ByteList(prependedBytes));
        }

    }

    @CoreMethod(names = "set_byte", required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class SetByteNode extends CoreMethodArrayArgumentsNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setByte(DynamicObject bytes, int index, int value) {
            if (index < 0 || index >= Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexError("index out of bounds", this));
            }

            Layouts.BYTE_ARRAY.getBytes(bytes).set(index, value);
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject bytes) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3, lowerFixnumParameters = {1, 2})
    public abstract static class LocateNode extends CoreMethodArrayArgumentsNode {

        @Child private StringNodes.SizeNode sizeNode;

        public LocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] {});
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object getByte(VirtualFrame frame, DynamicObject bytes, DynamicObject pattern, int start, int length) {
            final int index = new ByteList(Layouts.BYTE_ARRAY.getBytes(bytes), start, length).indexOf(StringOperations.getByteList(pattern));

            if (index == -1) {
                return nil();
            } else {
                return start + index + sizeNode.executeInteger(frame, pattern);
            }
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
