/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.loader.SourceLoader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class JRubyInterop {

    private RubyContext context;

    public JRubyInterop(RubyContext context) {
        this.context = context;
    }

    @CompilerDirectives.TruffleBoundary
    public IRubyObject toJRuby(Object object) {
        if (object == context.getCoreLibrary().getNilObject()) {
            return context.getJRubyRuntime().getNil();
        } else if (object instanceof Boolean) {
            return context.getJRubyRuntime().newBoolean((boolean) object);
        } else if (RubyGuards.isRubyString(object)) {
            return toJRubyString((DynamicObject) object);
        } else if (RubyGuards.isRubyEncoding(object)) {
            return context.getJRubyRuntime().getEncodingService().rubyEncodingFromObject(context.getJRubyRuntime().newString(Layouts.ENCODING.getName((DynamicObject) object)));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public org.jruby.RubyString toJRubyString(DynamicObject string) {
        assert RubyGuards.isRubyString(string);
        return context.getJRubyRuntime().newString(StringOperations.rope(string).toByteListCopy());
    }

    @CompilerDirectives.TruffleBoundary
    public Object toTruffle(IRubyObject object) {
        if (object instanceof org.jruby.RubyFixnum) {
            final long value = ((org.jruby.RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return value;
            }

            return (int) value;
        } else if (object instanceof org.jruby.RubyBignum) {
            final BigInteger value = ((org.jruby.RubyBignum) object).getBigIntegerValue();
            return Layouts.BIGNUM.createBignum(context.getCoreLibrary().getBignumFactory(), value);
        } else if (object instanceof org.jruby.RubyString) {
            return StringOperations.createString(context, ((org.jruby.RubyString) object).getByteList().dup());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public DynamicObject toTruffle(org.jruby.RubyException jrubyException, RubyNode currentNode) {
        switch (jrubyException.getMetaClass().getName()) {
            case "ArgumentError":
                return context.getCoreLibrary().argumentError(jrubyException.getMessage().toString(), currentNode);
            case "Encoding::CompatibilityError":
                return context.getCoreLibrary().encodingCompatibilityError(jrubyException.getMessage().toString(), currentNode);
            case "RegexpError":
                return context.getCoreLibrary().regexpError(jrubyException.getMessage().toString(), currentNode);
        }

        throw new UnsupportedOperationException();
    }

    public String[] getArgv() {
        final IRubyObject[] jrubyStrings = ((org.jruby.RubyArray) context.getJRubyRuntime().getObject().getConstant("ARGV")).toJavaArray();
        final String[] strings = new String[jrubyStrings.length];

        for (int n = 0; n < strings.length; n++) {
            strings[n] = jrubyStrings[n].toString();
        }

        return strings;
    }

    public String[] getOriginalLoadPath() {
        final List<String> loadPath = new ArrayList<>();

        for (IRubyObject path : ((org.jruby.RubyArray) context.getJRubyRuntime().getLoadService().getLoadPath()).toJavaArray()) {
            String pathString = path.toString();

            if (!(pathString.endsWith("lib/ruby/2.2/site_ruby")
                    || pathString.endsWith("lib/ruby/shared")
                    || pathString.endsWith("lib/ruby/stdlib"))) {

                if (pathString.startsWith("uri:classloader:")) {
                    pathString = SourceLoader.JRUBY_SCHEME + pathString.substring("uri:classloader:".length());
                }

                loadPath.add(pathString);
            }
        }

        return loadPath.toArray(new String[loadPath.size()]);
    }

}
