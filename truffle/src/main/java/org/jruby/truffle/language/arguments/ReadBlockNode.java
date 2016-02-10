/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

/**
 * Read the block as a {@code Proc}.
 */
public class ReadBlockNode extends RubyNode {

    private final ConditionProfile hasBlockProfile = ConditionProfile.createBinaryProfile();
    private final Object valueIfAbsent;

    public ReadBlockNode(RubyContext context, SourceSection sourceSection, Object valueIfAbsent) {
        super(context, sourceSection);
        this.valueIfAbsent = valueIfAbsent;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject block = RubyArguments.getBlock(frame.getArguments());

        if (hasBlockProfile.profile(block != null)) {
            return block;
        } else {
            return valueIfAbsent;
        }
    }

}
