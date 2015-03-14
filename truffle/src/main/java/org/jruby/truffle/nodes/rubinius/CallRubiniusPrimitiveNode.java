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
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.ReturnException;

/**
 * Node which wraps a {@link RubiniusPrimitiveNode}, providing the implicit control flow that you get with calls to
 * Rubinius primitives.
 */
public class CallRubiniusPrimitiveNode extends RubyNode {

    @Child private RubyNode primitive;
    private final long returnID;

    private final ConditionProfile primitiveSucceededCondition = ConditionProfile.createBinaryProfile();

    public CallRubiniusPrimitiveNode(RubyContext context, SourceSection sourceSection, RubyNode primitive, long returnID) {
        super(context, sourceSection);
        this.primitive = primitive;
        this.returnID = returnID;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        final Object value = primitive.execute(frame);

        if (primitiveSucceededCondition.profile(value != null)) {
            // If the primitive didn't fail its value is returned in the calling method

            throw new ReturnException(returnID, value);
        }

        // Primitives may return null to indicate that they have failed, in which case we continue with the fallback
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return nil();
    }

}
