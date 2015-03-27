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
import org.jcodings.transcode.Transcoder;
import org.jcodings.transcode.TranscoderDB;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.KeyValue;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

import java.util.ArrayList;
import java.util.List;

@CoreClass(name = "Encoding::Converter")
public abstract class EncodingConverterNodes {

    @RubiniusOnly
    @CoreMethod(names = "initialize_jruby", required = 2, optional = 1)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
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

    }

    @RubiniusOnly
    @CoreMethod(names = "transcoding_map", onSingleton = true)
    public abstract static class TranscodingMapNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode upcaseNode;
        @Child private CallDispatchHeadNode toSymNode;
        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        public TranscodingMapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            upcaseNode = DispatchHeadNodeFactory.createMethodCall(context);
            toSymNode = DispatchHeadNodeFactory.createMethodCall(context);
            newLookupTableNode = DispatchHeadNodeFactory.createMethodCall(context);
            lookupTableWriteNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public TranscodingMapNode(TranscodingMapNode prev) {
            super(prev);
            upcaseNode = prev.upcaseNode;
            toSymNode = prev.toSymNode;
            newLookupTableNode = prev.newLookupTableNode;
            lookupTableWriteNode = prev.lookupTableWriteNode;
        }

        @Specialization
        public RubyHash transcodingMap(VirtualFrame frame) {
            List<KeyValue> entries = new ArrayList<>();

            for (RubyEncoding e : RubyEncoding.cloneEncodingList()) {
                final Object upcased = upcaseNode.call(frame, getContext().makeString(e.getName()), "upcase", null);
                final Object key = toSymNode.call(frame, upcased, "to_sym", null);
                final Object value = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

                final Object tupleValues = new Object[2];


                lookupTableWriteNode.call(frame, value, "[]=", null, key, nil());

                entries.add(new KeyValue(key, value));
            }

            return HashOperations.verySlowFromEntries(getContext().getCoreLibrary().getHashClass(), entries, true);
        }
    }

}
