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

import static org.jruby.util.StringSupport.CR_7BIT;

import org.joni.Region;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyMatchData;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.util.ByteList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.util.RegexpOptions;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(RubyRegexp a, RubyRegexp b) {
            if (a == b) {
                return true;
            }

            if (a.getRegex().getOptions() != b.getRegex().getOptions()) {
                return false;
            }

            if (a.getSource().getEncoding() != b.getSource().getEncoding()) {
                return false;
            }

            return a.getSource().equal(b.getSource());
        }

        @Specialization(guards = "!isRubyRegexp(b)")
        public boolean equal(RubyRegexp a, Object b) {
            return false;
        }

    }

    public abstract static class EscapingNode extends CoreMethodNode {

        @Child private EscapeNode escapeNode;

        public EscapingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EscapingNode(EscapingNode prev) {
            super(prev);
        }

        protected RubyString escape(VirtualFrame frame, RubyString string) {
            if (escapeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                escapeNode = insert(RegexpNodesFactory.EscapeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }
            return escapeNode.executeEscape(frame, string);
        }
    }

    public abstract static class EscapingYieldingNode extends YieldingCoreMethodNode {
        @Child private EscapeNode escapeNode;

        public EscapingYieldingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EscapingYieldingNode(EscapingYieldingNode prev) {
            super(prev);
        }

        protected RubyString escape(VirtualFrame frame, RubyString string) {
            if (escapeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                escapeNode = insert(RegexpNodesFactory.EscapeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }
            return escapeNode.executeEscape(frame, string);
        }
    }

    @CoreMethod(names = "===", required = 1)
    public abstract static class CaseEqualNode extends CoreMethodNode {

        public CaseEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CaseEqualNode(CaseEqualNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            notDesignedForCompilation();

            return regexp.matchCommon(string, true, false) != nil();
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubySymbol symbol) {
            notDesignedForCompilation();

            return regexp.matchCommon(symbol.toRubyString(), true, false) != nil();
        }

    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodNode {

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchOperatorNode(MatchOperatorNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            return regexp.matchCommon(string, true, true);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyBasicObject other) {
            notDesignedForCompilation();

            if (other instanceof RubyString) {
                return match(regexp, (RubyString) other);
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "escape", onSingleton = true, required = 1)
    public abstract static class EscapeNode extends CoreMethodNode {

        public EscapeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EscapeNode(EscapeNode prev) {
            super(prev);
        }

        public abstract RubyString executeEscape(VirtualFrame frame, RubyString pattern);

        @Specialization
        public RubyString escape(RubyString pattern) {
            notDesignedForCompilation();

            return getContext().makeString(org.jruby.RubyRegexp.quote19(new ByteList(pattern.getBytes()), true).toString());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public int hash(RubyRegexp regexp) {
            int options = regexp.getRegex().getOptions() & ~32 /* option n, NO_ENCODING in common/regexp.rb */;
            return options ^ regexp.getSource().hashCode();
        }

    }

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString match(RubyRegexp regexp) {
            return new RubyString(getContext().getCoreLibrary().getStringClass(), ((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), regexp.getSource(), regexp.getRegex().getOptions()).inspect19()).getByteList());
        }

    }

    @CoreMethod(names = "match", required = 1, taintFromSelf = true)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            return regexp.matchCommon(string, false, false);
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "match_start", required = 2)
    public abstract static class MatchStartNode extends CoreMethodNode {

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchStartNode(MatchStartNode prev) {
            super(prev);
        }

        @Specialization
        public Object matchStart(RubyRegexp regexp, RubyString string, int startPos) {
            final Object matchResult = regexp.matchCommon(string, false, false, startPos);
            if (matchResult instanceof RubyMatchData && ((RubyMatchData) matchResult).getNumberOfRegions() > 0
                && ((RubyMatchData) matchResult).getRegion().beg[0] == startPos) {
                return matchResult;
            }
            return nil();
        }
    }

    @CoreMethod(names = "options")
    public abstract static class OptionsNode extends CoreMethodNode {

        private final ConditionProfile notYetInitializedProfile = ConditionProfile.createBinaryProfile();

        public OptionsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OptionsNode(OptionsNode prev) {
            super(prev);
        }

        @Specialization
        public int options(RubyRegexp regexp) {
            notDesignedForCompilation();

            if (notYetInitializedProfile.profile(regexp.getRegex() == null)) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeError("uninitialized Regexp", this));
            }
            if(regexp.getOptions() != null){
                return regexp.getOptions().toOptions();
            }

            return RegexpOptions.fromJoniOptions(regexp.getRegex().getOptions()).toOptions();
        }

    }

    @CoreMethod(names = { "quote", "escape" }, needsSelf = false, onSingleton = true, required = 1)
    public abstract static class QuoteNode extends CoreMethodNode {

        public QuoteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public QuoteNode(QuoteNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString quote(RubyString raw) {
            notDesignedForCompilation();

            boolean isAsciiOnly = raw.getByteList().getEncoding().isAsciiCompatible() && raw.scanForCodeRange() == CR_7BIT;

            return getContext().makeString(org.jruby.RubyRegexp.quote19(raw.getBytes(), isAsciiOnly));
        }

        @Specialization
        public RubyString quote(RubySymbol raw) {
            notDesignedForCompilation();

            return quote(raw.toRubyString());
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodNode {

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SearchFromNode(SearchFromNode prev) {
            super(prev);
        }

        @Specialization
        public Object searchFrom(RubyRegexp regexp, RubyString string, int startPos) {
            return regexp.matchCommon(string, false, false, startPos);
        }
    }

    @CoreMethod(names = "source")
    public abstract static class SourceNode extends CoreMethodNode {

        public SourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SourceNode(SourceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString source(RubyRegexp regexp) {
            return getContext().makeString(regexp.getSource().dup());
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString to_s(RubyRegexp regexp) {
            return new RubyString(getContext().getCoreLibrary().getStringClass(), ((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), regexp.getSource(), regexp.getRegex().getOptions()).to_s()).getByteList());
        }

    }

}
