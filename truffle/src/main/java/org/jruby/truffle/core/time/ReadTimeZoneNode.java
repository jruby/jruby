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
import org.jcodings.specific.UTF8Encoding;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;

public abstract class ReadTimeZoneNode extends RubyNode {
    
    protected static final CyclicAssumption TZ_UNCHANGED = new CyclicAssumption("ENV['TZ'] is unmodified");

    public static void invalidateTZ() {
        TZ_UNCHANGED.invalidate();
    }

    private static final Rope DEFAULT_ZONE = StringOperations.encodeRope(DateTimeZone.getDefault().toString(), UTF8Encoding.INSTANCE);

    @Child SnippetNode snippetNode = new SnippetNode();

    @Specialization(assumptions = "TZ_UNCHANGED.getAssumption()")
    public DynamicObject getTZ(VirtualFrame frame,
            @Cached("getTZValue(frame)") DynamicObject tzValue) {
        return tzValue;
    }

    protected DynamicObject getTZValue(VirtualFrame frame) {
        Object tz = snippetNode.execute(frame, "ENV['TZ']");

        // TODO CS 4-May-15 not sure how TZ ends up being nil

        if (tz == nil()) {
            return createString(DEFAULT_ZONE);
        } else if (RubyGuards.isRubyString(tz)) {
            return (DynamicObject) tz;
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
