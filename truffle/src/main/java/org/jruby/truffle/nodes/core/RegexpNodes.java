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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

import static org.jruby.util.StringSupport.CR_7BIT;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

    public abstract static class EscapingNode extends CoreMethodArrayArgumentsNode {

        @Child private EscapeNode escapeNode;

        public EscapingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        protected RubyString escape(VirtualFrame frame, RubyString string) {
            if (escapeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                escapeNode = insert(RegexpNodesFactory.EscapeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }
            return escapeNode.executeEscape(frame, string);
        }
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toSNode;
        @Child private ToStrNode toStrNode;

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            return regexp.matchCommon(string, true, true);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyRegexp regexp, RubySymbol symbol) {
            if (toSNode == null) {
                CompilerDirectives.transferToInterpreter();
                toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return match(regexp, (RubyString) toSNode.call(frame, symbol, "to_s", null));
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyNilClass nil) {
            return nil();
        }

        @Specialization(guards = { "!isRubyString(other)", "!isRubySymbol(other)", "!isRubyNilClass(other)" })
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

        public abstract RubyString executeEscape(VirtualFrame frame, RubyString pattern);

        @Specialization
        public RubyString escape(RubyString pattern) {
            notDesignedForCompilation();

            return getContext().makeString(org.jruby.RubyRegexp.quote19(new ByteList(pattern.getByteList()), true).toString());
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

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodArrayArgumentsNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString match(RubyRegexp regexp) {
            return new RubyString(getContext().getCoreLibrary().getStringClass(), ((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), regexp.getSource(), regexp.getRegex().getOptions()).inspect19()).getByteList());
        }

    }

    @CoreMethod(names = "match", required = 1, taintFromSelf = true)
    public abstract static class MatchNode extends CoreMethodArrayArgumentsNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            return regexp.matchCommon(string, false, false);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyNilClass nil) {
            return nil();
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "match_start", required = 2)
    public abstract static class MatchStartNode extends CoreMethodArrayArgumentsNode {

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class OptionsNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile notYetInitializedProfile = ConditionProfile.createBinaryProfile();

        public OptionsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class QuoteNode extends CoreMethodArrayArgumentsNode {

        public QuoteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString quote(RubyString raw) {
            notDesignedForCompilation();

            boolean isAsciiOnly = raw.getByteList().getEncoding().isAsciiCompatible() && raw.scanForCodeRange() == CR_7BIT;

            return getContext().makeString(org.jruby.RubyRegexp.quote19(raw.getByteList(), isAsciiOnly));
        }

        @Specialization
        public RubyString quote(RubySymbol raw) {
            notDesignedForCompilation();

            return quote(raw.toRubyString());
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodArrayArgumentsNode {

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object searchFrom(RubyRegexp regexp, RubyString string, int startPos) {
            return regexp.matchCommon(string, false, false, startPos);
        }
    }

    @CoreMethod(names = "source")
    public abstract static class SourceNode extends CoreMethodArrayArgumentsNode {

        public SourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString source(RubyRegexp regexp) {
            return getContext().makeString(regexp.getSource().dup());
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString to_s(RubyRegexp regexp) {
            return new RubyString(getContext().getCoreLibrary().getStringClass(), ((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), regexp.getSource(), regexp.getRegex().getOptions()).to_s()).getByteList());
        }

    }

}
