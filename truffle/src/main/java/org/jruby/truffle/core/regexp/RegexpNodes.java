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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
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
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.rubinius.RegexpPrimitiveNodes.RegexpSetLastMatchPrimitiveNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.RegexpSupport;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

@CoreClass("Regexp")
public abstract class RegexpNodes {

    @TruffleBoundary
    public static Object matchCommon(RubyContext context, RopeNodes.MakeSubstringNode makeSubstringNode, DynamicObject regexp, DynamicObject source, boolean operator, boolean setNamedCaptures, int startPos) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(source);

        final Rope sourceRope = StringOperations.rope(source);

        final Rope regexpSourceRope = Layouts.REGEXP.getSource(regexp);
        final Encoding enc = checkEncoding(regexp, sourceRope, true);
        final ByteList preprocessed = RegexpSupport.preprocess(context.getJRubyRuntime(), RopeOperations.getByteListReadOnly(regexpSourceRope), enc, new Encoding[] { null }, RegexpSupport.ErrorMode.RAISE);

        final Regex r = new Regex(preprocessed.getUnsafeBytes(), preprocessed.getBegin(), preprocessed.getBegin() + preprocessed.getRealSize(), Layouts.REGEXP.getOptions(regexp).toJoniOptions(), checkEncoding(regexp, sourceRope, true));
        final Matcher matcher = r.matcher(sourceRope.getBytes(), 0, sourceRope.byteLength());
        int range = sourceRope.byteLength();

