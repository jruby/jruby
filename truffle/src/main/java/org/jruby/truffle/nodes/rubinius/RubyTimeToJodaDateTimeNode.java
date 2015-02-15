/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyTime;
import org.jruby.truffle.runtime.core.TimeOperations;

/**
 * Supports {@link TimePrimitiveNodes} by converting a {@link RubyTime} to a {@link DateTime}. We use a node because
 * doing this requires accessing instance variables, which we want to use an inline cache for.
 */
class RubyTimeToJodaDateTimeNode extends Node {

    private final RubyContext context;

    @Child private ReadHeadObjectFieldNode readIsGMTNode = new ReadHeadObjectFieldNode("@is_gmt");
    @Child private ReadHeadObjectFieldNode readOffsetNode = new ReadHeadObjectFieldNode("@offset");

    public RubyTimeToJodaDateTimeNode(RubyContext context, SourceSection sourceSection) {
        this.context = context;
    }

    public DateTime toDateTime(VirtualFrame frame, RubyTime time) {
        final Object isGMTObject = readIsGMTNode.execute(time);

        // The @is_gmt instance variable is only for internal use so we don't need a full cast here

        final boolean isGMT;

        if (isGMTObject instanceof Boolean && ((boolean) isGMTObject)) {
            isGMT = true;
        } else {
            isGMT = false;
        }

        return toDateTime(time.getZone(), time.getSeconds(),
                time.getNanoseconds(),
                isGMT,
                readOffsetNode.execute(time));
    }

    @CompilerDirectives.TruffleBoundary
    private DateTime toDateTime(DateTimeZone zone, long seconds, long nanoseconds, boolean isGMT, Object offset) {
        long time = TimeOperations.secondsAndNanosecondsToMiliseconds(seconds, nanoseconds);

        final DateTimeZone dateTimeZone;

        if (isGMT || offset == null || offset == context.getCoreLibrary().getNilObject()) {
            dateTimeZone = zone;
        } else if (offset instanceof Integer) {
            final int intOffset = (int) offset;

            // TODO CS 14-Feb-15 why is this negative? Rbx has some comments about having to reserve it
            dateTimeZone = zone;//DateTimeZone.forOffsetMillis(-intOffset);

            // TODO CS 14-Feb-15 we seem to need to actually apply the offset here as well? And twice?
            //time -= dateTimeZone.getOffset(time) * 2;
        } else {
            throw new UnsupportedOperationException(offset.getClass().getName());
        }

        return new DateTime(time, dateTimeZone);
    }

}
