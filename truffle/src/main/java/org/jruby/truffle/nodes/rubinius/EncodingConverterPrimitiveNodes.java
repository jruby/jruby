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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyEncodingConverter;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding::Converter} class..
 */
public abstract class EncodingConverterPrimitiveNodes {

    @RubiniusPrimitive(name = "encoding_converter_allocate")
    public static abstract class EncodingConverterAllocateNode extends RubiniusPrimitiveNode {

        public EncodingConverterAllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingConverterAllocateNode(EncodingConverterAllocateNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingConverterAllocate(RubyEncoding fromEncoding, RubyEncoding toEncoding, RubyHash options) {
            return new RubyEncodingConverter(getContext().getCoreLibrary().getEncodingConverterClass(), null);
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_primitive_convert")
    public static abstract class EncodingConverterPrimitiveConvertNode extends RubiniusPrimitiveNode {

        public EncodingConverterPrimitiveConvertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingConverterPrimitiveConvertNode(EncodingConverterPrimitiveConvertNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingConverterPrimitiveConvert(RubyBasicObject encodingConverter, RubyString source,
                                                        RubyString target, int offset, int size, RubyHash options) {
            throw new UnsupportedOperationException("not implemented");
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_putback")
    public static abstract class EncodingConverterPutbackNode extends RubiniusPrimitiveNode {

        public EncodingConverterPutbackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingConverterPutbackNode(EncodingConverterPutbackNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingConverterPutback(RubyBasicObject encodingConverter, int maxBytes) {
            throw new UnsupportedOperationException("not implemented");
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_last_error")
    public static abstract class EncodingConverterLastErrorNode extends RubiniusPrimitiveNode {

        public EncodingConverterLastErrorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingConverterLastErrorNode(EncodingConverterLastErrorNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingConverterLastError(RubyBasicObject encodingConverter) {
            throw new UnsupportedOperationException("not implemented");
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_primitive_errinfo")
    public static abstract class EncodingConverterErrinfoNode extends RubiniusPrimitiveNode {

        public EncodingConverterErrinfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingConverterErrinfoNode(EncodingConverterErrinfoNode prev) {
            super(prev);
        }

        @Specialization
        public Object encodingConverterLastError(RubyEncodingConverter encodingConverter) {
            throw new UnsupportedOperationException("not implemented");
        }

    }

}
