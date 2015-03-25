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
import org.jruby.truffle.runtime.core.*;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding} class..
 */
public abstract class EncodingPrimitiveNodes {

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
        public Object encodingConverterLastError(RubyBasicObject encodingConverter) {
            throw new UnsupportedOperationException("not implemented");
        }

    }

    @RubiniusPrimitive(name = "encoding_get_object_encoding", needsSelf = false)
    public static abstract class EncodingGetObjectEncodingNode extends RubiniusPrimitiveNode {

        public EncodingGetObjectEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingGetObjectEncodingNode(EncodingGetObjectEncodingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubyString string) {
            notDesignedForCompilation();

            return RubyEncoding.getEncoding(string.getBytes().getEncoding());
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubySymbol symbol) {
            notDesignedForCompilation();

            return RubyEncoding.getEncoding(symbol.getSymbolBytes().getEncoding());
        }

        @Specialization
        public RubyEncoding encodingGetObjectEncoding(RubyEncoding encoding) {
            return encoding;
        }

        @Specialization(guards = {"!isRubyString", "!isRubySymbol", "!isRubyEncoding"})
        public RubyNilClass encodingGetObjectEncoding(RubyBasicObject object) {
            // TODO(CS, 26 Jan 15) something to do with __encoding__ here?
            return nil();
        }

    }

}
