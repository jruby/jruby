/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class ReadInstanceVariableNode extends RubyNode {

    private final String name;

    @Child private RubyNode receiver;
    @Child private ReadObjectFieldNode readNode;
    @Child private ReadObjectFieldNode readOrNullNode;

    private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();

    public ReadInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver) {
        super(context, sourceSection);
        this.name = name;
        this.receiver = receiver;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (objectProfile.profile(receiverObject instanceof DynamicObject)) {
            return getReadNode().execute((DynamicObject) receiverObject);
        } else {
            return nil();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (objectProfile.profile(receiverObject instanceof DynamicObject)) {
            if (getReadOrNullNode().execute((DynamicObject) receiverObject) == null) {
                return nil();
            } else {
                return coreStrings().INSTANCE_VARIABLE.createInstance();
            }
        } else {
            return false;
        }
    }

    private ReadObjectFieldNode getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreter();
            readNode = insert(ReadObjectFieldNodeGen.create(getContext(), name, nil()));
        }

        return readNode;
    }

    private ReadObjectFieldNode getReadOrNullNode() {
        if (readOrNullNode == null) {
            CompilerDirectives.transferToInterpreter();
            readOrNullNode = insert(ReadObjectFieldNodeGen.create(getContext(), name, null));
        }

        return readOrNullNode;
    }

}
