/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyConverter.java
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvResult;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.encoding.EncodingConverterNodes;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.util.ByteList;

import static org.jruby.truffle.core.string.StringOperations.rope;

/**
 * Rubinius primitives associated with the Ruby {@code Encoding::Converter} class..
 */
public abstract class EncodingConverterPrimitiveNodes {

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "encoding_converter_allocate")
    public static abstract class EncodingConverterAllocateNode extends RubiniusPrimitiveArrayArgumentsNode {

        public EncodingConverterAllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object encodingConverterAllocate(DynamicObject encodingConverterClass, NotProvided unused1, NotProvided unused2) {
            return EncodingConverterNodes.createEncodingConverter(encodingConverterClass, null);
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "encoding_converter_primitive_convert")
    public static abstract class PrimitiveConvertNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public PrimitiveConvertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = {"isRubyString(source)", "isRubyString(target)", "isRubyHash(options)"})
        public Object encodingConverterPrimitiveConvert(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, DynamicObject options) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Specialization(guards = {"isNil(source)", "isRubyString(target)"})
        public Object primitiveConvertNilSource(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, int options) {
            return primitiveConvertHelper(encodingConverter, source, target, offset, size, options);
        }

        @Specialization(guards = {"isRubyString(source)", "isRubyString(target)"})
        public Object encodingConverterPrimitiveConvert(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, int options) {

            // Taken from org.jruby.RubyConverter#primitive_convert.

            return primitiveConvertHelper(encodingConverter, source, target, offset, size, options);
        }

        @TruffleBoundary
        private Object primitiveConvertHelper(DynamicObject encodingConverter, DynamicObject source,
                                              DynamicObject target, int offset, int size, int options) {
            // Taken from org.jruby.RubyConverter#primitive_convert.

            final boolean nonNullSource = source != nil();
            Rope sourceRope = nonNullSource ? rope(source) : RopeConstants.EMPTY_UTF8_ROPE;
            final Rope targetRope = rope(target);
            final ByteList outBytes = targetRope.toByteListCopy();

            final Ptr inPtr = new Ptr();
            final Ptr outPtr = new Ptr();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            final boolean changeOffset = (offset == 0);
            final boolean growOutputBuffer = (size == -1);

            if (size == -1) {
                size = 16; // in MRI, this is RSTRING_EMBED_LEN_MAX

                if (nonNullSource) {
                    if (size < sourceRope.byteLength()) {
                        size = sourceRope.byteLength();
                    }
                }
            }

            while (true) {

                if (changeOffset) {
                    offset = outBytes.getRealSize();
                }

                if (outBytes.getRealSize() < offset) {
                    throw new RaiseException(
                            coreLibrary().argumentError("output offset too big", this)
                    );
                }

                long outputByteEnd = offset + size;

                if (outputByteEnd > Integer.MAX_VALUE) {
                    // overflow check
                    throw new RaiseException(
                            coreLibrary().argumentError("output offset + bytesize too big", this)
                    );
                }

                outBytes.ensure((int) outputByteEnd);

                inPtr.p = 0;
                outPtr.p = offset;
                int os = outPtr.p + size;
                EConvResult res = ec.convert(sourceRope.getBytes(), inPtr, sourceRope.byteLength() + inPtr.p, outBytes.getUnsafeBytes(), outPtr, os, options);

                outBytes.setRealSize(outPtr.p - outBytes.begin());

                if (nonNullSource) {
                    sourceRope = makeSubstringNode.executeMake(sourceRope, inPtr.p, sourceRope.byteLength() - inPtr.p);
                    StringOperations.setRope(source, sourceRope);
                }

                if (growOutputBuffer && res == EConvResult.DestinationBufferFull) {
                    if (Integer.MAX_VALUE / 2 < size) {
                        throw new RaiseException(
                                coreLibrary().argumentError("too long conversion result", this)
                        );
                    }
                    size *= 2;
                    continue;
                }

                if (ec.destinationEncoding != null) {
                    outBytes.setEncoding(ec.destinationEncoding);
                }

                StringOperations.setRope(target, StringOperations.ropeFromByteList(outBytes));

                return getSymbol(res.symbolicName());
            }
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "encoding_converter_putback")
    public static abstract class EncodingConverterPutbackNode extends RubiniusPrimitiveArrayArgumentsNode {

        public EncodingConverterPutbackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject encodingConverterPutback(DynamicObject encodingConverter, int maxBytes) {
            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);
            final int putbackable = ec.putbackable();

            return putback(encodingConverter, putbackable < maxBytes ? putbackable : maxBytes);
        }

        @Specialization
        public DynamicObject encodingConverterPutback(DynamicObject encodingConverter, NotProvided maxBytes) {
            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            return putback(encodingConverter, ec.putbackable());
        }

        private DynamicObject putback(DynamicObject encodingConverter, int n) {
            assert RubyGuards.isRubyEncodingConverter(encodingConverter);

            // Taken from org.jruby.RubyConverter#putback.

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            final ByteList bytes = new ByteList(n);
            ec.putback(bytes.getUnsafeBytes(), bytes.getBegin(), n);
            bytes.setRealSize(n);

            if (ec.sourceEncoding != null) {
                bytes.setEncoding(ec.sourceEncoding);
            }

            return createString(bytes);
        }
    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "encoding_converter_last_error")
    public static abstract class EncodingConverterLastErrorNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        public EncodingConverterLastErrorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object encodingConverterLastError(VirtualFrame frame, DynamicObject encodingConverter) {
            CompilerDirectives.transferToInterpreter();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);
            final EConv.LastError lastError = ec.lastError;

            if (lastError.getResult() != EConvResult.InvalidByteSequence &&
                    lastError.getResult() != EConvResult.IncompleteInput &&
                    lastError.getResult() != EConvResult.UndefinedConversion) {
                return nil();
            }

            Object ret = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new", null);

            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("result"), eConvResultToSymbol(lastError.getResult()));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("source_encoding_name"), createString(new ByteList(lastError.getSource())));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("destination_encoding_name"), createString(new ByteList(lastError.getDestination())));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("error_bytes"), createString(new ByteList(lastError.getErrorBytes())));

            if (lastError.getReadAgainLength() != 0) {
                lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("read_again_bytes"), lastError.getReadAgainLength());
            }

            return ret;
        }

        private DynamicObject eConvResultToSymbol(EConvResult result) {
            switch(result) {
                case InvalidByteSequence: return getSymbol("invalid_byte_sequence");
                case UndefinedConversion: return getSymbol("undefined_conversion");
                case DestinationBufferFull: return getSymbol("destination_buffer_full");
                case SourceBufferEmpty: return getSymbol("source_buffer_empty");
                case Finished: return getSymbol("finished");
                case AfterOutput: return getSymbol("after_output");
                case IncompleteInput: return getSymbol("incomplete_input");
            }

            throw new UnsupportedOperationException(String.format("Unknown EConv result: %s", result));
        }

    }

    @RubiniusPrimitive(unsafeNeedsAudit = true, name = "encoding_converter_primitive_errinfo")
    public static abstract class EncodingConverterErrinfoNode extends RubiniusPrimitiveArrayArgumentsNode {

        public EncodingConverterErrinfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object encodingConverterLastError(DynamicObject encodingConverter) {
            CompilerDirectives.transferToInterpreter();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            final Object[] ret = { getSymbol(ec.lastError.getResult().symbolicName()), nil(), nil(), nil(), nil() };

            if (ec.lastError.getSource() != null) {
                ret[1] = createString(new ByteList(ec.lastError.getSource()));
            }

            if (ec.lastError.getDestination() != null) {
                ret[2] = createString(new ByteList(ec.lastError.getDestination()));
            }

            if (ec.lastError.getErrorBytes() != null) {
                ret[3] = createString(new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP(), ec.lastError.getErrorBytesLength()));
                ret[4] = createString(new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP() + ec.lastError.getErrorBytesLength(), ec.lastError.getReadAgainLength()));
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ret, ret.length);
        }

    }

}
