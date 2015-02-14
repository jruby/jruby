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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyTime;
import org.jruby.truffle.runtime.core.TimeOperations;

class JodaDateTimeToRubyTimeNode extends Node {

    private final RubyContext context;

    @Child private WriteHeadObjectFieldNode writeIsGMTNode = new WriteHeadObjectFieldNode("@is_gmt");
    @Child private WriteHeadObjectFieldNode writeOffsetNode = new WriteHeadObjectFieldNode("@offset");

    public JodaDateTimeToRubyTimeNode(RubyContext context, SourceSection sourceSection) {
        this.context = context;
    }

    public RubyTime toDateTime(VirtualFrame frame, DateTime dateTime) {
        final long miliseconds = dateTime.getMillis();
        final RubyTime time = new RubyTime(context.getCoreLibrary().getTimeClass(), TimeOperations.millisecondsToSeconds(miliseconds), TimeOperations.nanosecondsInCurrentSecond(miliseconds));
        writeIsGMTNode.execute(time, dateTime.getZone() == DateTimeZone.UTC);
        writeOffsetNode.execute(time, dateTime.getZone().getOffset(miliseconds));
        return time;
    }

}
