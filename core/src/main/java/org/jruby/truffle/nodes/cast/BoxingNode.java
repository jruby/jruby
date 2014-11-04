/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.RubyFloat;

import java.math.BigInteger;

@NodeChild("child")
public abstract class BoxingNode extends RubyNode {

    private final ConditionProfile booleanProfile = ConditionProfile.createCountingProfile();

    public BoxingNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public BoxingNode(BoxingNode prev) {
        super(prev);
    }

    public abstract RubyBasicObject executeRubyBasicObject(VirtualFrame frame);

    protected abstract RubyBasicObject executeRubyBasicObject(VirtualFrame frame, Object value);

    public RubyBasicObject box(Object value) {
        return executeRubyBasicObject(null, value);
    }

    @Specialization
    public RubyBasicObject box(RubyBasicObject object) {
        return object;
    }

    @Specialization
    public RubyBasicObject box(boolean value) {
        if (booleanProfile.profile(value)) {
            return getContext().getCoreLibrary().getTrueObject();
        } else {
            return getContext().getCoreLibrary().getFalseObject();
        }
    }

    @Specialization
    public RubyBasicObject box(int value) {
        return new RubyFixnum.IntegerFixnum(getContext().getCoreLibrary().getFixnumClass(), value);
    }

    @Specialization
    public RubyBasicObject box(long value) {
        return new RubyFixnum.LongFixnum(getContext().getCoreLibrary().getFixnumClass(), value);
    }

    @Specialization
    public RubyBasicObject box(double value) {
        return new RubyFloat(getContext().getCoreLibrary().getFloatClass(), value);
    }

    @Specialization
    public RubyBasicObject box(BigInteger value) {
        return new RubyBignum(getContext().getCoreLibrary().getFixnumClass(), value);
    }

}
