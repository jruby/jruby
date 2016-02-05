/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.RubyContext;

public class ReadInstanceVariableNode extends RubyNode {

    @Child private RubyNode receiver;
    @Child private ReadHeadObjectFieldNode readNode;

    private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();

    public ReadInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver) {
        super(context, sourceSection);
        this.receiver = receiver;
        readNode = ReadHeadObjectFieldNodeGen.create(getContext(), name, nil());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (objectProfile.profile(receiverObject instanceof DynamicObject)) {
            return readNode.execute((DynamicObject) receiverObject);
        } else {
            return nil();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            final DynamicObject receiverRubyObject = (DynamicObject) receiverObject;

            if (receiverRubyObject.getShape().hasProperty(readNode.getName())) {
                return create7BitString("instance-variable", UTF8Encoding.INSTANCE);
            } else {
                return nil();
            }
        } else {
            return false;
        }
    }

}
