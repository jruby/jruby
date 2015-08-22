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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Ptr;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvResult;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.EncodingConverterNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

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
        public Object encodingConverterAllocate(DynamicObject encodingConverterClass, NotProvided unused1, NotProvided unused2) {
            return EncodingConverterNodes.createEncodingConverter(encodingConverterClass, null);
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_primitive_convert")
    public static abstract class PrimitiveConvertNode extends RubiniusPrimitiveNode {

        private final ConditionProfile nonNullSourceProfile = ConditionProfile.createBinaryProfile();

        public PrimitiveConvertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(source)", "isRubyString(target)", "isRubyHash(options)"})
        public Object encodingConverterPrimitiveConvert(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, DynamicObject options) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Specialization(guards = {"isNil(source)", "isRubyString(target)"})
        public Object primitiveConvertNilSource(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, int options) {
            return primitiveConvertHelper(encodingConverter, new ByteList(), source, target, offset, size, options);
        }

        @Specialization(guards = {"isRubyString(source)", "isRubyString(target)"})
        public Object encodingConverterPrimitiveConvert(DynamicObject encodingConverter, DynamicObject source,
                                                        DynamicObject target, int offset, int size, int options) {

            // Taken from org.jruby.RubyConverter#primitive_convert.

            StringNodes.modify(source);
            StringNodes.clearCodeRange(source);

            return primitiveConvertHelper(encodingConverter, Layouts.STRING.getByteList(source), source, target, offset, size, options);
        }

        @TruffleBoundary
        private Object primitiveConvertHelper(DynamicObject encodingConverter, ByteList inBytes, DynamicObject source,
                                              DynamicObject target, int offset, int size, int options) {
            // Taken from org.jruby.RubyConverter#primitive_convert.

            final boolean nonNullSource = source != nil();

            StringNodes.modify(target);
            StringNodes.clearCodeRange(target);

            final ByteList outBytes = Layouts.STRING.getByteList(target);

            final Ptr inPtr = new Ptr();
            final Ptr outPtr = new Ptr();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            final boolean changeOffset = (offset == 0);
            final boolean growOutputBuffer = (size == -1);

            if (size == -1) {
                size = 16; // in MRI, this is RSTRING_EMBED_LEN_MAX

                if (nonNullSourceProfile.profile(nonNullSource)) {
                    if (size < Layouts.STRING.getByteList(source).getRealSize()) {
                        size = Layouts.STRING.getByteList(source).getRealSize();
                    }
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

                outBytes.ensure((int) outputByteEnd);

                inPtr.p = inBytes.getBegin();
                outPtr.p = outBytes.getBegin() + offset;
                int os = outPtr.p + size;
                EConvResult res = ec.convert(inBytes.getUnsafeBytes(), inPtr, inBytes.getRealSize() + inPtr.p, outBytes.getUnsafeBytes(), outPtr, os, options);

                outBytes.setRealSize(outPtr.p - outBytes.begin());

                if (nonNullSourceProfile.profile(nonNullSource)) {
                    Layouts.STRING.getByteList(source).setRealSize(inBytes.getRealSize() - (inPtr.p - inBytes.getBegin()));
                    Layouts.STRING.getByteList(source).setBegin(inPtr.p);
                }

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

                return getSymbol(res.symbolicName());
            }
        }

    }

    @RubiniusPrimitive(name = "encoding_converter_putback")
    public static abstract class EncodingConverterPutbackNode extends RubiniusPrimitiveNode {

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

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), bytes, StringSupport.CR_UNKNOWN, null);
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
        public Object encodingConverterLastError(VirtualFrame frame, DynamicObject encodingConverter) {
            CompilerDirectives.transferToInterpreter();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);
            final EConv.LastError lastError = ec.lastError;

            if (lastError.getResult() != EConvResult.InvalidByteSequence &&
                    lastError.getResult() != EConvResult.IncompleteInput &&
                    lastError.getResult() != EConvResult.UndefinedConversion) {
                return nil();
            }

            Object ret = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("result"), eConvResultToSymbol(lastError.getResult()));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("source_encoding_name"), Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(lastError.getSource()), StringSupport.CR_UNKNOWN, null));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("destination_encoding_name"), Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(lastError.getDestination()), StringSupport.CR_UNKNOWN, null));
            lookupTableWriteNode.call(frame, ret, "[]=", null, getSymbol("error_bytes"), Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(lastError.getErrorBytes()), StringSupport.CR_UNKNOWN, null));

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

    @RubiniusPrimitive(name = "encoding_converter_primitive_errinfo")
    public static abstract class EncodingConverterErrinfoNode extends RubiniusPrimitiveNode {

        public EncodingConverterErrinfoNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object encodingConverterLastError(DynamicObject encodingConverter) {
            CompilerDirectives.transferToInterpreter();

            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);

            final Object[] ret = { getSymbol(ec.lastError.getResult().symbolicName()), nil(), nil(), nil(), nil() };

            if (ec.lastError.getSource() != null) {
                ret[1] = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(ec.lastError.getSource()), StringSupport.CR_UNKNOWN, null);
            }

            if (ec.lastError.getDestination() != null) {
                ret[2] = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(ec.lastError.getDestination()), StringSupport.CR_UNKNOWN, null);
            }

            if (ec.lastError.getErrorBytes() != null) {
                ret[3] = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP(), ec.lastError.getErrorBytesLength()), StringSupport.CR_UNKNOWN, null);
                ret[4] = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), new ByteList(ec.lastError.getErrorBytes(), ec.lastError.getErrorBytesP() + ec.lastError.getErrorBytesLength(), ec.lastError.getReadAgainLength()), StringSupport.CR_UNKNOWN, null);
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), ret, ret.length);
        }

    }

}
