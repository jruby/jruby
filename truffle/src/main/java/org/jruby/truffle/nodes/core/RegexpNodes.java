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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.joni.*;
import org.joni.exception.SyntaxException;
import org.joni.exception.ValueException;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import static org.jruby.util.StringSupport.CR_7BIT;

@CoreClass(name = "Regexp")
public abstract class RegexpNodes {

    public static DynamicObject makeString(DynamicObject source, int start, int length) {
        assert RubyGuards.isRubyString(source);

        final ByteList bytes = new ByteList(Layouts.STRING.getByteList(source), start, length);
        final DynamicObject ret = StringNodes.createString(Layouts.BASIC_OBJECT.getLogicalClass(source), bytes);

        Layouts.STRING.setCodeRange(ret, Layouts.STRING.getCodeRange(source));

        return ret;
    }

    public static void setFrame(Frame frame, String name, Object value) {
        assert value != null;

        while (frame != null) {
            final FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(name);

            if (slot != null) {
                frame.setObject(slot, value);
                break;
            }

            frame = RubyArguments.getDeclarationFrame(frame.getArguments());
        }
    }

    @TruffleBoundary
    public static Object matchCommon(DynamicObject regexp, DynamicObject source, boolean operator, boolean setNamedCaptures) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(source);
        return matchCommon(regexp, source, operator, setNamedCaptures, 0);
    }

    @TruffleBoundary
    public static Object matchCommon(DynamicObject regexp, DynamicObject source, boolean operator, boolean setNamedCaptures, int startPos) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(source);

        final byte[] stringBytes = Layouts.STRING.getByteList(source).bytes();

        final ByteList bl = Layouts.REGEXP.getSource(regexp);
        final Encoding enc = checkEncoding(regexp, StringNodes.getCodeRangeable(source), true);
        final ByteList preprocessed = RegexpSupport.preprocess(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getRuntime(), bl, enc, new Encoding[]{null}, RegexpSupport.ErrorMode.RAISE);

        final Regex r = new Regex(preprocessed.getUnsafeBytes(), preprocessed.getBegin(), preprocessed.getBegin() + preprocessed.getRealSize(), Layouts.REGEXP.getOptions(regexp).toJoniOptions(), checkEncoding(regexp, StringNodes.getCodeRangeable(source), true));
        final Matcher matcher = r.matcher(stringBytes);
        int range = stringBytes.length;

        return matchCommon(regexp, source, operator, setNamedCaptures, matcher, startPos, range);
    }

    @TruffleBoundary
    public static Object matchCommon(DynamicObject regexp, DynamicObject source, boolean operator, boolean setNamedCaptures, Matcher matcher, int startPos, int range) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(source);

        final ByteList bytes = Layouts.STRING.getByteList(source);
        final RubyContext context = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext();

        final Frame frame = RubyCallStack.getCallerFrame(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext()).getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

        final int match = matcher.search(startPos, range, Option.DEFAULT);

        final DynamicObject nil = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();

        if (match == -1) {
            setThread(regexp, "$~", nil);

            if (setNamedCaptures && Layouts.REGEXP.getRegex(regexp).numberOfNames() > 0) {
                for (Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                    setFrame(frame, name, Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject());
                }
            }

            return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();
        }

        final Region region = matcher.getEagerRegion();
        final Object[] values = new Object[region.numRegs];

        for (int n = 0; n < region.numRegs; n++) {
            final int start = region.beg[n];
            final int end = region.end[n];

            if (operator) {
                final Object groupString;

                if (start > -1 && end > -1) {
                    groupString = makeString(source, start, end - start);
                } else {
                    groupString = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();
                }

                values[n] = groupString;
            } else {
                if (start == -1 || end == -1) {
                    values[n] = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();
                } else {
                    values[n] = makeString(source, start, end - start);
                }
            }
        }

        final DynamicObject pre = makeString(source, 0, region.beg[0]);
        final DynamicObject post = makeString(source, region.end[0], bytes.length() - region.end[0]);
        final DynamicObject global = makeString(source, region.beg[0], region.end[0] - region.beg[0]);

        final DynamicObject matchObject = Layouts.MATCH_DATA.createMatchData(Layouts.CLASS.getInstanceFactory(context.getCoreLibrary().getMatchDataClass()), source, regexp, region, values, pre, post, global, matcher.getBegin(), matcher.getEnd(), false, null, null);

        if (operator) {
            if (values.length > 0) {
                int nonNil = values.length - 1;

                while (values[nonNil] == Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject()) {
                    nonNil--;
                }
            }
        }

        setThread(regexp, "$~", matchObject);

        if (setNamedCaptures && Layouts.REGEXP.getRegex(regexp).numberOfNames() > 0) {
            for (Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                int nth = Layouts.REGEXP.getRegex(regexp).nameToBackrefNumber(e.name, e.nameP, e.nameEnd, region);

                final Object value;

                // Copied from jruby/RubyRegexp - see copyright notice there

                if (nth >= region.numRegs || (nth < 0 && (nth+=region.numRegs) <= 0)) {
                    value = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();
                } else {
                    final int start = region.beg[nth];
                    final int end = region.end[nth];
                    if (start != -1) {
                        value = makeString(source, start, end - start);
                    } else {
                        value = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getCoreLibrary().getNilObject();
                    }
                }

                setFrame(frame, name, value);
            }
        }

        if (operator) {
            return matcher.getBegin();
        } else {
            return matchObject;
        }
    }

    public static void setThread(DynamicObject regexp, String name, Object value) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert value != null;
        Layouts.THREAD.getThreadLocals(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext().getThreadManager().getCurrentThread()).define(name, value, 0);
    }

    @TruffleBoundary
    public static DynamicObject gsub(DynamicObject regexp, DynamicObject string, String replacement) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(string);

        final RubyContext context = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext();

        final byte[] stringBytes = Layouts.STRING.getByteList(string).bytes();

        final Encoding encoding = Layouts.STRING.getByteList(string).getEncoding();
        final Matcher matcher = Layouts.REGEXP.getRegex(regexp).matcher(stringBytes);

        int p = Layouts.STRING.getByteList(string).getBegin();
        int end = 0;
        int range = p + Layouts.STRING.getByteList(string).getRealSize();
        int lastMatchEnd = 0;

        // We only ever care about the entire matched string, not each of the matched parts, so we can hard-code the index.
        int matchedStringIndex = 0;

        final StringBuilder builder = new StringBuilder();

        while (true) {
            Object matchData = matchCommon(regexp, string, false, false, matcher, p + end, range);

            if (matchData == context.getCoreLibrary().getNilObject()) {
                builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, lastMatchEnd, range - lastMatchEnd)));

                break;
            }

            Region region = matcher.getEagerRegion();

            int regionStart = region.beg[matchedStringIndex];
            int regionEnd = region.end[matchedStringIndex];

            builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, lastMatchEnd, regionStart - lastMatchEnd)));
            builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(replacement.getBytes(StandardCharsets.UTF_8))));

            lastMatchEnd = regionEnd;
            end = StringSupport.positionEndForScan(Layouts.STRING.getByteList(string), matcher, encoding, p, range);
        }

        return StringNodes.createString(context.getCoreLibrary().getStringClass(), builder.toString());
    }

    @TruffleBoundary
    public static DynamicObject[] split(DynamicObject regexp, final DynamicObject string, final boolean useLimit, final int limit) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(string);

        final RubyContext context = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext();

        final ByteList bytes = Layouts.STRING.getByteList(string);
        final byte[] byteArray = bytes.bytes();
        final int begin = bytes.getBegin();
        final int len = bytes.getRealSize();
        final int range = begin + len;
        final Encoding encoding = Layouts.STRING.getByteList(string).getEncoding();
        final Matcher matcher = Layouts.REGEXP.getRegex(regexp).matcher(byteArray);

        final ArrayList<DynamicObject> strings = new ArrayList<>();

        int end, beg = 0;
        int i = 1;
        boolean lastNull = false;
        int start = begin;

        if (useLimit && limit == 1) {
            strings.add(string);

        } else {
            while ((end = matcher.search(start, range, Option.NONE)) >= 0) {
                if (start == end + begin && matcher.getBegin() == matcher.getEnd()) {
                    if (len == 0) {
                        strings.add(StringNodes.createString(context.getCoreLibrary().getStringClass(), ""));
                        break;

                    } else if (lastNull) {
                        final int substringLength = StringSupport.length(encoding, byteArray, begin + beg, range);
                        strings.add(StringNodes.createString(context.getCoreLibrary().getStringClass(), bytes.makeShared(beg, substringLength).dup()));
                        beg = start - begin;

                    } else {
                        start += start == range ? 1 : StringSupport.length(encoding, byteArray, start, range);
                        lastNull = true;
                        continue;
                    }
                } else {
                    strings.add(StringNodes.createString(context.getCoreLibrary().getStringClass(), bytes.makeShared(beg, end - beg).dup()));
                    beg = matcher.getEnd();
                    start = begin + beg;
                }
                lastNull = false;

                //if (captures) populateCapturesForSplit(runtime, result, matcher, true);
                if (useLimit && limit <= ++i) break;
            }

            if (len > 0 && (useLimit || len > beg || limit < 0)) {
                strings.add(StringNodes.createString(context.getCoreLibrary().getStringClass(), bytes.makeShared(beg, len - beg).dup()));
            }
        }

        // Suppress trailing empty fields if not using a limit and the supplied limit isn't negative.
        if (!useLimit && limit == 0) {
            while (! strings.isEmpty() && (StringNodes.length(strings.get(strings.size() - 1)) == 0)) {
                strings.remove(strings.size() - 1);
            }
        }

        return strings.toArray(new DynamicObject[strings.size()]);
    }

    @TruffleBoundary
    public static Regex compile(Node currentNode, RubyContext context, ByteList bytes, RegexpOptions options) {
        try {
            /*
                    // This isn't quite right - we shouldn't be looking up by name, we need a real reference to this constants
        if (node.getOptions().isEncodingNone()) {
            if (!all7Bit(node.getValue().bytes())) {
                regexp.getSource().setEncoding(ASCIIEncoding.INSTANCE);
            } else {
                regexp.getSource().setEncoding(USASCIIEncoding.INSTANCE);
            }
        } else if (node.getOptions().getKCode().getKCode().equals("SJIS")) {
            regexp.getSource().setEncoding(Windows_31JEncoding.INSTANCE);
        } else if (node.getOptions().getKCode().getKCode().equals("UTF8")) {
            regexp.getSource().setEncoding(UTF8Encoding.INSTANCE);
        }
             */

            Encoding enc = bytes.getEncoding();
            Encoding[] fixedEnc = new Encoding[]{null};
            ByteList unescaped = RegexpSupport.preprocess(context.getRuntime(), bytes, enc, fixedEnc, RegexpSupport.ErrorMode.RAISE);
            if (fixedEnc[0] != null) {
                if ((fixedEnc[0] != enc && options.isFixed()) ||
                        (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                    RegexpSupport.raiseRegexpError19(context.getRuntime(), bytes, enc, options, "incompatible character encoding");
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

            bytes.setEncoding(enc);

            Regex ret = new Regex(unescaped.getUnsafeBytes(), unescaped.getBegin(), unescaped.getBegin() + unescaped.getRealSize(), options.toJoniOptions(), enc, Syntax.RUBY);
            ret.setUserObject(bytes);

            return ret;
        } catch (ValueException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().runtimeError("error compiling regex", currentNode));
        } catch (SyntaxException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().regexpError(e.getMessage(), currentNode));
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

    public static void setSource(DynamicObject regexp, ByteList source) {
        Layouts.REGEXP.setSource(regexp, source);
    }

    public static void setOptions(DynamicObject regexp, RegexpOptions options) {
        Layouts.REGEXP.setOptions(regexp, options);
    }

    // TODO (nirvdrum 03-June-15) Unify with JRuby in RegexpSupport.
    public static Encoding checkEncoding(DynamicObject regexp, CodeRangeable str, boolean warn) {
        assert RubyGuards.isRubyRegexp(regexp);

        final Regex pattern = Layouts.REGEXP.getRegex(regexp);

        /*
        if (str.scanForCodeRange() == StringSupport.CR_BROKEN) {
            throw getRuntime().newArgumentError("invalid byte sequence in " + str.getEncoding());
        }
        */
        //check();
        Encoding enc = str.getByteList().getEncoding();
        if (!enc.isAsciiCompatible()) {
            if (enc != pattern.getEncoding()) {
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
            enc = pattern.getEncoding();
        }
        /*
        if (warn && this.options.isEncodingNone() && enc != ASCIIEncoding.INSTANCE && str.scanForCodeRange() != StringSupport.CR_7BIT) {
            getRuntime().getWarnings().warn(ID.REGEXP_MATCH_AGAINST_STRING, "regexp match /.../n against to " + enc + " string");
        }
        */
        return enc;
    }

    public static void initialize(DynamicObject regexp, Node currentNode, ByteList setSource, int options) {
        assert RubyGuards.isRubyRegexp(regexp);
        setSource(regexp, setSource);
        setOptions(regexp, RegexpOptions.fromEmbeddedOptions(options));
        setRegex(regexp, compile(currentNode, Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexp)).getContext(), setSource, Layouts.REGEXP.getOptions(regexp)));
    }

    public static void initialize(DynamicObject regexp, Regex setRegex, ByteList setSource) {
        assert RubyGuards.isRubyRegexp(regexp);
        setRegex(regexp, setRegex);
        setSource(regexp, setSource);
    }

    public static DynamicObject createRubyRegexp(Node currentNode, DynamicObject regexpClass, ByteList regex, RegexpOptions options) {
        return Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), RegexpNodes.compile(currentNode, Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexpClass)).getContext(), regex, options), regex, options, null);
    }

    public static DynamicObject createRubyRegexp(Node currentNode, DynamicObject regexpClass, ByteList regex, int options) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.setOptions(regexp, RegexpOptions.fromEmbeddedOptions(options));
        RegexpNodes.initialize(regexp, RegexpNodes.compile(currentNode, Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(regexpClass)).getContext(), regex, Layouts.REGEXP.getOptions(regexp)), regex);
        return regexp;
    }

    public static DynamicObject createRubyRegexp(DynamicObject regexpClass, Regex regex, ByteList source, RegexpOptions options) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.setOptions(regexp, options);
        RegexpNodes.initialize(regexp, regex, source);
        return regexp;
    }

    public static DynamicObject createRubyRegexp(DynamicObject regexpClass, Regex regex, ByteList source) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.initialize(regexp, regex, source);
        return regexp;
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toSNode;
        @Child private ToStrNode toStrNode;

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object match(DynamicObject regexp, DynamicObject string) {
            return matchCommon(regexp, string, true, true);
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public Object match(VirtualFrame frame, DynamicObject regexp, DynamicObject symbol) {
            if (toSNode == null) {
                CompilerDirectives.transferToInterpreter();
                toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return match(regexp, (DynamicObject) toSNode.call(frame, symbol, "to_s", null));
        }

        @Specialization(guards = "isNil(nil)")
        public Object match(DynamicObject regexp, Object nil) {
            return nil();
        }

        @Specialization(guards = { "!isRubyString(other)", "!isRubySymbol(other)", "!isNil(other)" })
        public Object matchGeneric(VirtualFrame frame, DynamicObject regexp, DynamicObject other) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return match(regexp, toStrNode.executeToStr(frame, other));
        }

    }

    @CoreMethod(names = "escape", onSingleton = true, required = 1)
    public abstract static class EscapeNode extends CoreMethodArrayArgumentsNode {

        public EscapeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(pattern)")
        public DynamicObject escape(DynamicObject pattern) {
            return createString(org.jruby.RubyRegexp.quote19(new ByteList(Layouts.STRING.getByteList(pattern)), true).toString());
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject regexp) {
            int options = Layouts.REGEXP.getRegex(regexp).getOptions() & ~32 /* option n, NO_ENCODING in common/regexp.rb */;
            return options ^ Layouts.REGEXP.getSource(regexp).hashCode();
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "match_start", required = 2)
    public abstract static class MatchStartNode extends CoreMethodArrayArgumentsNode {

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object matchStart(DynamicObject regexp, DynamicObject string, int startPos) {
            final Object matchResult = matchCommon(regexp, string, false, false, startPos);
            if (RubyGuards.isRubyMatchData(matchResult) && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).numRegs > 0
                && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).beg[0] == startPos) {
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
        public DynamicObject quoteString(DynamicObject raw) {
            boolean isAsciiOnly = Layouts.STRING.getByteList(raw).getEncoding().isAsciiCompatible() && StringNodes.scanForCodeRange(raw) == CR_7BIT;
            return createString(org.jruby.RubyRegexp.quote19(Layouts.STRING.getByteList(raw), isAsciiOnly));
        }

        @Specialization(guards = "isRubySymbol(raw)")
        public DynamicObject quoteSymbol(DynamicObject raw) {
            return quoteString(StringNodes.createString(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(raw)).getContext().getCoreLibrary().getStringClass(), Layouts.SYMBOL.getString(raw)));
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodArrayArgumentsNode {

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object searchFrom(DynamicObject regexp, DynamicObject string, int startPos) {
            return matchCommon(regexp, string, false, false, startPos);
        }
    }

    @CoreMethod(names = "source")
    public abstract static class SourceNode extends CoreMethodArrayArgumentsNode {

        public SourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject source(DynamicObject regexp) {
            return createString(Layouts.REGEXP.getSource(regexp).dup());
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject regexp) {
            return createString(((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getRuntime(), Layouts.REGEXP.getSource(regexp), Layouts.REGEXP.getRegex(regexp).getOptions()).to_s()).getByteList());
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
        public DynamicObject rubiniusNamesNoCaptures(DynamicObject regexp) {
            return nil();
        }

        @Specialization(guards = "anyNames(regexp)")
        public Object rubiniusNames(VirtualFrame frame, DynamicObject regexp) {
            if (getCachedNames(regexp) != null) {
                return getCachedNames(regexp);
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

            for (final Iterator<NameEntry> i = Layouts.REGEXP.getRegex(regexp).namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final DynamicObject name = getSymbol(new ByteList(e.name, e.nameP, e.nameEnd - e.nameP, false));

                final int[] backrefs = e.getBackRefs();
                final DynamicObject backrefsRubyArray = createArray(backrefs, backrefs.length);

                lookupTableWriteNode.call(frame, namesLookupTable, "[]=", null, name, backrefsRubyArray);
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

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(rubyClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        }

    }
}
