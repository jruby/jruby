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
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.StringSupport;

/**
 * Rubinius primitives associated with the Ruby {@code String} class.
 */
public abstract class StringPrimitiveNodes {

    @RubiniusPrimitive(name = "string_check_null_safe", needsSelf = false)
    public static abstract class StringCheckNullSafePrimitiveNode extends RubiniusPrimitiveNode {

        private final ConditionProfile nullByteProfile = ConditionProfile.createBinaryProfile();

        public StringCheckNullSafePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringCheckNullSafePrimitiveNode(StringCheckNullSafePrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public boolean stringCheckNullSafe(RubyString string) {
            for (byte b : string.getBytes().unsafeBytes()) {
                if (nullByteProfile.profile(b == 0)) {
                    return false;
                }
            }

            return true;
        }

    }

    @RubiniusPrimitive(name = "string_to_f", needsSelf = false)
    public static abstract class StringToFPrimitiveNode extends RubiniusPrimitiveNode {

        public StringToFPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringToFPrimitiveNode(StringToFPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString stringToF(RubyString string) {
            throw new UnsupportedOperationException("string_to_f");
        }

    }

    @RubiniusPrimitive(name = "string_index")
    public static abstract class StringIndexPrimitiveNode extends RubiniusPrimitiveNode {

        public StringIndexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StringIndexPrimitiveNode(StringIndexPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object stringIndex(RubyString string, RubyString pattern, int start) {
            final int index = StringSupport.index(string,
                    pattern,
                    start, string.getBytes().getEncoding());

            if (index == -1) {
                return getContext().getCoreLibrary().getNilObject();
            }

            return index;
        }

    }

}
