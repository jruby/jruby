/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyComparable.java
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
 */

package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

/**
 * This is a port of MRI's rb_cmpint, as taken from RubyComparable and broken out into specialized nodes.
 */

@NodeChildren({
    @NodeChild(value = "value"),
    @NodeChild(value = "receiver"),
    @NodeChild(value = "other")
})
public abstract class CmpIntNode extends RubyNode {

    @Child private CallDispatchHeadNode gtNode;
    @Child private CallDispatchHeadNode ltNode;

    public CmpIntNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract int executeCmpInt(VirtualFrame frame, Object cmpResult, RubyString a, Object b);

    @Specialization
    public int cmpInt(int value, Object receiver, Object other) {
        if (value > 0) {
            return 1;
        }

        if (value < 0) {
            return -1;
        }

        return 0;
    }

    @Specialization
    public int cmpLong(long value, Object receiver, Object other) {
        if (value > 0) {
            return 1;
        }

        if (value < 0) {
            return -1;
        }

        return 0;
    }

    @Specialization
    public int cmpBignum(RubyBignum value, Object receiver, Object other) {
        return value.bigIntegerValue().signum();
    }

    @Specialization
    public int cmpNil(RubyNilClass value, Object receiver, Object other) {
        throw new RaiseException(
            getContext().getCoreLibrary().argumentError(
                String.format("comparison of %s with %s failed",
                    getContext().getCoreLibrary().getLogicalClass(receiver).getName(),
                    getContext().getCoreLibrary().getLogicalClass(other).getName()), this)
        );
    }

    @Specialization(guards = {
            "!isInteger(value)",
            "!isLong(value)",
            "!isRubyBignum(value)",
            "!isRubyNilClass(value)" })
    public int cmpObject(VirtualFrame frame, Object value, Object receiver, Object other) {
        if (gtNode == null) {
            CompilerDirectives.transferToInterpreter();
            gtNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        if (gtNode.callBoolean(frame, value, ">", null, 0)) {
            return 1;
        }

        if (ltNode == null) {
            CompilerDirectives.transferToInterpreter();
            ltNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        if (ltNode.callBoolean(frame, value, "<", null, 0)) {
            return -1;
        }

        return 0;
    }
}
