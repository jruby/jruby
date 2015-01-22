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

import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding} class..
 */
public abstract class EncodingPrimitiveNodes {

    @RubiniusPrimitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingPrimitiveNode extends RubiniusPrimitiveNode {

        public EncodingGetObjectEncodingPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingGetObjectEncodingPrimitiveNode(EncodingGetObjectEncodingPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingGetObjectEncoding(Object object) {
            throw new UnsupportedOperationException("encoding_get_object_encoding");
        }

    }

}
