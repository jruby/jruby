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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

public class ReadBlockNode extends RubyNode {

    private final Object valueIfAbsent;

    private final ConditionProfile nullProfile = ConditionProfile.createBinaryProfile();

    public ReadBlockNode(Object valueIfAbsent) {
        this.valueIfAbsent = valueIfAbsent;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject block = RubyArguments.getBlock(frame);

        if (nullProfile.profile(block == null)) {
            return valueIfAbsent;
        } else {
            if (!Layouts.PROC.isProc(block)) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException("Method passed something that isn't a Proc as a block");
            }

            return block;
        }
    }

}
