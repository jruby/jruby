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
import org.joni.Option;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.util.ByteList;
import org.jruby.util.cli.Options;

import static org.jruby.util.StringSupport.CR_7BIT;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

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
            notDesignedForCompilation();

            return ((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), a.getSource(), a.getRegex().getOptions()).to_s()).getByteList().equals(((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), b.getSource(), b.getRegex().getOptions()).to_s()).getByteList());
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

            return regexp.matchCommon(string.getBytes(), true, false) != getContext().getCoreLibrary().getNilObject();
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
            return regexp.matchCommon(string.getBytes(), true, true);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyBasicObject other) {
            notDesignedForCompilation();

            if (other instanceof RubyString) {
                return match(regexp, (RubyString) other);
            } else {
                return getContext().getCoreLibrary().getNilObject();
            }
        }

    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingNode(EncodingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding encoding(RubyRegexp regexp) {
            notDesignedForCompilation();
            return regexp.getEncoding();
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

    @CoreMethod(names = "initialize", required = 1, optional = 1, lowerFixnumParameters = 2)
    public abstract static class InitializeNode extends CoreMethodNode {

        private ConditionProfile booleanOptionsProfile = ConditionProfile.createBinaryProfile();

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyString string, UndefinedPlaceholder options) {
            notDesignedForCompilation();

            regexp.initialize(this, string.getBytes(), Option.DEFAULT);
            return regexp;
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyString string, RubyNilClass options) {
            notDesignedForCompilation();

            return initialize(regexp, string, UndefinedPlaceholder.INSTANCE);
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyString string, boolean options) {
            notDesignedForCompilation();

            if (booleanOptionsProfile.profile(options)) {
                return initialize(regexp, string, Option.IGNORECASE);
            } else {
                return initialize(regexp, string, UndefinedPlaceholder.INSTANCE);
            }
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyString string, int options) {
            notDesignedForCompilation();

            regexp.initialize(this, string.getBytes(), options);
            return regexp;
        }

        @Specialization(guards = { "!isRubyNilClass(arguments[2])", "!isUndefinedPlaceholder(arguments[2])" })
        public RubyRegexp initialize(RubyRegexp regexp, RubyString string, Object options) {
            notDesignedForCompilation();

            return initialize(regexp, string, Option.IGNORECASE);
        }

        @Specialization
        public RubyRegexp initialize(RubyRegexp regexp, RubyRegexp from, UndefinedPlaceholder options) {
            notDesignedForCompilation();

            regexp.initialize(this, from.getSource(), from.getRegex().getOptions()); // TODO: is copying needed?
            return regexp;
        }

        @Specialization(guards = "!isUndefinedPlaceholder(arguments[2])")
        public RubyRegexp initialize(RubyRegexp regexp, RubyRegexp from, Object options) {
            notDesignedForCompilation();

            if (Options.PARSER_WARN_FLAGS_IGNORED.load()) {
                getContext().getWarnings().warn("flags ignored");
            }

            return initialize(regexp, from, UndefinedPlaceholder.INSTANCE);
        }
    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization
        public Object initializeCopy(RubyRegexp self, RubyRegexp from) {
            notDesignedForCompilation();

            if (self == from) {
                return self;
            }

            self.initialize(this, from.getSource(), from.getRegex().getOptions()); // TODO: is copying needed?

            return self;
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

    @CoreMethod(names = "match", required = 1)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyRegexp regexp, RubyString string) {
            return regexp.matchCommon(string.getBytes(), false, false);
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

            return regexp.getRegex().getOptions();
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
            return getContext().makeString(regexp.getSource().toString());
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
