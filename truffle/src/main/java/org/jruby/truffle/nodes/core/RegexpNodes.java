/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.joni.NameEntry;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyMatchData;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.util.Iterator;

import static org.jruby.util.StringSupport.CR_7BIT;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toSNode;
        @Child private ToStrNode toStrNode;

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object match(RubyRegexp regexp, RubyBasicObject string) {
            return regexp.matchCommon(string, true, true);
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public Object match(VirtualFrame frame, RubyRegexp regexp, RubyBasicObject symbol) {
            if (toSNode == null) {
                CompilerDirectives.transferToInterpreter();
                toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return match(regexp, (RubyString) toSNode.call(frame, symbol, "to_s", null));
        }

        @Specialization(guards = "isNil(nil)")
        public Object match(RubyRegexp regexp, Object nil) {
            return nil();
        }

        @Specialization(guards = { "!isRubyString(other)", "!isRubySymbol(other)", "!isNil(other)" })
        public Object matchGeneric(VirtualFrame frame, RubyRegexp regexp, RubyBasicObject other) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return match(regexp, toStrNode.executeRubyString(frame, other));
        }

    }

    @CoreMethod(names = "escape", onSingleton = true, required = 1)
    public abstract static class EscapeNode extends CoreMethodArrayArgumentsNode {

        public EscapeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(pattern)")
        public RubyBasicObject escape(RubyBasicObject pattern) {
            return createString(org.jruby.RubyRegexp.quote19(new ByteList(StringNodes.getByteList(pattern)), true).toString());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubyRegexp regexp) {
            int options = regexp.getRegex().getOptions() & ~32 /* option n, NO_ENCODING in common/regexp.rb */;
            return options ^ regexp.getSource().hashCode();
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "match_start", required = 2)
    public abstract static class MatchStartNode extends CoreMethodArrayArgumentsNode {

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object matchStart(RubyRegexp regexp, RubyBasicObject string, int startPos) {
            final Object matchResult = regexp.matchCommon(string, false, false, startPos);
            if (matchResult instanceof RubyMatchData && ((RubyMatchData) matchResult).getNumberOfRegions() > 0
                && ((RubyMatchData) matchResult).getRegion().beg[0] == startPos) {
                return matchResult;
            }
            return nil();
        }
    }

    @CoreMethod(names = { "quote", "escape" }, needsSelf = false, onSingleton = true, required = 1)
    public abstract static class QuoteNode extends CoreMethodArrayArgumentsNode {

        public QuoteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(raw)")
        public RubyBasicObject quoteString(RubyBasicObject raw) {
            boolean isAsciiOnly = StringNodes.getByteList(raw).getEncoding().isAsciiCompatible() && StringNodes.scanForCodeRange(raw) == CR_7BIT;
            return createString(org.jruby.RubyRegexp.quote19(StringNodes.getByteList(raw), isAsciiOnly));
        }

        @Specialization(guards = "isRubySymbol(raw)")
        public RubyBasicObject quoteSymbol(RubyBasicObject raw) {
            return quoteString(StringNodes.createString(raw.getContext().getCoreLibrary().getStringClass(), SymbolNodes.getString(raw)));
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodArrayArgumentsNode {

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object searchFrom(RubyRegexp regexp, RubyBasicObject string, int startPos) {
            return regexp.matchCommon(string, false, false, startPos);
        }
    }

    @CoreMethod(names = "source")
    public abstract static class SourceNode extends CoreMethodArrayArgumentsNode {

        public SourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject source(RubyRegexp regexp) {
            return createString(regexp.getSource().dup());
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyRegexp regexp) {
            return createString(((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), regexp.getSource(), regexp.getRegex().getOptions()).to_s()).getByteList());
        }

    }

    @RubiniusOnly
    @NodeChild(value = "self")
    public abstract static class RubiniusNamesNode extends RubyNode {

        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        public RubiniusNamesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "!anyNames(regexp)")
        public RubyBasicObject rubiniusNamesNoCaptures(RubyRegexp regexp) {
            return nil();
        }

        @Specialization(guards = "anyNames(regexp)")
        public Object rubiniusNames(VirtualFrame frame, RubyRegexp regexp) {
            if (regexp.getCachedNames() != null) {
                return regexp.getCachedNames();
            }

            if (newLookupTableNode == null) {
                CompilerDirectives.transferToInterpreter();
                newLookupTableNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            if (lookupTableWriteNode == null) {
                CompilerDirectives.transferToInterpreter();
                lookupTableWriteNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object namesLookupTable = newLookupTableNode.call(frame, getContext().getCoreLibrary().getLookupTableClass(), "new", null);

            for (final Iterator<NameEntry> i = regexp.getRegex().namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final RubyBasicObject name = getSymbol(new ByteList(e.name, e.nameP, e.nameEnd - e.nameP, false));

                final int[] backrefs = e.getBackRefs();
                final RubyBasicObject backrefsRubyArray = ArrayNodes.createArray(getContext().getCoreLibrary().getArrayClass(), backrefs, backrefs.length);

                lookupTableWriteNode.call(frame, namesLookupTable, "[]=", null, name, backrefsRubyArray);
            }

            regexp.setCachedNames(namesLookupTable);

            return namesLookupTable;
        }

        public static boolean anyNames(RubyRegexp regexp) {
            return regexp.getRegex().numberOfNames() > 0;
        }
    }

}
