/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.*;
import org.joni.exception.ValueException;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Represents the Ruby {@code Regexp} class.
 */
public class RubyRegexp extends RubyObject {

    /**
     * The class from which we create the object that is {@code Regexp}. A subclass of
     * {@link RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyRegexp} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyRegexpClass extends RubyClass {

        public RubyRegexpClass(RubyClass objectClass) {
            super(null, objectClass, "Regexp");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyRegexp(getContext().getCoreLibrary().getRegexpClass());
        }

    }

    @CompilationFinal private Regex regex;
    @CompilationFinal private Object source;

    public RubyRegexp(RubyClass regexpClass) {
        super(regexpClass);
    }

    public RubyRegexp(RubyClass regexpClass, String regex, int options) {
        this(regexpClass);
        initialize(compile(getRubyClass().getContext(), regex, options), regex);
    }

    public RubyRegexp(RubyClass regexpClass, Regex regex, String source) {
        this(regexpClass);
        initialize(regex, source);
    }

    public void initialize(String setRegex) {
        regex = compile(getRubyClass().getContext(), setRegex, Option.DEFAULT);
        source = setRegex;
    }

    public void initialize(Regex setRegex, String setSource) {
        regex = setRegex;
        source = setSource;
    }

    public Regex getRegex() {
        return regex;
    }

    @CompilerDirectives.SlowPath
    public Object matchOperator(Frame frame, String string) {
        final RubyContext context = getRubyClass().getContext();

        final byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(stringBytes);
        final int match = matcher.search(0, stringBytes.length, Option.DEFAULT);

        if (match != -1) {
            final Region region = matcher.getEagerRegion();

            for (int n = 1; n < region.numRegs + 1; n++) {
                final FrameSlot slot = frame.getFrameDescriptor().findFrameSlot("$" + n);

                if (slot != null) {
                    final int start = region.beg[n];
                    final int end = region.end[n];
                    final RubyString groupString = context.makeString(string.substring(start, end));
                    frame.setObject(slot, groupString);
                }
            }

            return matcher.getBegin();
        } else {
            return NilPlaceholder.INSTANCE;
        }
    }

    @CompilerDirectives.SlowPath
    public Object match(String string) {
        final RubyContext context = getRubyClass().getContext();

        final byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        final Matcher matcher = regex.matcher(stringBytes);
        final int match = matcher.search(0, stringBytes.length, Option.DEFAULT);

        if (match != -1) {
            final Region region = matcher.getEagerRegion();

            final Object[] values = new Object[region.numRegs];

            for (int n = 0; n < region.numRegs; n++) {
                final int start = region.beg[n];
                final int end = region.end[n];

                if (start == -1 || end == -1) {
                    values[n] = NilPlaceholder.INSTANCE;
                } else {
                    final RubyString groupString = context.makeString(string.substring(start, end));
                    values[n] = groupString;
                }
            }

            return new RubyMatchData(context.getCoreLibrary().getMatchDataClass(), values);
        } else {
            return NilPlaceholder.INSTANCE;
        }
    }

    @CompilerDirectives.SlowPath
    public RubyString gsub(String string, String replacement) {
        final RubyContext context = getRubyClass().getContext();

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
    public RubyString[] split(String string) {
        final RubyContext context = getRubyClass().getContext();

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
    public RubyString[] scan(RubyString string) {
        final RubyContext context = getRubyClass().getContext();

        final byte[] stringBytes = string.getBytes().bytes();
        final Matcher matcher = regex.matcher(stringBytes);

        final ArrayList<RubyString> strings = new ArrayList<>();

        int p = 0;

        while (true) {
            final int match = matcher.search(p, stringBytes.length, Option.DEFAULT);

            if (match == -1) {
                break;
            } else {
                strings.add(context.makeString(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringBytes, matcher.getBegin(), matcher.getEnd() - matcher.getBegin())).toString()));
            }

            p = matcher.getEnd();
        }

        return strings.toArray(new RubyString[strings.size()]);
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

    public static Regex compile(RubyContext context, String pattern, int options) {
        final byte[] bytes = pattern.getBytes(StandardCharsets.UTF_8);
        return compile(context, bytes, UTF8Encoding.INSTANCE, options);
    }

    public static Regex compile(RubyContext context, byte[] bytes, Encoding encoding, int options) {
        try {
            return new Regex(bytes, 0, bytes.length, options, encoding, Syntax.RUBY);
        } catch (ValueException e) {
            throw new org.jruby.truffle.runtime.control.RaiseException(context.getCoreLibrary().runtimeError("error compiling regex"));
        }
    }

}