        return matchCommon(context, makeSubstringNode, regexp, source, operator, setNamedCaptures, matcher, startPos, range);
    }

    @TruffleBoundary
    public static Object matchCommon(RubyContext context, RopeNodes.MakeSubstringNode makeSubstringNode, DynamicObject regexp, DynamicObject source, boolean operator, boolean setNamedCaptures, Matcher matcher, int startPos, int range) {
        assert RubyGuards.isRubyRegexp(regexp);
        assert RubyGuards.isRubyString(source);

        final Rope sourceRope = StringOperations.rope(source);

        final int match = matcher.search(startPos, range, Option.DEFAULT);

        final DynamicObject nil = context.getCoreLibrary().getNilObject();

        if (match == -1) {
            RegexpSetLastMatchPrimitiveNode.setLastMatch(context, nil);

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

        final Region region = matcher.getEagerRegion();
        final Object[] values = new Object[region.numRegs];

        for (int n = 0; n < region.numRegs; n++) {
            final int start = region.beg[n];
            final int end = region.end[n];

            if (operator) {
                final Object groupString;

                if (start > -1 && end > -1) {
                    groupString = createSubstring(makeSubstringNode, source, start, end - start);
                } else {
                    groupString = nil;
                }

                values[n] = groupString;
            } else {
                if (start == -1 || end == -1) {
                    values[n] = nil;
                } else {
                    values[n] = createSubstring(makeSubstringNode, source, start, end - start);
                }
            }
        }

        final DynamicObject pre = createSubstring(makeSubstringNode, source, 0, region.beg[0]);
        final DynamicObject post = createSubstring(makeSubstringNode, source, region.end[0], sourceRope.byteLength() - region.end[0]);
        final DynamicObject global = createSubstring(makeSubstringNode, source, region.beg[0], region.end[0] - region.beg[0]);

        final DynamicObject matchObject = Layouts.MATCH_DATA.createMatchData(Layouts.CLASS.getInstanceFactory(context.getCoreLibrary().getMatchDataClass()),
                source, regexp, region, values, pre, post, global, null);

        if (operator) {
            if (values.length > 0) {
                int nonNil = values.length - 1;

                while (values[nonNil] == nil) {
                    nonNil--;
                }
            }
        }

        RegexpSetLastMatchPrimitiveNode.setLastMatch(context, matchObject);

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
                        value = createSubstring(makeSubstringNode, source, start, end - start);
                    } else {
                        value = nil;
                    }
                }

                setLocalVariable(frame, name, value);
            }
        }

        if (operator) {
            return matcher.getBegin();
        } else {
            return matchObject;
        }
    }

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
                    bytesString = bytesString.replace("\\w", "[[:alpha:]]");
                } break;

                case 'd': {

                } break;

                case 'a': {
                    bytesString = bytesString.replace("[[:alpha:]]", "[a-zA-Z]");
                } break;

                default:
                    throw new UnsupportedOperationException();
            }

            bytes = StringOperations.createRope(bytesString, ASCIIEncoding.INSTANCE);
        }

        return bytes;
    }

    @TruffleBoundary
    public static Regex compile(Node currentNode, RubyContext context, Rope bytes, RegexpOptions options) {
        bytes = shimModifiers(bytes);

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

            final ByteList byteList = RopeOperations.getByteListReadOnly(bytes);
            Encoding enc = bytes.getEncoding();
            Encoding[] fixedEnc = new Encoding[]{null};
            ByteList unescaped = RegexpSupport.preprocess(context.getJRubyRuntime(), byteList, enc, fixedEnc, RegexpSupport.ErrorMode.RAISE);
            if (fixedEnc[0] != null) {
                if ((fixedEnc[0] != enc && options.isFixed()) ||
                        (fixedEnc[0] != ASCIIEncoding.INSTANCE && options.isEncodingNone())) {
                    RegexpSupport.raiseRegexpError19(context.getJRubyRuntime(), byteList, enc, options, "incompatible character encoding");
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

            Regex ret = new Regex(unescaped.getUnsafeBytes(), unescaped.getBegin(), unescaped.getBegin() + unescaped.getRealSize(), options.toJoniOptions(), enc, Syntax.RUBY);
            ret.setUserObject(RopeOperations.withEncodingVerySlow(bytes, enc));

            return ret;
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

        final Regex pattern = Layouts.REGEXP.getRegex(regexp);

        /*
        if (str.scanForCodeRange() == StringSupport.CR_BROKEN) {
            throw getRuntime().newArgumentError("invalid byte sequence in " + str.getEncoding());
        }
        */
        //check();
        Encoding enc = str.getEncoding();
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

    public static DynamicObject createRubyRegexp(RubyContext context, Node currentNode, DynamicObject regexpClass, Rope source, RegexpOptions options) {
        final Regex regexp = RegexpNodes.compile(currentNode, context, source, options);

        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        return Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), regexp, (Rope) regexp.getUserObject(), options, null);
    }

    public static DynamicObject createRubyRegexp(DynamicObject regexpClass, Regex regex, Rope source, RegexpOptions options) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.setOptions(regexp, options);
        RegexpNodes.initialize(regexp, regex, source);
        return regexp;
    }

    public static DynamicObject createRubyRegexp(DynamicObject regexpClass, Regex regex, Rope source) {
        final DynamicObject regexp = Layouts.REGEXP.createRegexp(Layouts.CLASS.getInstanceFactory(regexpClass), null, null, RegexpOptions.NULL_OPTIONS, null);
        RegexpNodes.initialize(regexp, regex, source);
        return regexp;
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private CallDispatchHeadNode toSNode;
        @Child private ToStrNode toStrNode;

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object match(DynamicObject regexp, DynamicObject string) {
            return matchCommon(getContext(), makeSubstringNode, regexp, string, true, true, 0);
        }

        @Specialization(guards = "isRubySymbol(symbol)")
        public Object match(VirtualFrame frame, DynamicObject regexp, DynamicObject symbol) {
            if (toSNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toSNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return match(regexp, (DynamicObject) toSNode.call(frame, symbol, "to_s"));
        }

        @Specialization(guards = "isNil(nil)")
        public Object match(DynamicObject regexp, Object nil) {
            return nil();
        }

        @Specialization(guards = { "!isRubyString(other)", "!isRubySymbol(other)", "!isNil(other)" })
        public Object matchGeneric(VirtualFrame frame, DynamicObject regexp, DynamicObject other) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return match(regexp, toStrNode.executeToStr(frame, other));
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

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public MatchStartNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object matchStart(DynamicObject regexp, DynamicObject string, int startPos) {
            final Object matchResult = matchCommon(getContext(), makeSubstringNode, regexp, string, false, false, startPos);
            if (RubyGuards.isRubyMatchData(matchResult) && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).numRegs > 0
                && Layouts.MATCH_DATA.getRegion((DynamicObject) matchResult).beg[0] == startPos) {
                return matchResult;
            }
            return nil();
        }
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
            return createString(org.jruby.RubyRegexp.quote19(StringOperations.getByteListReadOnly(raw), isAsciiOnly));
        }

        @Specialization(guards = "isRubySymbol(raw)")
        public DynamicObject quoteSymbol(DynamicObject raw) {
            return quoteString(createString(StringOperations.encodeRope(Layouts.SYMBOL.getString(raw), UTF8Encoding.INSTANCE)));
        }

        @Fallback
        public DynamicObject quote(VirtualFrame frame, Object raw) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return executeQuote(frame, toStrNode.executeToStr(frame, raw));
        }

    }

    @NonStandard
    @CoreMethod(names = "search_from", required = 2)
    public abstract static class SearchFromNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public SearchFromNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object searchFrom(DynamicObject regexp, DynamicObject string, int startPos) {
            return matchCommon(getContext(), makeSubstringNode, regexp, string, false, false, startPos);
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
            return createString(((org.jruby.RubyString) org.jruby.RubyRegexp.newRegexp(getContext().getJRubyRuntime(), RopeOperations.getByteListReadOnly(Layouts.REGEXP.getSource(regexp)), Layouts.REGEXP.getRegex(regexp).getOptions()).to_s()).getByteList());
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
                final DynamicObject backrefsRubyArray = Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), backrefs, backrefs.length);

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
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, null, RegexpOptions.NULL_OPTIONS, null);
        }

    }
}
