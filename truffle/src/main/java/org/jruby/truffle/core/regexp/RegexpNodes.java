/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
/*
 * Copyright (c) 2013, 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.regexp;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.Syntax;
import org.joni.exception.SyntaxException;
import org.joni.exception.ValueException;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.regexp.RegexpNodesFactory.RegexpSetLastMatchPrimitiveNodeFactory;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObject;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpSupport;
import org.jruby.util.RegexpSupport.ErrorMode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

@CoreClass("Regexp")
public abstract class RegexpNodes {

    @TruffleBoundary
    public static Matcher createMatcher(RubyContext context, DynamicObject regexp, DynamicObject string) {
        final Rope stringRope = StringOperations.rope(string);
        final Encoding enc = checkEncoding(regexp, stringRope, true);
        Regex regex = Layouts.REGEXP.getRegex(regexp);

        if (regex.getEncoding() != enc) {
            final Encoding[] fixedEnc = new Encoding[] { null };
            final ByteList sourceByteList = RopeOperations.getByteListReadOnly(Layouts.REGEXP.getSource(regexp));
            final ByteList preprocessed = ClassicRegexp.preprocess(context, sourceByteList, enc, fixedEnc, ErrorMode.RAISE);
            final RegexpOptions options = Layouts.REGEXP.getOptions(regexp);
            final Encoding newEnc = checkEncoding(regexp, stringRope, true);
            regex = new Regex(preprocessed.getUnsafeBytes(), preprocessed.getBegin(), preprocessed.getBegin() + preprocessed.getRealSize(),
                    options.toJoniOptions(), newEnc);
            assert enc == newEnc;
        }

        return regex.matcher(stringRope.getBytes(), 0, stringRope.byteLength());
    }

    @TruffleBoundary
    public static DynamicObject matchCommon(RubyContext context, Node currentNode, RopeNodes.MakeSubstringNode makeSubstringNode, DynamicObject regexp, DynamicObject string,
            boolean setNamedCaptures, int startPos) {
        final Matcher matcher = createMatcher(context, regexp, string);
        int range = StringOperations.rope(string).byteLength();
        return matchCommon(context, currentNode, makeSubstringNode, regexp, string, setNamedCaptures, matcher, startPos, range);
    }

    @TruffleBoundary
    public static DynamicObject matchCommon(RubyContext context, Node currentNode, RopeNodes.MakeSubstringNode makeSubstringNode, DynamicObject regexp, DynamicObject string,
            boolean setNamedCaptures, Matcher matcher, int startPos, int range) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(string);

        final Rope sourceRope = StringOperations.rope(string);

        int match = context.getThreadManager().runUntilResult(currentNode, () -> matcher.searchInterruptible(startPos, range, Option.DEFAULT));

        final DynamicObject nil = context.getCoreLibrary().getNilObject();

        if (match == -1) {
            if (setNamedCaptures && Layouts.REGEXP.getRegex(regexp).numberOfNames() > 0) {
                final Frame frame = context.getCallStack().getCallerFrameIgnoringSend().getFrame(FrameAccess.READ_WRITE, false);
                for (Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                    setLocalVariable(frame, name, nil);
                }
            }

            return nil;
        }

        assert match >= 0;

        final Region region = matcher.getEagerRegion();
        final Object[] values = new Object[region.numRegs];

        for (int n = 0; n < region.numRegs; n++) {
            final int start = region.beg[n];
            final int end = region.end[n];

            if (start > -1 && end > -1) {
                values[n] = createSubstring(makeSubstringNode, string, start, end - start);
            } else {
                values[n] = nil;
            }
        }

        final DynamicObject pre = createSubstring(makeSubstringNode, string, 0, region.beg[0]);
        final DynamicObject post = createSubstring(makeSubstringNode, string, region.end[0], sourceRope.byteLength() - region.end[0]);
        final DynamicObject global = createSubstring(makeSubstringNode, string, region.beg[0], region.end[0] - region.beg[0]);

        final DynamicObject matchData = Layouts.MATCH_DATA.createMatchData(context.getCoreLibrary().getMatchDataFactory(),
                string, regexp, region, values, pre, post, global, null);

        if (setNamedCaptures && Layouts.REGEXP.getRegex(regexp).numberOfNames() > 0) {
            final Frame frame = context.getCallStack().getCallerFrameIgnoringSend().getFrame(FrameAccess.READ_WRITE, false);
            for (Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                int nth = Layouts.REGEXP.getRegex(regexp).nameToBackrefNumber(e.name, e.nameP, e.nameEnd, region);

                final Object value;

                // Copied from jruby/RubyRegexp - see copyright notice there

                if (nth >= region.numRegs || (nth < 0 && (nth+=region.numRegs) <= 0)) {
                    value = nil;
                } else {
                    final int start = region.beg[nth];
                    final int end = region.end[nth];
                    if (start != -1) {
                        value = createSubstring(makeSubstringNode, string, start, end - start);
                    } else {
                        value = nil;
                    }
                }

                setLocalVariable(frame, name, value);
            }
        }

        return matchData;
    }

    // because the factory is not constant
    @TruffleBoundary
    private static DynamicObject createSubstring(RopeNodes.MakeSubstringNode makeSubstringNode, DynamicObject source, int start, int length) {
        assert RubyGuards.isRubyString(source);

        final Rope sourceRope = StringOperations.rope(source);
        final Rope substringRope = makeSubstringNode.executeMake(sourceRope, start, length);

        final DynamicObject ret = Layouts.STRING.createString(Layouts.CLASS.getInstanceFactory(Layouts.BASIC_OBJECT.getLogicalClass(source)), substringRope);

        return ret;
    }

    private static void setLocalVariable(Frame frame, String name, Object value) {
        assert value != null;

        while (frame != null) {
            final FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(name);
            if (slot != null) {
                frame.setObject(slot, value);
                break;
            }

            frame = RubyArguments.getDeclarationFrame(frame);
        }
    }

    public static Rope shimModifiers(Rope bytes) {
        // Joni doesn't support (?u) etc but we can shim some common cases

        String bytesString = bytes.toString();

        if (bytesString.startsWith("(?u)") || bytesString.startsWith("(?d)") || bytesString.startsWith("(?a)")) {
            final char modifier = (char) bytes.get(2);
            bytesString = bytesString.substring(4);

            switch (modifier) {
                case 'u': {
                    bytesString = StringUtils.replace(bytesString, "\\w", "[[:alpha:]]");
                } break;

                case 'd': {

                } break;

                case 'a': {
                    bytesString = StringUtils.replace(bytesString, "[[:alpha:]]", "[a-zA-Z]");
                } break;

                default:
                    throw new UnsupportedOperationException();
            }

            bytes = StringOperations.encodeRope(bytesString, ASCIIEncoding.INSTANCE);
        }

        return bytes;
    }

    @TruffleBoundary
    public static Regex compile(Node currentNode, RubyContext context, Rope bytes, RegexpOptions options) {
        bytes = shimModifiers(bytes);

        try {
            final ByteList byteList = RopeOperations.getByteListReadOnly(bytes);
            Encoding enc = bytes.getEncoding();
            Encoding[] fixedEnc = new Encoding[]{null};
            ByteList unescaped = ClassicRegexp.preprocess(context, byteList, enc, fixedEnc, RegexpSupport.ErrorMode.RAISE);
            if (fixedEnc[0] != null) {
                if ((fixedEnc[0] != enc && options.isFixed()) ||
                        (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                    throw new RaiseException(context.getCoreExceptions().regexpError("incompatible character encoding", null));
                }
                if (fixedEnc[0] != ASCIIEncoding.INSTANCE) {
                    options.setFixed(true);
                    enc = fixedEnc[0];
                }
            } else if (!options.isFixed()) {
                enc = USASCIIEncoding.INSTANCE;
            }

            if (fixedEnc[0] != null) options.setFixed(true);
            //if (regexpOptions.isEncodingNone()) setEncodingNone();

            Regex regexp = new Regex(unescaped.getUnsafeBytes(), unescaped.getBegin(), unescaped.getBegin() + unescaped.getRealSize(), options.toJoniOptions(), enc, Syntax.RUBY);
            regexp.setUserObject(RopeOperations.withEncodingVerySlow(bytes, enc));

            return regexp;
        } catch (ValueException e) {
            throw new RaiseException(context.getCoreExceptions().runtimeError("error compiling regex", currentNode));
        } catch (SyntaxException e) {
            throw new RaiseException(context.getCoreExceptions().regexpError(e.getMessage(), currentNode));
        }
    }

    public static Object getCachedNames(DynamicObject regexp) {
        return Layouts.REGEXP.getCachedNames(regexp);
    }

    public static void setCachedNames(DynamicObject regexp, Object cachedNames) {
        Layouts.REGEXP.setCachedNames(regexp, cachedNames);
    }

    public static void setRegex(DynamicObject regexp, Regex regex) {
        Layouts.REGEXP.setRegex(regexp, regex);
    }

    public static void setSource(DynamicObject regexp, Rope source) {
        Layouts.REGEXP.setSource(regexp, source);
    }

    public static void setOptions(DynamicObject regexp, RegexpOptions options) {
        Layouts.REGEXP.setOptions(regexp, options);
    }

    // TODO (nirvdrum 03-June-15) Unify with JRuby in RegexpSupport.
    public static Encoding checkEncoding(DynamicObject regexp, Rope str, boolean warn) {
        assert RubyGuards.isRubyRegexp(regexp);

        final Encoding strEnc = str.getEncoding();
        final Encoding regexEnc = Layouts.REGEXP.getRegex(regexp).getEncoding();

        /*
        if (str.scanForCodeRange() == StringSupport.CR_BROKEN) {
            throw getRuntime().newArgumentError("invalid byte sequence in " + str.getEncoding());
        }
        */
        //check();
        if (strEnc == regexEnc) {
            return regexEnc;
        } else if (regexEnc == USASCIIEncoding.INSTANCE && str.getCodeRange() == CodeRange.CR_7BIT) {
            return regexEnc;
        } else if (!strEnc.isAsciiCompatible()) {
            if (strEnc != regexEnc) {
                //encodingMatchError(getRuntime(), pattern, enc);
            }
        } else if (Layouts.REGEXP.getOptions(regexp).isFixed()) {
            /*
            if (enc != pattern.getEncoding() &&
                    (!pattern.getEncoding().isAsciiCompatible() ||
                            str.scanForCodeRange() != StringSupport.CR_7BIT)) {
                encodingMatchError(getRuntime(), pattern, enc);
            }
            */
            return regexEnc;
        }
        /*
        if (warn && this.options.isEncodingNone() && enc != ASCIIEncoding.INSTANCE && str.scanForCodeRange() != StringSupport.CR_7BIT) {
            getRuntime().getWarnings().warn(ID.REGEXP_MATCH_AGAINST_STRING, "regexp match /.../n against to " + enc + " string");
        }
        */
        return strEnc;
    }

    public static void initialize(RubyContext context, DynamicObject regexp, Node currentNode, Rope setSource, int options) {
        assert RubyGuards.isRubyRegexp(regexp);
        final RegexpOptions regexpOptions = RegexpOptions.fromEmbeddedOptions(options);
        final Regex regex = compile(currentNode, context, setSource, regexpOptions);

        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        setSource(regexp, (Rope) regex.getUserObject());
        setOptions(regexp, regexpOptions);
        setRegex(regexp, regex);
    }

    public static void initialize(DynamicObject regexp, Regex setRegex, Rope setSource) {
        assert RubyGuards.isRubyRegexp(regexp);
        setRegex(regexp, setRegex);
        setSource(regexp, setSource);
    }

    public static DynamicObject createRubyRegexp(RubyContext context, Node currentNode, DynamicObjectFactory factory, Rope source, RegexpOptions options) {
        final Regex regexp = RegexpNodes.compile(currentNode, context, source, options);

        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        return Layouts.REGEXP.createRegexp(factory, regexp, (Rope) regexp.getUserObject(), options, null);
    }

    @TruffleBoundary
    public static DynamicObject createRubyRegexp(DynamicObjectFactory factory, Regex regex, Rope source, RegexpOptions options) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(factory, null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.setOptions(regexp, options);
        RegexpNodes.initialize(regexp, regex, source);
        return regexp;
    }

    public static boolean isSuitableMatchDataType(RubyContext context, DynamicObject matchData) {
        return matchData == context.getCoreLibrary().getNilObject() || RubyGuards.isRubyMatchData(matchData);
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dupNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private RegexpSetLastMatchPrimitiveNode setLastMatchNode;
        @Child private CallDispatchHeadNode toSNode;
        @Child private ToStrNode toStrNode;

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dupNode = DispatchHeadNodeFactory.createMethodCall(context);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            setLastMatchNode = RegexpSetLastMatchPrimitiveNodeFactory.create(null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object matchString(VirtualFrame frame, DynamicObject regexp, DynamicObject string) {
            final DynamicObject dupedString = (DynamicObject) dupNode.call(frame, string, "dup");

            return matchWithStringCopy(regexp, dupedString);
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public Object matchSymbol(VirtualFrame frame, DynamicObject regexp, DynamicObject symbol) {
            if (toSNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return matchWithStringCopy(regexp, (DynamicObject) toSNode.call(frame, symbol, "to_s"));
        }

        @Specialization(guards = "isNil(nil)")
        public Object matchNil(DynamicObject regexp, Object nil) {
            return nil();
        }

        @Specialization(guards = { "!isRubyString(other)", "!isRubySymbol(other)", "!isNil(other)" })
        public Object matchGeneric(VirtualFrame frame, DynamicObject regexp, DynamicObject other) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            return matchWithStringCopy(regexp, toStrNode.executeToStr(frame, other));
        }

        // Creating a MatchData will store a copy of the source string. It's tempting to use a rope here, but a bit
        // inconvenient because we can't work with ropes directly in Ruby and some MatchData methods are nicely
        // implemented using the source string data. Likewise, we need to taint objects based on the source string's
        // taint state. We mustn't allow the source string's contents to change, however, so we must ensure that we have
        // a private copy of that string. Since the source string would otherwise be a reference to string held outside
        // the MatchData object, it would be possible for the source string to be modified externally.
        //
        // Ex. x = "abc"; x =~ /(.*)/; x.upcase!
        //
        // Without a private copy, the MatchData's source could be modified to be upcased when it should remain the
        // same as when the MatchData was created.
        private Object matchWithStringCopy(DynamicObject regexp, DynamicObject string) {
            final Matcher matcher = createMatcher(getContext(), regexp, string);
            final int range = StringOperations.rope(string).byteLength();

            final DynamicObject matchData = matchCommon(getContext(), this, makeSubstringNode, regexp, string, true, matcher, 0, range);
            setLastMatchNode.executeSetLastMatch(matchData);

            if (matchData != nil()) {
                return matcher.getBegin();
            }

            return nil();
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int hash(DynamicObject regexp) {
            int options = Layouts.REGEXP.getRegex(regexp).getOptions() & ~32 /* option n, NO_ENCODING in common/regexp.rb */;
            return options ^ Layouts.REGEXP.getSource(regexp).hashCode();
        }

    }

    @NonStandard
    @CoreMethod(names = "match_start", required = 2)
    public abstract static class MatchStartNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dupNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dupNode = DispatchHeadNodeFactory.createMethodCall(context);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object matchStart(VirtualFrame frame, DynamicObject regexp, DynamicObject string, int startPos) {
            final DynamicObject dupedString = (DynamicObject) dupNode.call(frame, string, "dup");
            final Object matchResult = matchCommon(getContext(), this, makeSubstringNode, regexp, dupedString, false, startPos);

            if (RubyGuards.isRubyMatchData(matchResult) && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).numRegs > 0
                && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).beg[0] == startPos) {
                return matchResult;
            }

            return nil();
        }
    }

    @CoreMethod(names = "last_match", onSingleton = true, optional = 1, lowerFixnum = 1)
    public abstract static class LastMatchNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object lastMatch(NotProvided index) {
            return getMatchData();
        }

        @Specialization
        public Object lastMatch(VirtualFrame frame, int index,
                                @Cached("create()") MatchDataNodes.GetIndexNode getIndexNode) {
            final Object matchData = getMatchData();

            if (matchData == nil()) {
                return nil();
            } else {
                return getIndexNode.executeGetIndex(frame, matchData, index, NotProvided.INSTANCE);
            }
        }

        @TruffleBoundary
        private Object getMatchData() {
            Frame frame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameAccess.READ_ONLY, true);
            ThreadLocalObject lastMatch = getMatchDataThreadLocal(getContext(), frame, false);
            if (lastMatch == null) {
                return nil();
            } else {
                return lastMatch.get();
            }
        }

    }

    @Primitive(name = "regexp_set_last_match", needsSelf = false)
    @ImportStatic(RegexpNodes.class)
    public static abstract class RegexpSetLastMatchPrimitiveNode extends PrimitiveArrayArgumentsNode {

        public static RegexpSetLastMatchPrimitiveNode create() {
            return RegexpSetLastMatchPrimitiveNodeFactory.create(null);
        }

        public abstract DynamicObject executeSetLastMatch(Object matchData);

        @TruffleBoundary
        @Specialization(guards = "isSuitableMatchDataType(getContext(), matchData)")
        public DynamicObject setLastMatchData(DynamicObject matchData) {
            Frame frame = getContext().getCallStack().getCallerFrameIgnoringSend().getFrame(FrameInstance.FrameAccess.READ_WRITE, true);
            ThreadLocalObject lastMatch = getMatchDataThreadLocal(getContext(), frame, true);
            lastMatch.set(matchData);
            return matchData;
        }

    }

    @Primitive(name = "regexp_set_block_last_match", needsSelf = false)
    @ImportStatic(RegexpNodes.class)
    public static abstract class RegexpSetBlockLastMatchPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = { "isRubyProc(block)", "isSuitableMatchDataType(getContext(), matchData)" })
        public Object setBlockLastMatch(DynamicObject block, DynamicObject matchData) {
            final Frame declarationFrame = Layouts.PROC.getDeclarationFrame(block);
            if (declarationFrame == null) { // Symbol#to_proc currently does not have a declaration frame
                return matchData;
            }

            ThreadLocalObject lastMatch = getMatchDataThreadLocal(getContext(), declarationFrame, true);
            lastMatch.set(matchData);
            return matchData;
        }

    }

    @TruffleBoundary
    private static ThreadLocalObject getMatchDataThreadLocal(RubyContext context, Frame topFrame, boolean add) {
        Frame frame = topFrame;
        FrameSlot slot = null;

        while (true) {
            slot = frame.getFrameDescriptor().findFrameSlot("$~");
            if (slot != null) {
                break;
            }

            final Frame nextFrame = RubyArguments.getDeclarationFrame(frame);
            if (nextFrame != null) {
                frame = nextFrame;
            } else {
                if (add) {
                    slot = frame.getFrameDescriptor().addFrameSlot("$~", FrameSlotKind.Object);
                    break;
                } else {
                    return null;
                }
            }
        }

        final Object previousMatchData = frame.getValue(slot);
        if (previousMatchData == frame.getFrameDescriptor().getDefaultValue()) { // Never written to
            if (add) {
                ThreadLocalObject threadLocalObject = new ThreadLocalObject(context);
                frame.setObject(slot, threadLocalObject);
                return threadLocalObject;
            } else {
                return null;
            }
        }
        return (ThreadLocalObject) previousMatchData;
    }

    @CoreMethod(names = { "quote", "escape" }, onSingleton = true, required = 1)
    public abstract static class QuoteNode extends CoreMethodArrayArgumentsNode {

        @Child ToStrNode toStrNode;

        abstract public DynamicObject executeQuote(VirtualFrame frame, Object raw);

        @TruffleBoundary
        @Specialization(guards = "isRubyString(raw)")
        public DynamicObject quoteString(DynamicObject raw) {
            final Rope rope = StringOperations.rope(raw);
            boolean isAsciiOnly = rope.getEncoding().isAsciiCompatible() && rope.getCodeRange() == CodeRange.CR_7BIT;
            return createString(ClassicRegexp.quote19(StringOperations.getByteListReadOnly(raw), isAsciiOnly));
        }

        @Specialization(guards = "isRubySymbol(raw)")
        public DynamicObject quoteSymbol(DynamicObject raw) {
            return quoteString(createString(StringOperations.encodeRope(Layouts.SYMBOL.getString(raw), UTF8Encoding.INSTANCE)));
        }

        @Fallback
        public DynamicObject quote(VirtualFrame frame, Object raw) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            return executeQuote(frame, toStrNode.executeToStr(frame, raw));
        }

    }

    @NonStandard
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode dupNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dupNode = DispatchHeadNodeFactory.createMethodCall(context);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object searchFrom(VirtualFrame frame, DynamicObject regexp, DynamicObject string, int startPos) {
            final DynamicObject dupedString = (DynamicObject) dupNode.call(frame, string, "dup");

            return matchCommon(getContext(), this, makeSubstringNode, regexp, dupedString, false, startPos);
        }
    }

    @CoreMethod(names = "source")
    public abstract static class SourceNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject source(DynamicObject regexp) {
            return createString(Layouts.REGEXP.getSource(regexp));
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject regexp) {
            final ClassicRegexp classicRegexp = ClassicRegexp.newRegexp(getContext(), RopeOperations.getByteListReadOnly(Layouts.REGEXP.getSource(regexp)), Layouts.REGEXP.getRegex(regexp).getOptions());
            return createString(classicRegexp.toByteList());
        }

    }

    @NonStandard
    @NodeChild(value = "self")
    public abstract static class RubiniusNamesNode extends RubyNode {

        @Child private CallDispatchHeadNode newLookupTableNode;
        @Child private CallDispatchHeadNode lookupTableWriteNode;

        @Specialization(guards = "!anyNames(regexp)")
        public DynamicObject rubiniusNamesNoCaptures(DynamicObject regexp) {
            return nil();
        }

        @Specialization(guards = "anyNames(regexp)")
        public Object rubiniusNames(VirtualFrame frame, DynamicObject regexp) {
            if (getCachedNames(regexp) != null) {
                return getCachedNames(regexp);
            }

            if (newLookupTableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                newLookupTableNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            if (lookupTableWriteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lookupTableWriteNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object namesLookupTable = newLookupTableNode.call(frame, coreLibrary().getLookupTableClass(), "new");

            for (final Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final DynamicObject name = getContext().getSymbolTable().getSymbol(getContext().getRopeTable().getRope(Arrays.copyOfRange(e.name, e.nameP, e.nameEnd), USASCIIEncoding.INSTANCE, CodeRange.CR_7BIT));

                final int[] backrefs = e.getBackRefs();
                final DynamicObject backrefsRubyArray = createArray(backrefs, backrefs.length);

                lookupTableWriteNode.call(frame, namesLookupTable, "[]=", name, backrefsRubyArray);
            }

            setCachedNames(regexp, namesLookupTable);

            return namesLookupTable;
        }

        public static boolean anyNames(DynamicObject regexp) {
            return Layouts.REGEXP.getRegex(regexp).numberOfNames() > 0;
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, null, RegexpOptions.NULL_OPTIONS, null);
        }

    }

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

        @Specialization(guards = { "isRegexpLiteral(regexp)", "isRubyString(pattern)" })
        public DynamicObject initializeRegexpLiteral(DynamicObject regexp, DynamicObject pattern, int options) {
            throw new RaiseException(coreExceptions().securityError("can't modify literal regexp", this));
        }

        @Specialization(guards = { "!isRegexpLiteral(regexp)", "isInitialized(regexp)", "isRubyString(pattern)" })
        public DynamicObject initializeAlreadyInitialized(DynamicObject regexp, DynamicObject pattern, int options) {
            throw new RaiseException(coreExceptions().typeError("already initialized regexp", this));
        }

        @Specialization(guards = { "!isRegexpLiteral(regexp)", "!isInitialized(regexp)", "isRubyString(pattern)" })
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

        @Specialization(guards = { "!isInitialized(regexp)", "isRubyString(string)" })
        public Object searchRegionNotInitialized(DynamicObject regexp, DynamicObject string, int start, int end, boolean forward) {
            throw new RaiseException(coreExceptions().typeError("uninitialized Regexp", this));
        }

        @Specialization(guards = { "isRubyString(string)", "!isValidEncoding(string)" })
        public Object searchRegionInvalidEncoding(DynamicObject regexp, DynamicObject string, int start, int end, boolean forward) {
            throw new RaiseException(coreExceptions().argumentError(formatError(string), this));
        }

        @TruffleBoundary
        private String formatError(DynamicObject string) {
            return StringUtils.format("invalid byte sequence in %s", Layouts.STRING.getRope(string).getEncoding());
        }

        @Specialization(guards = { "isInitialized(regexp)", "isRubyString(string)", "isValidEncoding(string)" })
        public Object searchRegion(VirtualFrame frame, DynamicObject regexp, DynamicObject string,
                int start, int end, boolean forward,
                @Cached("createX()") RopeNodes.MakeSubstringNode makeSubstringNode,
                @Cached("createMethodCall()") CallDispatchHeadNode dupNode) {
            final DynamicObject dupedString = (DynamicObject) dupNode.call(frame, string, "dup");
            final Matcher matcher = RegexpNodes.createMatcher(getContext(), regexp, dupedString);

            if (forward) {
                // Search forward through the string.
                return RegexpNodes.matchCommon(getContext(), this, makeSubstringNode, regexp, dupedString, false, matcher, start, end);
            } else {
                // Search backward through the string.
                return RegexpNodes.matchCommon(getContext(), this, makeSubstringNode, regexp, dupedString, false, matcher, end, start);
            }
        }

    }
}
