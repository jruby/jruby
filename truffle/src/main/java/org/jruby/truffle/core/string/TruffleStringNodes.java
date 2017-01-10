/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.string;


import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;

import static org.jruby.truffle.core.string.StringOperations.rope;

@CoreClass("Truffle::String")
public class TruffleStringNodes {

    @CoreMethod(names = "truncate", onSingleton = true, required = 2, lowerFixnum = 2)
    public abstract static class TruncateNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = { "newByteLength < 0" })
        @TruffleBoundary
        public DynamicObject truncateLengthNegative(DynamicObject string, int newByteLength) {
            throw new RaiseException(
                    getContext().getCoreExceptions().argumentError(formatNegativeError(newByteLength), this));
        }

        @Specialization(guards = { "newByteLength >= 0", "isRubyString(string)", "isNewLengthTooLarge(string, newByteLength)" })
        @TruffleBoundary
        public DynamicObject truncateLengthTooLong(DynamicObject string, int newByteLength) {
            throw new RaiseException(
                    getContext().getCoreExceptions().argumentError(formatTooLongError(newByteLength, rope(string)), this));
        }

        @Specialization(guards = { "newByteLength >= 0", "isRubyString(string)", "!isNewLengthTooLarge(string, newByteLength)" })
        public DynamicObject stealStorage(DynamicObject string, int newByteLength,
                @Cached("create()") RopeNodes.MakeSubstringNode makeSubstringNode) {

            StringOperations.setRope(string, makeSubstringNode.executeMake(rope(string), 0, newByteLength));

            return string;
        }

        protected static boolean isNewLengthTooLarge(DynamicObject string, int newByteLength) {
            assert RubyGuards.isRubyString(string);

            return newByteLength > rope(string).byteLength();
        }

        @TruffleBoundary
        private String formatNegativeError(int count) {
            return StringUtils.format("Invalid byte count: %d is negative", count);
        }

        @TruffleBoundary
        private String formatTooLongError(int count, final Rope rope) {
            return StringUtils.format("Invalid byte count: %d exceeds string size of %d bytes", count, rope.byteLength());
        }

    }

}
