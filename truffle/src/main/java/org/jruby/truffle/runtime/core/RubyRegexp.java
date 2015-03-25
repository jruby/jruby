/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;

import com.oracle.truffle.api.nodes.Node;
import org.jcodings.Encoding;
import org.joni.*;
import org.joni.exception.SyntaxException;
import org.joni.exception.ValueException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the Ruby {@code Regexp} class.
 */
public class RubyRegexp extends RubyBasicObject {

    // TODO(CS): not sure these compilation finals are correct - are they needed anyway?
    @CompilationFinal private Regex regex;
    @CompilationFinal private ByteList source;
    @CompilationFinal private RegexpOptions options;


    public RubyRegexp(RubyClass regexpClass) {
        super(regexpClass);
    }


    public RubyRegexp(Node currentNode, RubyClass regexpClass, ByteList regex, RegexpOptions options) {
        this(regexpClass);
        this.options = options;
        initialize(compile(currentNode, getContext(), regex, options.toJoniOptions()), regex);
    }

    public RubyRegexp(Node currentNode, RubyClass regexpClass, ByteList regex, int options) {
        this(regexpClass);
        initialize(compile(currentNode, getContext(), regex, options), regex);
    }

    public RubyRegexp(RubyClass regexpClass, Regex regex, ByteList source, RegexpOptions options ) {
        this(regexpClass);
        this.options = options;
        initialize(regex, source);
    }

    public RubyRegexp(RubyClass regexpClass, Regex regex, ByteList source) {
        this(regexpClass);
        initialize(regex, source);
    }

    public void initialize(Node currentNode, ByteList setSource, int options) {
        regex = compile(currentNode, getContext(), setSource, options);
        source = setSource;
    }

    public void initialize(Regex setRegex, ByteList setSource) {
        regex = setRegex;
        source = setSource;
    }

    public Regex getRegex() {
        return regex;
    }

    public ByteList getSource() {
        return source;
    }

    public RegexpOptions getOptions() {
        return options;
    }

    @CompilerDirectives.TruffleBoundary
    public Object matchCommon(RubyString source, boolean operator, boolean setNamedCaptures) {
        return matchCommon(source, operator, setNamedCaptures, 0);
    }

    @CompilerDirectives.TruffleBoundary
    public Object matchCommon(RubyString source, boolean operator, boolean setNamedCaptures, int startPos) {
        final byte[] stringBytes = source.getByteList().bytes();
        final Matcher matcher = regex.matcher(stringBytes);
        int range = stringBytes.length;

        return matchCommon(source, operator, setNamedCaptures, matcher, startPos, range);
    }

    @CompilerDirectives.TruffleBoundary
    public Object matchCommon(RubyString source, boolean operator, boolean setNamedCaptures, Matcher matcher, int startPos, int range) {
        final ByteList bytes = source.getByteList();
        final RubyContext context = getContext();

        final Frame frame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

        final int match = matcher.search(startPos, range, Option.DEFAULT);

        final RubyNilClass nil = getContext().getCoreLibrary().getNilObject();

        if (match == -1) {
            setThread("$~", nil);

            if (setNamedCaptures && regex.numberOfNames() > 0) {
                for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext();) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                    setFrame(frame, name, getContext().getCoreLibrary().getNilObject());
                }
            }

