/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyConverter.java
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyEncodingConverter;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

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
        public Object encodingConverterPrimitiveConvert(RubyEncodingConverter encodingConverter, RubyString source,
                                                        RubyString target, int offset, int size, RubyHash options) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Specialization
        public Object encodingConverterPrimitiveConvert(RubyEncodingConverter encodingConverter, RubyString source,
                                                        RubyString target, int offset, int size, int options) {

            // Taken from org.jruby.RubyConverter#primitive_convert.

            source.modify();
            source.clearCodeRange();

            target.modify();
            target.clearCodeRange();

            final ByteList inBytes = source.getByteList();
            final ByteList outBytes = target.getByteList();

            final Ptr inPtr = new Ptr();
            final Ptr outPtr = new Ptr();

            final EConv ec = encodingConverter.getEConv();

            final boolean changeOffset = (offset == 0);
            final boolean growOutputBuffer = (size == -1);

            if (size == -1) {
                size = 16; // in MRI, this is RSTRING_EMBED_LEN_MAX

                if (size < source.getByteList().getRealSize()) {
                    size = source.getByteList().getRealSize();
                }
            }

            while (true) {

                if (changeOffset) {
                    offset = outBytes.getRealSize();
                }

                if (outBytes.getRealSize() < offset) {
                    throw new RaiseException(
                            getContext().getCoreLibrary().argumentError("output offset too big", this)
                    );
                }

                long outputByteEnd = offset + size;

                if (outputByteEnd > Integer.MAX_VALUE) {
                    // overflow check
                    throw new RaiseException(
                            getContext().getCoreLibrary().argumentError("output offset + bytesize too big", this)
                    );
                }

                outBytes.ensure((int)outputByteEnd);

                inPtr.p = inBytes.getBegin();
                outPtr.p = outBytes.getBegin() + offset;
                int os = outPtr.p + size;
                EConvResult res = ec.convert(inBytes.getUnsafeBytes(), inPtr, inBytes.getRealSize() + inPtr.p, outBytes.getUnsafeBytes(), outPtr, os, options);

                outBytes.setRealSize(outPtr.p - outBytes.begin());

                source.getByteList().setRealSize(inBytes.getRealSize() - (inPtr.p - inBytes.getBegin()));
                source.getByteList().setBegin(inPtr.p);

                if (growOutputBuffer && res == EConvResult.DestinationBufferFull) {
                    if (Integer.MAX_VALUE / 2 < size) {
                        throw new RaiseException(
                                getContext().getCoreLibrary().argumentError("too long conversion result", this)
                        );
                    }
                    size *= 2;
                    continue;
                }

                if (ec.destinationEncoding != null) {
                    outBytes.setEncoding(ec.destinationEncoding);
                }

                return getContext().newSymbol(res.symbolicName());
            }
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
        public Object encodingConverterLastError(RubyEncodingConverter encodingConverter) {
            notDesignedForCompilation();

            final org.jruby.exceptions.RaiseException e = EncodingUtils.makeEconvException(getContext().getRuntime(), encodingConverter.getEConv());

            if (e == null) {
                return nil();
            }

            return getContext().toTruffle(e.getException());
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
            notDesignedForCompilation();

            final EConv ec = encodingConverter.getEConv();

            final Object[] ret = { getContext().newSymbol(ec.lastError.getResult().symbolicName()), nil(), nil(), nil(), nil() };

            if (ec.lastError.getSource() != null) {
                ret[1] = getContext().makeString(new ByteList(ec.lastError.getSource()));
            }

            if (ec.lastError.getDestination() != null) {
                ret[2] = getContext().makeString(new ByteList(ec.lastError.getDestination()));
            }

            if (ec.lastError.getErrorBytes() != null) {
                ret[3] = getContext().makeString(new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP(), ec.lastError.getErrorBytesLength()));
                ret[4] = getContext().makeString(new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP() + ec.lastError.getErrorBytesLength(), ec.lastError.getReadAgainLength()));
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ret, ret.length);
        }

    }

}
