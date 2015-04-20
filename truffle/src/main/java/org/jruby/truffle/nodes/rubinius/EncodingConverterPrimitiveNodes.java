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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvResult;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
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

        @Specialization
        public Object encodingConverterAllocate(RubyClass encodingConverterClass, UndefinedPlaceholder undefined1, UndefinedPlaceholder undefined2) {
            return new RubyEncodingConverter(encodingConverterClass, null);
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_primitive_convert")
    public static abstract class PrimitiveConvertNode extends RubiniusPrimitiveNode {

        public PrimitiveConvertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization
        public RubyString encodingConverterPutback(RubyEncodingConverter encodingConverter, int maxBytes) {
            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = encodingConverter.getEConv();
            final int putbackable = ec.putbackable();

            return putback(encodingConverter, putbackable < maxBytes ? putbackable : maxBytes);
        }

        @Specialization
        public RubyString encodingConverterPutback(RubyEncodingConverter encodingConverter, UndefinedPlaceholder maxBytes) {
            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = encodingConverter.getEConv();

            return putback(encodingConverter, ec.putbackable());
        }

        private RubyString putback(RubyEncodingConverter encodingConverter, int n) {
            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = encodingConverter.getEConv();

            final ByteList bytes = new ByteList(n);
            ec.putback(bytes.getUnsafeBytes(), bytes.getBegin(), n);
            bytes.setRealSize(n);

            if (ec.sourceEncoding != null) {
                bytes.setEncoding(ec.sourceEncoding);
            }

            return getContext().makeString(bytes);
        }
    }

    @RubiniusPrimitive(name = "encoding_converter_last_error")
    public static abstract class EncodingConverterLastErrorNode extends RubiniusPrimitiveNode {

        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        public EncodingConverterLastErrorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object encodingConverterLastError(VirtualFrame frame, RubyEncodingConverter encodingConverter) {
            notDesignedForCompilation();

            final EConv ec = encodingConverter.getEConv();
            final EConv.LastError lastError = ec.lastError;

            if (lastError.getResult() != EConvResult.InvalidByteSequence &&
                    lastError.getResult() != EConvResult.IncompleteInput &&
                    lastError.getResult() != EConvResult.UndefinedConversion) {
                return nil();
            }

            Object ret = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().newSymbol("result"), eConvResultToSymbol(lastError.getResult()));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().newSymbol("source_encoding_name"), getContext().makeString(new ByteList(lastError.getSource())));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().newSymbol("destination_encoding_name"), getContext().makeString(new ByteList(lastError.getDestination())));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().newSymbol("error_bytes"), getContext().makeString(new ByteList(lastError.getErrorBytes())));

            if (lastError.getReadAgainLength() != 0) {
                lookupTableWriteNode.call(frame, ret, "[]=", null, getContext().newSymbol("read_again_bytes"), lastError.getReadAgainLength());
            }

            return ret;
        }

        private RubySymbol eConvResultToSymbol(EConvResult result) {
            switch(result) {
                case InvalidByteSequence: return getContext().newSymbol("invalid_byte_sequence");
                case UndefinedConversion: return getContext().newSymbol("undefined_conversion");
                case DestinationBufferFull: return getContext().newSymbol("destination_buffer_full");
                case SourceBufferEmpty: return getContext().newSymbol("source_buffer_empty");
                case Finished: return getContext().newSymbol("finished");
                case AfterOutput: return getContext().newSymbol("after_output");
                case IncompleteInput: return getContext().newSymbol("incomplete_input");
            }

            throw new UnsupportedOperationException(String.format("Unknown EConv result: %s", result));
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_primitive_errinfo")
    public static abstract class EncodingConverterErrinfoNode extends RubiniusPrimitiveNode {

        public EncodingConverterErrinfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
