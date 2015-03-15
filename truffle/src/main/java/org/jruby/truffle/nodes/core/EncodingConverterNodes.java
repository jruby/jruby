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
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.Encoding;
import org.jcodings.transcode.EConv;
import org.jcodings.transcode.Transcoder;
import org.jcodings.transcode.TranscoderDB;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

@CoreClass(name = "Encoding::Converter")
public abstract class EncodingConverterNodes {

    @CoreMethod(names = "convpath")
    public abstract static class ConvPathNode extends CoreMethodNode {

        public ConvPathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConvPathNode(ConvPathNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyArray convpath(RubyEncodingConverter converter) {
            notDesignedForCompilation();

            // Adapated from RubyConverter - see attribution there

            Ruby runtime = getContext().getRuntime();

            EConv ec = converter.getEConv();

            Object[] result = new Object[ec.numTranscoders];
            int r = 0;

            for (int i = 0; i < ec.numTranscoders; i++) {
                Transcoder tr = ec.elements[i].transcoding.transcoder;
                Object v;
                if (EncodingUtils.DECORATOR_P(tr.getSource(), tr.getDestination())) {
                    v = new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(tr.getDestination()));
                } else {
                    Encoding source = runtime.getEncodingService().findEncodingOrAliasEntry(tr.getSource()).getEncoding();
                    Encoding destination = runtime.getEncodingService().findEncodingOrAliasEntry(tr.getDestination()).getEncoding();

                    v = new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                            new Object[]{
                                RubyEncoding.getEncoding(source),
                                RubyEncoding.getEncoding(destination)
                            }, 2);
                }
                result[r++] = v;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), result, result.length);
        }

    }

    @CoreMethod(names = "initialize", required = 2)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyNilClass initialize(RubyEncodingConverter self, RubyString source, RubyString destination) {
            notDesignedForCompilation();

            // Adapted from RubyConverter - see attribution there

            Ruby runtime = getContext().getRuntime();
            Encoding[] encs = {null, null};
            byte[][] encNames = {null, null};
            int[] ecflags = {0};
            IRubyObject[] ecopts = {runtime.getNil()};

            EncodingUtils.econvArgs(runtime.getCurrentContext(), new IRubyObject[]{getContext().toJRuby(source), getContext().toJRuby(destination)}, encNames, encs, ecflags, ecopts);
            EConv econv = EncodingUtils.econvOpenOpts(runtime.getCurrentContext(), encNames[0], encNames[1], ecflags[0], ecopts[0]);

            if (econv == null) {
                throw new UnsupportedOperationException();
            }

            self.setEConv(econv);

            return nil();
        }

    }

    @CoreMethod(names = "search_convpath", onSingleton = true, required = 2)
    public abstract static class SearchConvPathNode extends CoreMethodNode {

        public SearchConvPathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SearchConvPathNode(SearchConvPathNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyArray searchConvpath(RubyString source, RubyString destination) {
            notDesignedForCompilation();

            // Adapted from RubyConverter - see attribution there

            final Ruby runtime = getContext().getRuntime();
            final RubyNilClass nil = nil();
            ThreadContext context = runtime.getCurrentContext();
            final byte[][] encNames = {null, null};
            final Encoding[] encs = {null, null};
            final int[] ecflags_p = {0};
            final IRubyObject[] ecopts_p = {context.nil};
            final Object[] convpath = {nil()};

            EncodingUtils.econvArgs(context, new IRubyObject[]{getContext().toJRuby(source), getContext().toJRuby(destination)}, encNames, encs, ecflags_p, ecopts_p);

            TranscoderDB.searchPath(encNames[0], encNames[1], new TranscoderDB.SearchPathCallback() {

                public void call(byte[] source, byte[] destination, int depth) {
                    Object v;

                    if (convpath[0] == nil) {
                        convpath[0] = new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
                    }

                    if (EncodingUtils.DECORATOR_P(encNames[0], encNames[1])) {
                        v = new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(encNames[2]));
                    } else {
                        Encoding sourceEncoding = runtime.getEncodingService().findEncodingOrAliasEntry(source).getEncoding();
                        Encoding destinationEncoding = runtime.getEncodingService().findEncodingOrAliasEntry(destination).getEncoding();

                        v = new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                                new Object[]{
                                        RubyEncoding.getEncoding(destinationEncoding),
                                        RubyEncoding.getEncoding(sourceEncoding)
                                }, 2);
                    }

                    ((RubyArray) convpath[0]).slowPush(v); // depth?
                }
            });

            if (convpath[0] == nil) {
                throw new UnsupportedOperationException();
            }

            //if (EncodingUtils.decorateConvpath(context, convpath[0], ecflags_p[0]) == -1) {
            //    throw EncodingUtils.econvOpenExc(context, encNames[0], encNames[1], ecflags_p[0]);
            //}

            return (RubyArray) convpath[0];
        }

    }

}
