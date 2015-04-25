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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.ByteList;

@CoreClass(name = "Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    @CoreMethod(names = "get_byte", required = 1)
    public abstract static class GetByteNode extends CoreMethodNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getByte(RubiniusByteArray bytes, int index) {
            return bytes.getBytes().get(index);
        }

    }

    @CoreMethod(names = "prepend", required = 1)
    public abstract static class PrependNode extends CoreMethodNode {

        public PrependNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubiniusByteArray prepend(RubiniusByteArray bytes, RubyString string) {
            final int prependLength = string.getByteList().getUnsafeBytes().length;
            final int originalLength = bytes.getBytes().getUnsafeBytes().length;
            final int newLength = prependLength + originalLength;
            final byte[] prependedBytes = new byte[newLength];
            System.arraycopy(string.getByteList().getUnsafeBytes(), 0, prependedBytes, 0, prependLength);
            System.arraycopy(bytes.getBytes().getUnsafeBytes(), 0, prependedBytes, prependLength, originalLength);
            return new RubiniusByteArray(getContext().getCoreLibrary().getByteArrayClass(), new ByteList(prependedBytes));
        }

    }

    @CoreMethod(names = "set_byte", required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class SetByteNode extends CoreMethodNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setByte(RubiniusByteArray bytes, int index, int value) {
            if (index < 0 || index >= bytes.getBytes().getRealSize()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexError("index out of bounds", this));
            }

            bytes.getBytes().set(index, value);
            return bytes.getBytes().get(index);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(RubiniusByteArray bytes) {
            return bytes.getBytes().getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3)
    public abstract static class LocateNode extends CoreMethodNode {

        public LocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getByte(RubiniusByteArray bytes, RubyString pattern, int start, int length) {
            final int index = new ByteList(bytes.getBytes().unsafeBytes(), start, length)
                    .indexOf(pattern.getBytes());

            if (index == -1) {
                return nil();
            } else {
                return start + index + pattern.length();
            }
        }

    }

}
