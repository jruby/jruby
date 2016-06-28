/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.util.ByteList;

@CoreClass("Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    public static DynamicObject createByteArray(DynamicObjectFactory factory, ByteList bytes) {
        return Layouts.BYTE_ARRAY.createByteArray(factory, bytes);
    }

    @CoreMethod(names = "get_byte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int getByte(DynamicObject bytes, int index,
                              @Cached("createBinaryProfile()") ConditionProfile nullByteIndexProfile) {
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);

            // Handling out-of-bounds issues like this is non-standard. In Rubinius, it would raise an exception instead.
            // We're modifying the semantics to address a primary use case for this class: Rubinius's @data array
            // in the String class. Rubinius Strings are NULL-terminated and their code working with Strings takes
            // advantage of that fact. So, where they expect to receive a NULL byte, we'd be out-of-bounds and raise
            // an exception. Simply appending a NULL byte may trigger a full copy of the original byte[], which we
            // want to avoid. The compromise is bending on the semantics here.
            if (nullByteIndexProfile.profile(index == byteList.realSize())) {
                return 0;
            }

            return byteList.get(index) & 0xff;
        }

    }

    @CoreMethod(names = "prepend", required = 1)
    public abstract static class PrependNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject prepend(DynamicObject bytes, DynamicObject string) {
            final Rope rope = StringOperations.rope(string);
            final int prependLength = rope.byteLength();
            final int originalLength = Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes().length;
            final int newLength = prependLength + originalLength;
            final byte[] prependedBytes = new byte[newLength];
            System.arraycopy(rope.getBytes(), 0, prependedBytes, 0, prependLength);
            System.arraycopy(Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes(), 0, prependedBytes, prependLength, originalLength);
            return ByteArrayNodes.createByteArray(coreLibrary().getByteArrayFactory(), new ByteList(prependedBytes));
        }

    }

    @CoreMethod(names = "set_byte", required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class SetByteNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object setByte(DynamicObject bytes, int index, int value,
                @Cached("create()") BranchProfile errorProfile) {
            if (index < 0 || index >= Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

            Layouts.BYTE_ARRAY.getBytes(bytes).set(index, value);
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int size(DynamicObject bytes) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3, lowerFixnumParameters = {1, 2})
    public abstract static class LocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(pattern)")
        public Object getByte(DynamicObject bytes, DynamicObject pattern, int start, int length) {
            final int index = new ByteList(Layouts.BYTE_ARRAY.getBytes(bytes), start, length).indexOf(StringOperations.getByteListReadOnly(pattern));

            if (index == -1) {
                return nil();
            } else {
                return start + index + StringOperations.rope(pattern).characterLength();
            }
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
