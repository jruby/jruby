/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.time;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.time.TimeNodes.TimeZoneParser;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;

import java.time.ZoneId;

public abstract class GetTimeZoneNode extends RubyNode {

    protected static final CyclicAssumption TZ_UNCHANGED = new CyclicAssumption("ENV['TZ'] is unmodified");

    public static void invalidateTZ() {
        TZ_UNCHANGED.invalidate();
    }

    @Child private SnippetNode snippetNode = new SnippetNode();

    public abstract TimeZoneAndName executeGetTimeZone(VirtualFrame frame);

    @Specialization(assumptions = "TZ_UNCHANGED.getAssumption()")
    public TimeZoneAndName getTimeZone(VirtualFrame frame,
            @Cached("getTimeZone(frame)") TimeZoneAndName zone) {
        return zone;
    }

    protected TimeZoneAndName getTimeZone(VirtualFrame frame) {
        Object tz = snippetNode.execute(frame, "ENV['TZ']");
        String tzString = "";
        if (RubyGuards.isRubyString(tz)) {
            tzString = StringOperations.getString((DynamicObject) tz);
        }

        // TODO CS 4-May-15 not sure how TZ ends up being nil
        if (tz == nil()) {
            return new TimeZoneAndName(ZoneId.systemDefault(), null);
        } else if (tzString.equalsIgnoreCase("localtime")) {
            // On Solaris, $TZ is "localtime", so get it from Java
            return new TimeZoneAndName(ZoneId.systemDefault(), null);
        } else if (RubyGuards.isRubyString(tz)) {
            return TimeZoneParser.parse(this, tzString);
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
