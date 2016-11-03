/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyConverter.java
 */
package org.jruby.truffle.core.encoding;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.Ptr;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.EConvResult;
import org.jcodings.transcode.TranscoderDB;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;

import static org.jruby.truffle.core.string.StringOperations.rope;

@CoreClass("Encoding::Converter")
public abstract class EncodingConverterNodes {

    @NonStandard
    @CoreMethod(names = "initialize_jruby", required = 2, optional = 1, lowerFixnum = 3, visibility = Visibility.PRIVATE)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubyEncoding(source)", "isRubyEncoding(destination)" })
        public DynamicObject initialize(DynamicObject self, DynamicObject source, DynamicObject destination, int options) {
            // Adapted from RubyConverter - see attribution there
            //
            // This method should only be called after the Encoding::Converter instance has already been initialized
            // by Rubinius.  Rubinius will do the heavy lifting of parsing the options hash and setting the `@options`
            // ivar to the resulting int for EConv flags.

            Encoding sourceEncoding = Layouts.ENCODING.getEncoding(source);
            Encoding destinationEncoding = Layouts.ENCODING.getEncoding(destination);
            final byte[] sourceEncodingName = sourceEncoding.getName();
            final byte[] destinationEncodingName = destinationEncoding.getName();

            final EConv econv = TranscoderDB.open(sourceEncodingName, destinationEncodingName, rubiniusToJRubyFlags(options));

            econv.sourceEncoding = sourceEncoding;
            econv.destinationEncoding = destinationEncoding;

            Layouts.ENCODING_CONVERTER.setEconv(self, econv);

            return nil();
        }

        /**
         * Rubinius and JRuby process Encoding::Converter options flags differently.  Rubinius splits the processing
         * between initial setup and the replacement value setup, whereas JRuby handles them all during initial setup.
         * We figure out what flags JRuby additionally expects to be set and set them to satisfy EConv.
         */
        private int rubiniusToJRubyFlags(int flags) {
            if ((flags & EConvFlags.XML_TEXT_DECORATOR) != 0) {
                flags |= EConvFlags.UNDEF_HEX_CHARREF;
            }

            if ((flags & EConvFlags.XML_ATTR_CONTENT_DECORATOR) != 0) {
                flags |= EConvFlags.UNDEF_HEX_CHARREF;
            }

            return flags;
        }

    }

    @NonStandard
    @CoreMethod(names = "transcoding_map", onSingleton = true)
    public abstract static class TranscodingMapNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode upcaseNode;
        @Child private CallDispatchHeadNode toSymNode;
        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;
        @Child private CallDispatchHeadNode newTranscodingNode;

        public TranscodingMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            upcaseNode = DispatchHeadNodeFactory.createMethodCall(context);
            toSymNode = DispatchHeadNodeFactory.createMethodCall(context);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
            newTranscodingNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object transcodingMap(VirtualFrame frame) {
            final Object ret = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new");

            for (CaseInsensitiveBytesHash<TranscoderDB.Entry> sourceEntry : TranscoderDB.transcoders) {
                Object key = null;
                final Object value = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new");

                for (Hash.HashEntry<TranscoderDB.Entry> destinationEntry : sourceEntry.entryIterator()) {
                    final TranscoderDB.Entry e = destinationEntry.value;

                    if (key == null) {
                        final Object upcased = upcaseNode.call(frame, createString(e.getSource(), USASCIIEncoding.INSTANCE), "upcase");
                        key = toSymNode.call(frame, upcased, "to_sym");
                    }

                    final Object upcasedLookupTableKey = upcaseNode.call(frame, createString(e.getDestination(), USASCIIEncoding.INSTANCE), "upcase");
                    final Object lookupTableKey = toSymNode.call(frame, upcasedLookupTableKey, "to_sym");
                    final Object lookupTableValue = newTranscodingNode.call(frame, coreLibrary().getTranscodingClass(), "create", key, lookupTableKey);
                    lookupTableWriteNode.call(frame, value, "[]=", lookupTableKey, lookupTableValue);
                }

                lookupTableWriteNode.call(frame, ret, "[]=", key, value);
            }

            return ret;
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            Object econv = null;
            return allocateNode.allocate(rubyClass, econv);
        }

    }

    @Primitive(name = "encoding_converter_primitive_convert")
    public static abstract class PrimitiveConvertNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public PrimitiveConvertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
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
            final ByteList outBytes = RopeOperations.toByteListCopy(targetRope);

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
                            coreExceptions().argumentError("output offset too big", this)
                    );
                }

                long outputByteEnd = offset + size;

                if (outputByteEnd > Integer.MAX_VALUE) {
                    // overflow check
                    throw new RaiseException(
                            coreExceptions().argumentError("output offset + bytesize too big", this)
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
                                coreExceptions().argumentError("too long conversion result", this)
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

    @Primitive(name = "encoding_converter_putback")
    public static abstract class EncodingConverterPutbackNode extends PrimitiveArrayArgumentsNode {

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

    @Primitive(name = "encoding_converter_last_error")
    public static abstract class EncodingConverterLastErrorNode extends PrimitiveArrayArgumentsNode {

        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        public EncodingConverterLastErrorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object encodingConverterLastError(VirtualFrame frame, DynamicObject encodingConverter) {
            final EConv ec = Layouts.ENCODING_CONVERTER.getEconv(encodingConverter);
            final EConv.LastError lastError = ec.lastError;

            if (lastError.getResult() != EConvResult.InvalidByteSequence &&
                    lastError.getResult() != EConvResult.IncompleteInput &&
                    lastError.getResult() != EConvResult.UndefinedConversion) {
                return nil();
            }

            Object ret = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new");

            lookupTableWriteNode.call(frame, ret, "[]=", getSymbol("result"), eConvResultToSymbol(lastError.getResult()));
            lookupTableWriteNode.call(frame, ret, "[]=", getSymbol("source_encoding_name"), createString(new ByteList(lastError.getSource())));
            lookupTableWriteNode.call(frame, ret, "[]=", getSymbol("destination_encoding_name"), createString(new ByteList(lastError.getDestination())));
            lookupTableWriteNode.call(frame, ret, "[]=", getSymbol("error_bytes"), createString(new ByteList(lastError.getErrorBytes(),
                lastError.getErrorBytesP(), lastError.getErrorBytesP() + lastError.getErrorBytesLength())));

            if (lastError.getReadAgainLength() != 0) {
                lookupTableWriteNode.call(frame, ret, "[]=", getSymbol("read_again_bytes"), createString(new ByteList(lastError.getErrorBytes(),
                    lastError.getErrorBytesLength() + lastError.getErrorBytesP(),
                    lastError.getReadAgainLength())));
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

            throw new UnsupportedOperationException(StringUtils.format("Unknown EConv result: %s", result));
        }

    }

    @Primitive(name = "encoding_converter_primitive_errinfo")
    public static abstract class EncodingConverterErrinfoNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object encodingConverterLastError(DynamicObject encodingConverter) {
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

            return createArray(ret, ret.length);
        }

    }

}
