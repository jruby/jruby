/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.transcode.TranscoderDB;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.Hash;
import org.jruby.Ruby;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

@CoreClass(name = "Encoding::Converter")
public abstract class EncodingConverterNodes {

    @RubiniusOnly
    @CoreMethod(names = "initialize_jruby", required = 2, optional = 1, visibility = Visibility.PRIVATE)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyNilClass initialize(RubyEncodingConverter self, Object source, Object destination, Object options) {
            notDesignedForCompilation();

            // Adapted from RubyConverter - see attribution there

            Ruby runtime = getContext().getRuntime();
            Encoding[] encs = {null, null};
            byte[][] encNames = {null, null};
            int[] ecflags = {0};
            IRubyObject[] ecopts = {runtime.getNil()};

            final IRubyObject sourceAsJRubyObj = getContext().toJRuby(source);
            final IRubyObject destinationAsJRubyObj = getContext().toJRuby(destination);

            EncodingUtils.econvArgs(runtime.getCurrentContext(), new IRubyObject[]{sourceAsJRubyObj, destinationAsJRubyObj}, encNames, encs, ecflags, ecopts);

            // This method should only be called after the Encoding::Converter instance has already been initialized
            // by Rubinius.  Rubinius will do the heavy lifting of parsing the options hash and setting the `@options`
            // ivar to the resulting int for EConv flags.  Since we don't pass the proper data structures to EncodingUtils,
            // we must override the flags after its had a pass in order to correct the bad flags value.
            ecflags[0] = rubiniusToJRubyFlags((int) self.getInstanceVariable("@options"));

            EConv econv = EncodingUtils.econvOpenOpts(runtime.getCurrentContext(), encNames[0], encNames[1], ecflags[0], ecopts[0]);

            if (econv == null) {
                throw new UnsupportedOperationException();
            }

            if (!EncodingUtils.DECORATOR_P(encNames[0], encNames[1])) {
                if (encs[0] == null) {
                    encs[0] = EncodingDB.dummy(encNames[0]).getEncoding();
                }
                if (encs[1] == null) {
                    encs[1] = EncodingDB.dummy(encNames[1]).getEncoding();
                }
            }

            econv.sourceEncoding = encs[0];
            econv.destinationEncoding = encs[1];

            self.setEConv(econv);

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

    @RubiniusOnly
    @CoreMethod(names = "transcoding_map", onSingleton = true)
    public abstract static class TranscodingMapNode extends CoreMethodNode {

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
            final Object ret = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

            for (CaseInsensitiveBytesHash<TranscoderDB.Entry> sourceEntry : TranscoderDB.transcoders) {
                Object key = null;
                final Object value = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

                for (Hash.HashEntry<TranscoderDB.Entry> destinationEntry : sourceEntry.entryIterator()) {
                    final TranscoderDB.Entry e = destinationEntry.value;

                    if (key == null) {
                        final Object upcased = upcaseNode.call(frame, getContext().makeString(new ByteList(e.getSource())), "upcase", null);
                        key = toSymNode.call(frame, upcased, "to_sym", null);
                    }

                    final Object upcasedLookupTableKey = upcaseNode.call(frame, getContext().makeString(new ByteList(e.getDestination())), "upcase", null);
                    final Object lookupTableKey = toSymNode.call(frame, upcasedLookupTableKey, "to_sym", null);
                    final Object lookupTableValue = newTranscodingNode.call(frame, getContext().getCoreLibrary().getTranscodingClass(), "create", null, key, lookupTableKey);
                    lookupTableWriteNode.call(frame, value, "[]=", null, lookupTableKey, lookupTableValue);
                }

                lookupTableWriteNode.call(frame, ret, "[]=", null, key, value);
            }

            return ret;
        }
    }

}
