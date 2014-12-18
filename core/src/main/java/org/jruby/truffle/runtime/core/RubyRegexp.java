/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import org.jcodings.Encoding;
import org.joni.*;
import org.joni.exception.SyntaxException;
import org.joni.exception.ValueException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the Ruby {@code Regexp} class.
 */
public class RubyRegexp extends RubyBasicObject {

    /**
     * The class from which we create the object that is {@code Regexp}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyRegexp} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyRegexpClass extends RubyClass {

        public RubyRegexpClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Regexp");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyRegexp(getContext().getCoreLibrary().getRegexpClass());
        }

    }

    @CompilationFinal private Regex regex;
    @CompilationFinal private ByteList source;

    private RubyEncoding encoding;

    public RubyRegexp(RubyClass regexpClass) {
        super(regexpClass);
    }

    public RubyRegexp(RubyNode currentNode, RubyClass regexpClass, ByteList regex, int options) {
        this(regexpClass);
        initialize(compile(currentNode, getContext(), regex, options), regex);
    }

    public RubyRegexp(RubyClass regexpClass, Regex regex, ByteList source) {
        this(regexpClass);
        initialize(regex, source);
    }

    public void initialize(RubyNode currentNode, ByteList setSource) {
        regex = compile(currentNode, getContext(), setSource, Option.DEFAULT);
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

    @CompilerDirectives.SlowPath
    public Object matchCommon(ByteList bytes, boolean operator, boolean setNamedCaptures) {
        final byte[] stringBytes = bytes.bytes();
        final Matcher matcher = regex.matcher(stringBytes);
        int range = stringBytes.length;

        return matchCommon(bytes, operator, setNamedCaptures, matcher, 0, range);
    }

    @CompilerDirectives.SlowPath
    public Object matchCommon(ByteList bytes, boolean operator, boolean setNamedCaptures, Matcher matcher, int startPos, int range) {
        final RubyContext context = getContext();

        final Frame frame = Truffle.getRuntime().getCallerFrame().getFrame(FrameInstance.FrameAccess.READ_WRITE, false);

        final int match = matcher.search(startPos, range, Option.DEFAULT);

        final RubyNilClass nil = getContext().getCoreLibrary().getNilObject();

        if (operator) {
            for (int n = 1; n < 10; n++) {
                setThread("$" + n, nil);
            }
        }

        if (match == -1) {
            setFrame(frame, "$&", nil);
            setFrame(frame, "$`", nil);
            setFrame(frame, "$'", nil);

            if (operator) {
                setFrame(frame, "$+", nil);
            }

            setThread("$~", nil);

            if (setNamedCaptures && regex.numberOfNames() > 0) {
                for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext();) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP).intern();
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
                    groupString = new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(start, end - start).dup());
                } else {
                    groupString = getContext().getCoreLibrary().getNilObject();
                }

                values[n] = groupString;

                if (n > 0 && n < 10) {
                    setThread("$" + n, groupString);
                }
            } else {
                if (start == -1 || end == -1) {
                    values[n] = getContext().getCoreLibrary().getNilObject();
                } else {
                    final RubyString groupString = new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(start, end - start).dup());
                    values[n] = groupString;
                }
            }
        }

        final RubyMatchData matchObject =  new RubyMatchData(context.getCoreLibrary().getMatchDataClass(), values);

        if (operator) {
            if (values.length > 0) {
                int nonNil = values.length - 1;

                while (values[nonNil] == getContext().getCoreLibrary().getNilObject()) {
                    nonNil--;
                }

                setFrame(frame, "$+", values[nonNil]);
            } else {
                setFrame(frame, "$+", getContext().getCoreLibrary().getNilObject());
            }
        }

        setFrame(frame, "$`", new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(0, region.beg[0]).dup()));
        setFrame(frame, "$'", new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(region.end[0], bytes.length() - region.end[0]).dup()));
        setFrame(frame, "$&", new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(region.beg[0], region.end[0] - region.beg[0]).dup()));

        setThread("$~", matchObject);

        if (setNamedCaptures && regex.numberOfNames() > 0) {
            for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext();) {
                final NameEntry e = i.next();
                final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP).intern();
                int nth = regex.nameToBackrefNumber(e.name, e.nameP, e.nameEnd, region);

                final Object value;

                // Copied from jubry/RubyRegexp - see copyright notice there

                if (region == null) {
                    if (nth >= 1 || (nth < 0 && ++nth <= 0)) {
                        value = getContext().getCoreLibrary().getNilObject();
                    } else {
                        final int start = matcher.getBegin();
                        final int end = matcher.getEnd();
                        if (start != -1) {
                            value = new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(start, end - start).dup());
                        } else {
                            value = getContext().getCoreLibrary().getNilObject();
                        }
                    }
                } else {
                    if (nth >= region.numRegs || (nth < 0 && (nth+=region.numRegs) <= 0)) {
                        value = getContext().getCoreLibrary().getNilObject();
                    } else {
                        final int start = region.beg[nth];
                        final int end = region.end[nth];
                        if (start != -1) {
                            value = new RubyString(context.getCoreLibrary().getStringClass(), bytes.makeShared(start, end - start).dup());
                        } else {
                            value = getContext().getCoreLibrary().getNilObject();
                        }
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

    private void setThread(String name, Object value) {
        assert value != null;

        getContext().getThreadManager().getCurrentThread().getThreadLocals().setInstanceVariable(name, value);
    }

    @CompilerDirectives.SlowPath
    public RubyString gsub(String string, String replacement) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(stringBytes);

        final StringBuilder builder = new StringBuilder();

        int p = 0;

        while (true) {
            final int match = matcher.search(p, stringBytes.length, Option.DEFAULT);

            if (match == -1) {
                builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, p, stringBytes.length - p)));
                break;
            } else {
                builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, p, matcher.getBegin() - p)));
                builder.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(replacement.getBytes(StandardCharsets.UTF_8))));
            }

            p = matcher.getEnd();
        }

        return context.makeString(builder.toString());
    }

    @CompilerDirectives.SlowPath
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

    @CompilerDirectives.SlowPath
    public RubyString[] split(String string) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(stringBytes);

        final ArrayList<RubyString> strings = new ArrayList<>();

        int p = 0;

        while (true) {
            final int match = matcher.search(p, stringBytes.length, Option.DEFAULT);

            if (match == -1) {
                strings.add(context.makeString(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, p, stringBytes.length - p)).toString()));
                break;
            } else {
                strings.add(context.makeString(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, p, matcher.getBegin() - p)).toString()));
            }

            p = matcher.getEnd();
        }

        return strings.toArray(new RubyString[strings.size()]);
    }

    @CompilerDirectives.SlowPath
    public Object scan(RubyString string) {
        final RubyContext context = getContext();

        final byte[] stringBytes = string.getBytes().bytes();
        final Encoding encoding = string.getBytes().getEncoding();
        final Matcher matcher = regex.matcher(stringBytes);

        int p = string.getBytes().getBegin();
        int end = 0;
        int range = p + string.getBytes().getRealSize();

        if (regex.numberOfCaptures() == 0) {
            final ArrayList<RubyString> strings = new ArrayList<>();

            while (true) {
                Object matchData = matchCommon(string.getBytes(), false, true, matcher, p + end, range);

                if (matchData == context.getCoreLibrary().getNilObject()) {
                    break;
                }

                RubyMatchData md = (RubyMatchData) matchData;
                Object[] values = md.getValues();

                assert values.length == 1;

                strings.add((RubyString) values[0]);

                end = org.jruby.RubyString.positionEnd(string.getBytes(), matcher, encoding, p, range);
            }

            return strings.toArray(new RubyString[strings.size()]);
        } else {
            final List<RubyArray> strings = new ArrayList<>();

            while (true) {
                Object matchData = matchCommon(string.getBytes(), false, true, matcher, p + end, stringBytes.length);

                if (matchData == context.getCoreLibrary().getNilObject()) {
                    break;
                }

                RubyMatchData md = (RubyMatchData) matchData;

                final List<RubyString> parts = new ArrayList<>();

                Object[] values = md.getValues();
                for (int i = 1; i < values.length; i++) {
                    parts.add((RubyString) values[i]);
                }

                RubyString[] matches = parts.toArray(new RubyString[parts.size()]);
                strings.add(RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), matches));

                end = org.jruby.RubyString.positionEnd(string.getBytes(), matcher, encoding, p, range);
            }

            return strings.toArray(new Object[strings.size()]);
        }
    }

    @Override
    public int hashCode() {
        return regex.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RubyRegexp)) {
            return false;
        }
        RubyRegexp other = (RubyRegexp) obj;
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

    public static Regex compile(RubyNode currentNode, RubyContext context, ByteList bytes, int options) {
        RubyNode.notDesignedForCompilation();
        return compile(currentNode, context, bytes.bytes(), bytes.getEncoding(), options);
    }

    public static Regex compile(RubyNode currentNode, RubyContext context, byte[] bytes, Encoding encoding, int options) {
        RubyNode.notDesignedForCompilation();

        try {
            return new Regex(bytes, 0, bytes.length, options, encoding, Syntax.RUBY);
        } catch (ValueException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().runtimeError("error compiling regex", currentNode));
        } catch (SyntaxException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().regexpError(e.getMessage(), currentNode));
        }
    }

    public void forceEncoding(RubyEncoding encoding) {
        this.encoding = encoding;
    }

    public RubyEncoding getEncoding() {
        if (encoding == null) {
            encoding = RubyEncoding.getEncoding(getContext(), regex.getEncoding());
        }

        return encoding;
    }

}