            return getContext().getCoreLibrary().getNilObject();
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
                    groupString = getContext().getCoreLibrary().getNilObject();
                }

                values[n] = groupString;
            } else {
                if (start == -1 || end == -1) {
                    values[n] = getContext().getCoreLibrary().getNilObject();
                } else {
                    values[n] = makeString(source, start, end - start);
                }
            }
        }

        final RubyString pre = makeString(source, 0, region.beg[0]);
        final RubyString post = makeString(source, region.end[0], bytes.length() - region.end[0]);
        final RubyString global = makeString(source, region.beg[0], region.end[0] - region.beg[0]);

        final RubyMatchData matchObject = new RubyMatchData(context.getCoreLibrary().getMatchDataClass(), source, regex, region, values, pre, post, global);

        if (operator) {
            if (values.length > 0) {
                int nonNil = values.length - 1;

                while (values[nonNil] == getContext().getCoreLibrary().getNilObject()) {
                    nonNil--;
                }
            }
        }

        setThread("$~", matchObject);

        if (setNamedCaptures && regex.numberOfNames() > 0) {
            for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();
                int nth = regex.nameToBackrefNumber(e.name, e.nameP, e.nameEnd, region);

                final Object value;

                // Copied from jruby/RubyRegexp - see copyright notice there

                if (nth >= region.numRegs || (nth < 0 && (nth+=region.numRegs) <= 0)) {
                    value = getContext().getCoreLibrary().getNilObject();
                } else {
                    final int start = region.beg[nth];
                    final int end = region.end[nth];
                    if (start != -1) {
                        value = makeString(source, start, end - start);
                    } else {
                        value = getContext().getCoreLibrary().getNilObject();
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

    private RubyString makeString(RubyString source, int start, int length) {
        final RubyString ret = getContext().makeString(source.getLogicalClass(), source.getByteList().makeShared(start, length).dup());

        ret.getByteList().setEncoding(source.getByteList().getEncoding());
        ret.setCodeRange(source.getCodeRange());

        return ret;
    }

    private void setFrame(Frame frame, String name, Object value) {
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

    public void setThread(String name, Object value) {
        assert value != null;

        RubyNode.notDesignedForCompilation();
        getContext().getThreadManager().getCurrentThread().getThreadLocals().getOperations().setInstanceVariable(getContext().getThreadManager().getCurrentThread().getThreadLocals(), name, value);
    }

    @CompilerDirectives.TruffleBoundary
    public RubyString gsub(RubyString string, String replacement) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes().bytes();

        final Encoding encoding = string.getBytes().getEncoding();
        final Matcher matcher = regex.matcher(stringBytes);

        int p = string.getBytes().getBegin();
        int end = 0;
        int range = p + string.getBytes().getRealSize();
        int lastMatchEnd = 0;

        // We only ever care about the entire matched string, not each of the matched parts, so we can hard-code the index.
        int matchedStringIndex = 0;

        final StringBuilder builder = new StringBuilder();

        while (true) {
            Object matchData = matchCommon(string, false, false, matcher, p + end, range);

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
            end = StringSupport.positionEndForScan(string.getBytes(), matcher, encoding, p, range);
        }

        return context.makeString(builder.toString());
    }

    @CompilerDirectives.TruffleBoundary
    public RubyString sub(String string, String replacement) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(stringBytes);

        final int match = matcher.search(0, stringBytes.length, Option.DEFAULT);

        if (match == -1) {
            return context.makeString(string);
        } else {
            final StringBuilder builder = new StringBuilder();
            builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, 0, matcher.getBegin())));
            builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(replacement.getBytes(StandardCharsets.UTF_8))));
            builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, matcher.getEnd(), stringBytes.length - matcher.getEnd())));
            return context.makeString(builder.toString());
        }
    }

    @CompilerDirectives.TruffleBoundary
    public RubyString[] split(final RubyString string, final boolean useLimit, final int limit) {
        final RubyContext context = getContext();

        final ByteList bytes = string.getBytes();
        final byte[] byteArray = bytes.bytes();
        final int begin = bytes.getBegin();
        final int len = bytes.getRealSize();
        final int range = begin + len;
        final Encoding encoding = string.getBytes().getEncoding();
        final Matcher matcher = regex.matcher(byteArray);

        final ArrayList<RubyString> strings = new ArrayList<>();

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
                        strings.add(context.makeString(""));
                        break;

                    } else if (lastNull) {
                        final int substringLength = StringSupport.length(encoding, byteArray, begin + beg, range);
                        strings.add(context.makeString(bytes.makeShared(beg, substringLength).dup()));
                        beg = start - begin;

                    } else {
                        start += start == range ? 1 : StringSupport.length(encoding, byteArray, start, range);
                        lastNull = true;
                        continue;
                    }
                } else {
                    strings.add(context.makeString(bytes.makeShared(beg, end - beg).dup()));
                    beg = matcher.getEnd();
                    start = begin + beg;
                }
                lastNull = false;

                //if (captures) populateCapturesForSplit(runtime, result, matcher, true);
                if (useLimit && limit <= ++i) break;
            }

            if (len > 0 && (useLimit || len > beg || limit < 0)) {
                strings.add(context.makeString(bytes.makeShared(beg, len - beg).dup()));
            }
        }

        // Suppress trailing empty fields if not using a limit and the supplied limit isn't negative.
        if (!useLimit && limit == 0) {
            while (! strings.isEmpty() && (strings.get(strings.size() - 1).length() == 0)) {
                strings.remove(strings.size() - 1);
            }
        }

        return strings.toArray(new RubyString[strings.size()]);
    }

    @CompilerDirectives.TruffleBoundary
    public Object scan(RubyString string) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes().bytes();
        final Encoding encoding = string.getBytes().getEncoding();
        final Matcher matcher = regex.matcher(stringBytes);

        int p = string.getBytes().getBegin();
        int end = 0;
        int range = p + string.getBytes().getRealSize();

        Object lastGoodMatchData = getContext().getCoreLibrary().getNilObject();

        if (regex.numberOfCaptures() == 0) {
            final ArrayList<RubyString> strings = new ArrayList<>();

            while (true) {
                Object matchData = matchCommon(string, false, true, matcher, p + end, range);

                if (matchData == context.getCoreLibrary().getNilObject()) {
                    break;
                }

                RubyMatchData md = (RubyMatchData) matchData;
                Object[] values = md.getValues();

                assert values.length == 1;

                strings.add((RubyString) values[0]);

                lastGoodMatchData = matchData;
                end = StringSupport.positionEndForScan(string.getBytes(), matcher, encoding, p, range);
            }

            setThread("$~", lastGoodMatchData);
            return strings.toArray(new RubyString[strings.size()]);
        } else {
            final List<RubyArray> allMatches = new ArrayList<>();

            while (true) {
                Object matchData = matchCommon(string, false, true, matcher, p + end, stringBytes.length);

                if (matchData == context.getCoreLibrary().getNilObject()) {
                    break;
                }

                final Object[] captures = ((RubyMatchData) matchData).getCaptures();
                allMatches.add(new RubyArray(context.getCoreLibrary().getArrayClass(), captures, captures.length));

                lastGoodMatchData = matchData;
                end = StringSupport.positionEndForScan(string.getBytes(), matcher, encoding, p, range);
            }

            setThread("$~", lastGoodMatchData);
            return allMatches.toArray(new Object[allMatches.size()]);
        }
    }

    public static Regex compile(Node currentNode, RubyContext context, ByteList bytes, int options) {
        RubyNode.notDesignedForCompilation();
        return compile(currentNode, context, bytes.bytes(), bytes.getEncoding(), options);
    }

    @TruffleBoundary
    public static Regex compile(Node currentNode, RubyContext context, byte[] bytes, Encoding encoding, int options) {
        RubyNode.notDesignedForCompilation();

        try {
            return new Regex(bytes, 0, bytes.length, options, encoding, Syntax.RUBY);
        } catch (ValueException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().runtimeError("error compiling regex", currentNode));
        } catch (SyntaxException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().regexpError(e.getMessage(), currentNode));
        }
    }

    public static class RegexpAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyRegexp(context.getCoreLibrary().getRegexpClass());
        }

    }

}
