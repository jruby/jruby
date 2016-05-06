/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class WriteConstantNode extends RubyNode {

    private final String name;

    @Child private RubyNode moduleNode;
    @Child private RubyNode valueNode;

    private final ConditionProfile moduleProfile = ConditionProfile.createBinaryProfile();

    public WriteConstantNode(String name, RubyNode moduleNode, RubyNode valueNode) {
        this.name = name;
        this.moduleNode = moduleNode;
        this.valueNode = valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = valueNode.execute(frame);
        final Object moduleObject = moduleNode.execute(frame);

        if (!moduleProfile.profile(RubyGuards.isRubyModule(moduleObject))) {
            throw new RaiseException(coreExceptions().typeErrorIsNotAClassModule(moduleObject, this));
        }

        Layouts.MODULE.getFields((DynamicObject) moduleObject).setConstant(getContext(), this, name, value);

        return value;
    }

}
