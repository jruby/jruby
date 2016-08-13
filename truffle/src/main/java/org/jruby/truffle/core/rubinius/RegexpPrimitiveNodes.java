/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.object.DynamicObject;
import org.joni.Matcher;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.regexp.RegexpGuards;
import org.jruby.truffle.core.regexp.RegexpNodes;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;
import org.jruby.truffle.util.StringUtils;

/**
 * Rubinius primitives associated with the Ruby {@code Regexp} class.

 */
public abstract class RegexpPrimitiveNodes {

    @Primitive(name = "regexp_fixed_encoding_p")
    public static abstract class RegexpFixedEncodingPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public boolean fixedEncoding(DynamicObject regexp) {
            return Layouts.REGEXP.getOptions(regexp).isFixed();
        }

    }

    @Primitive(name = "regexp_initialize", lowerFixnum = 2)
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpInitializePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = {"isRegexpLiteral(regexp)", "isRubyString(pattern)"})
        public DynamicObject initializeRegexpLiteral(DynamicObject regexp, DynamicObject pattern, int options) {
            throw new RaiseException(coreExceptions().securityError("can't modify literal regexp", this));
        }

        @Specialization(guards = {"!isRegexpLiteral(regexp)", "isInitialized(regexp)", "isRubyString(pattern)"})
        public DynamicObject initializeAlreadyInitialized(DynamicObject regexp, DynamicObject pattern, int options) {
            throw new RaiseException(coreExceptions().typeError("already initialized regexp", this));
        }

        @Specialization(guards = {"!isRegexpLiteral(regexp)", "!isInitialized(regexp)", "isRubyString(pattern)"})
        public DynamicObject initialize(DynamicObject regexp, DynamicObject pattern, int options) {
            RegexpNodes.initialize(getContext(), regexp, this, StringOperations.rope(pattern), options);
            return regexp;
        }

    }

    @Primitive(name = "regexp_options")
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpOptionsPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isInitialized(regexp)")
        public int options(DynamicObject regexp) {
            return Layouts.REGEXP.getOptions(regexp).toOptions();
        }

        @Specialization(guards = "!isInitialized(regexp)")
        public int optionsNotInitialized(DynamicObject regexp) {
            throw new RaiseException(coreExceptions().typeError("uninitialized Regexp", this));
        }

    }

    @Primitive(name = "regexp_search_region", lowerFixnum = { 2, 3 })
    @ImportStatic(RegexpGuards.class)
    public static abstract class RegexpSearchRegionPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = {"!isInitialized(regexp)", "isRubyString(string)"})
        public Object searchRegionNotInitialized(DynamicObject regexp, DynamicObject string, int start, int end, boolean forward) {
            throw new RaiseException(coreExceptions().typeError("uninitialized Regexp", this));
        }

        @Specialization(guards = {"isRubyString(string)", "!isValidEncoding(string)"})
        public Object searchRegionInvalidEncoding(DynamicObject regexp, DynamicObject string, int start, int end, boolean forward) {
            throw new RaiseException(coreExceptions().argumentError(formatError(string), this));
        }

        @TruffleBoundary
        private String formatError(DynamicObject string) {
            return StringUtils.format("invalid byte sequence in %s", Layouts.STRING.getRope(string).getEncoding());
        }

        @TruffleBoundary
        @Specialization(guards = {"isInitialized(regexp)", "isRubyString(string)", "isValidEncoding(string)"})
        public Object searchRegion(DynamicObject regexp, DynamicObject string, int start, int end, boolean forward,
                                   @Cached("createX()") RopeNodes.MakeSubstringNode makeSubstringNode) {
            final Matcher matcher = RegexpNodes.createMatcher(getContext(), regexp, string);

            if (forward) {
                // Search forward through the string.
                return RegexpNodes.matchCommon(getContext(), this, makeSubstringNode, regexp, string, false, matcher, start, end);
            } else {
                // Search backward through the string.
                return RegexpNodes.matchCommon(getContext(), this, makeSubstringNode, regexp, string, false, matcher, end, start);
            }
        }

    }

    @Primitive(name = "regexp_set_last_match", needsSelf = false)
    public static abstract class RegexpSetLastMatchPrimitiveNode extends PrimitiveArrayArgumentsNode {

        public static RegexpSetLastMatchPrimitiveNode create() {
            return RegexpPrimitiveNodesFactory.RegexpSetLastMatchPrimitiveNodeFactory.create(null);
        }

        public abstract DynamicObject executeSetLastMatch(Object matchData);

        @TruffleBoundary
        @Specialization
        public DynamicObject setLastMatchData(DynamicObject matchData) {
            // TODO (nirvdrum 08-Aug-16): Validate that the matchData is either nil or a MatchData object, otherwise throw an exception. It should use the same logic as assigning $~ does in the translator.

            Frame frame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_WRITE, true);
            FrameSlot slot = frame.getFrameDescriptor().findFrameSlot("$~");

            while (slot == null) {
                final Frame nextFrame = RubyArguments.getDeclarationFrame(frame);

                if (nextFrame == null) {
                    slot = frame.getFrameDescriptor().addFrameSlot("$~", FrameSlotKind.Object);
                } else {
                    slot = nextFrame.getFrameDescriptor().findFrameSlot("$~");
                    frame = nextFrame;
                }
            }

            final Object previousMatchData;
            try {
                previousMatchData = frame.getObject(slot);

                if (previousMatchData instanceof ThreadLocalObject) {
                    final ThreadLocalObject threadLocalObject = (ThreadLocalObject) previousMatchData;

                    threadLocalObject.set(matchData);
                } else {
                    frame.setObject(slot, ThreadLocalObject.wrap(getContext(), matchData));
                }
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException(e);
            }

            return matchData;
        }

    }

    @Primitive(name = "regexp_set_block_last_match", needsSelf = false)
    public static abstract class RegexpSetBlockLastMatchPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyProc(block)")
        public Object setBlockLastMatch(DynamicObject block, DynamicObject matchData) {

            Frame callerFrame = Layouts.PROC.getDeclarationFrame(block);

            if (callerFrame == null) {
                return matchData;
            }

            Frame tempFrame = RubyArguments.getDeclarationFrame(callerFrame);

            while (tempFrame != null) {
                callerFrame = tempFrame;
                tempFrame = RubyArguments.getDeclarationFrame(callerFrame);
            }

            final FrameDescriptor callerFrameDescriptor = callerFrame.getFrameDescriptor();

            try {
                final FrameSlot frameSlot = callerFrameDescriptor.findFrameSlot("$~");

                if (frameSlot == null) {
                    return matchData;
                }

                final Object matchDataHolder = callerFrame.getObject(frameSlot);

                if (matchDataHolder instanceof ThreadLocalObject) {
                    ((ThreadLocalObject) matchDataHolder).set(matchData);
                } else {
                    callerFrame.setObject(frameSlot, ThreadLocalObject.wrap(getContext(), matchData));
                }
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException(e);
            }

            return matchData;
        }

    }
    
}
