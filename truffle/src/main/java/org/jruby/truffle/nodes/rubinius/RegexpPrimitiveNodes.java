/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.joni.Matcher;
import org.joni.Regex;
import org.jruby.truffle.nodes.core.RegexpGuards;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpSupport;

/**
 * Rubinius primitives associated with the Ruby {@code Regexp} class.
 * <p>
 * Also see {@link RubyRegexp}.

 */
public abstract class RegexpPrimitiveNodes {

    @RubiniusPrimitive(name = "regexp_fixed_encoding_p")
    public static abstract class RegexpFixedEncodingPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpFixedEncodingPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean fixedEncoding(RubyRegexp regexp) {
            return regexp.getOptions().isFixed();
        }

    }

    @RubiniusPrimitive(name = "regexp_initialize")
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpInitializePrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpInitializePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRegexpLiteral(regexp)", "isRubyString(pattern)"})
        public RubyRegexp initializeRegexpLiteral(RubyRegexp regexp, RubyBasicObject pattern, int options) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().securityError("can't modify literal regexp", this));
        }

        @Specialization(guards = {"!isRegexpLiteral(regexp)", "isInitialized(regexp)", "isRubyString(pattern)"})
        public RubyRegexp initializeAlreadyInitialized(RubyRegexp regexp, RubyBasicObject pattern, int options) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeError("already initialized regexp", this));
        }

        @Specialization(guards = {"!isRegexpLiteral(regexp)", "!isInitialized(regexp)", "isRubyString(pattern)"})
        public RubyRegexp initialize(RubyRegexp regexp, RubyBasicObject pattern, int options) {
            regexp.initialize(this, StringNodes.getByteList(pattern), options);
            return regexp;
        }

    }

    @RubiniusPrimitive(name = "regexp_options")
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpOptionsPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpOptionsPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isInitialized(regexp)")
        public int options(RubyRegexp regexp) {
            return regexp.getOptions().toOptions();
        }

        @Specialization(guards = "!isInitialized(regexp)")
        public int optionsNotInitialized(RubyRegexp regexp) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeError("uninitialized Regexp", this));
        }

    }

    @RubiniusPrimitive(name = "regexp_propagate_last_match")
    public static abstract class RegexpPropagateLastMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpPropagateLastMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject propagateLastMatch(RubyClass regexpClass) {
            // TODO (nirvdrum 08-Jun-15): This method seems to exist just to fix Rubinius's broken frame-local scoping.  This assertion needs to be verified, however.
            return nil();
        }

    }

    @RubiniusPrimitive(name = "regexp_search_region", lowerFixnumParameters = {1, 2})
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpSearchRegionPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpSearchRegionPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"!isInitialized(regexp)", "isRubyString(string)"})
        public Object searchRegionNotInitialized(RubyRegexp regexp, RubyBasicObject string, int start, int end, boolean forward) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeError("uninitialized Regexp", this));
        }

        @Specialization(guards = {"isRubyString(string)", "!isValidEncoding(string)"})
        public Object searchRegionInvalidEncoding(RubyRegexp regexp, RubyBasicObject string, int start, int end, boolean forward) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError(
                    String.format("invalid byte sequence in %s", StringNodes.getByteList(string).getEncoding()), this));
        }

        @TruffleBoundary
        @Specialization(guards = {"isInitialized(regexp)", "isRubyString(string)", "isValidEncoding(string)"})
        public Object searchRegion(RubyRegexp regexp, RubyBasicObject string, int start, int end, boolean forward) {
            final ByteList stringBl = StringNodes.getByteList(string);
            final ByteList bl = regexp.getSource();
            final Encoding enc = regexp.checkEncoding(StringNodes.getCodeRangeable(string), true);
            final ByteList preprocessed = RegexpSupport.preprocess(getContext().getRuntime(), bl, enc, new Encoding[]{null}, RegexpSupport.ErrorMode.RAISE);

            final Regex r = new Regex(preprocessed.getUnsafeBytes(), preprocessed.getBegin(), preprocessed.getBegin() + preprocessed.getRealSize(), regexp.getRegex().getOptions(), regexp.checkEncoding(StringNodes.getCodeRangeable(string), true));
            final Matcher matcher = r.matcher(stringBl.getUnsafeBytes(), stringBl.begin(), stringBl.begin() + stringBl.realSize());

            if (forward) {
                // Search forward through the string.
                return regexp.matchCommon(string, false, false, matcher, start + stringBl.begin(), end + stringBl.begin());
            } else {
                // Search backward through the string.
                return regexp.matchCommon(string, false, false, matcher, end + stringBl.begin(), start + stringBl.begin());
            }
        }

    }

    @RubiniusPrimitive(name = "regexp_set_last_match")
    public static abstract class RegexpSetLastMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpSetLastMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setLastMatch(RubyClass regexpClass, Object matchData) {
            RubyBasicObject.setInstanceVariable(
                    getContext().getThreadManager().getCurrentThread().getThreadLocals(), "$~", matchData);

            return matchData;
        }

    }

    @RubiniusPrimitive(name = "regexp_set_block_last_match")
    public static abstract class RegexpSetBlockLastMatchPrimitiveNode extends RubiniusPrimitiveNode {

        public RegexpSetBlockLastMatchPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject setBlockLastMatch(RubyClass regexpClass) {
            // TODO CS 7-Mar-15 what does this do?
            return nil();
        }

    }
    
}
