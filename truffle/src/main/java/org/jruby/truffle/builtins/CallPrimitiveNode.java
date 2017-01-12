/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;

public class CallPrimitiveNode extends RubyNode {

    @Child private RubyNode primitive;
    @Child private RubyNode fallback;

    private final ConditionProfile successProfile = ConditionProfile.createBinaryProfile();

    public CallPrimitiveNode(RubyNode primitive, RubyNode fallback) {
        this.primitive = primitive;
        this.fallback = fallback;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = primitive.execute(frame);

        // Primitives fail by returning null, allowing the method to continue (the fallback)
        if (successProfile.profile(value != null)) {
            return value;
        } else {
            return fallback.execute(frame);
        }

    }

}
