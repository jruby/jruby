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
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.ByteList;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;

import java.util.Arrays;

@CoreClass("Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    public static DynamicObject createByteArray(DynamicObjectFactory factory, ByteList bytes) {
        return Layouts.BYTE_ARRAY.createByteArray(factory, bytes);
    }

    @CoreMethod(names = {"get_byte", "getbyte", "[]"}, required = 1, optional = 1, lowerFixnum = { 1, 2 })
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "wasNotProvided(length)")
        public int getByte(DynamicObject bytes, int index, Object length,
                              @Cached("create()") BranchProfile errorProfile,
                              @Cached("createBinaryProfile()") ConditionProfile nullByteIndexProfile) {
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);

            if (index < 0 || index >= byteList.length()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

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

        @Specialization
        public DynamicObject getBytes(DynamicObject bytes, int index, int length,
                                      @Cached("create()") BranchProfile errorProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile nullByteIndexProfile) {
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);

            if (index < 0 || index >= byteList.length()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

            final byte[] newBytes = new byte[length];

            System.arraycopy(byteList.unsafeBytes(), byteList.begin() + index, newBytes, 0, length);

            return Layouts.BYTE_ARRAY.createByteArray(getContext().getCoreLibrary().getByteArrayFactory(), new ByteList(newBytes, ASCIIEncoding.INSTANCE, false));
        }

    }

    @CoreMethod(names = "index", required = 1, lowerFixnum = 1)
    public abstract static class IndexNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object index(DynamicObject byteArray, int value) {
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(byteArray);
            final byte[] bytes = byteList.unsafeBytes();

            for (int i = byteList.begin(); i < byteList.begin() + byteList.length(); i++) {
                if (bytes[i] == (byte) value) {
                    return i;
                }
            }

            return nil();
        }
    }

    @CoreMethod(names = {"append", "<<"}, required = 1)
    public abstract static class AppendNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject appendString(DynamicObject bytes, DynamicObject string) {
            final Rope rope = StringOperations.rope(string);
            Layouts.BYTE_ARRAY.getBytes(bytes).append(rope.getBytes());

            return bytes;
        }

        @Specialization(guards = "isRubiniusByteArray(otherBytes)")
        public DynamicObject appendByteArray(DynamicObject bytes, DynamicObject otherBytes) {
            Layouts.BYTE_ARRAY.getBytes(bytes).append(Layouts.BYTE_ARRAY.getBytes(otherBytes));

            return bytes;
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

    @CoreMethod(names = {"set_byte", "setbyte", "[]="}, required = 2, optional = 1, lowerFixnum = { 1, 2 })
    public abstract static class SetByteNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object setByte(DynamicObject bytes, int index, int value, NotProvided otherByteArray,
                @Cached("create()") BranchProfile errorProfile) {
            if (index < 0 || index >= Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

            Layouts.BYTE_ARRAY.getBytes(bytes).set(index, value);
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index);
        }

        @Specialization(guards = "isRubiniusByteArray(otherByteArray)")
        public Object setByte(DynamicObject bytes, int begin, int length, DynamicObject otherByteArray,
                              @Cached("create()") BranchProfile errorProfile) {
            if (begin < 0 || begin >= Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize()) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);
            final ByteList otherByteList = Layouts.BYTE_ARRAY.getBytes(otherByteArray);

            if (byteList.length() < begin) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError("index out of bounds", this));
            }

            if ((byteList.length() < length) || (byteList.length() < (begin + length))) {
                length = byteList.length() - begin;
            }

            final int newArrayLength = (begin - byteList.begin()) + otherByteList.length() + (byteList.length() - (byteList.begin() + begin + length));
            final byte[] newBytes = new byte[newArrayLength];

            System.arraycopy(byteList.unsafeBytes(), byteList.begin(), newBytes, 0, byteList.begin() + begin);
            System.arraycopy(otherByteList.unsafeBytes(), otherByteList.begin(), newBytes, begin, otherByteList.length());
            System.arraycopy(byteList.unsafeBytes(), byteList.begin() + begin + length, newBytes, begin + otherByteList.length(), byteList.length() - (byteList.begin() + begin + length));

            Layouts.BYTE_ARRAY.setBytes(bytes, new ByteList(newBytes, ASCIIEncoding.INSTANCE, false));

            return otherByteArray;
        }

    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int size(DynamicObject bytes) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3, lowerFixnum = { 2, 3 })
    public abstract static class LocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyString(pattern)")
        public Object getByte(DynamicObject bytes, DynamicObject pattern, int start, int length) {
            final Rope patternRope = StringOperations.rope(pattern);
            final int index = new ByteList(Layouts.BYTE_ARRAY.getBytes(bytes), start, length).indexOf(patternRope);

            if (index == -1) {
                return nil();
            } else {
                return start + index + StringOperations.rope(pattern).characterLength();
            }
        }

    }

    @CoreMethod(names = "truncate", required = 1, lowerFixnum = 1)
    public abstract static class TruncateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject index(DynamicObject byteArray, int index) {
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(byteArray);
            byteList.length(index);

            return byteArray;
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, ByteList.EMPTY_BYTELIST);
        }

    }

    @CoreMethod(names = "initialize", required = 2, lowerFixnum = { 1, 2 })
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject initialize(DynamicObject byteArray, int size, int value) {
            final byte[] bytes = new byte[size];
            Arrays.fill(bytes, (byte) value);

            Layouts.BYTE_ARRAY.setBytes(byteArray, new ByteList(bytes, ASCIIEncoding.INSTANCE, false));

            return byteArray;
        }

    }

}
