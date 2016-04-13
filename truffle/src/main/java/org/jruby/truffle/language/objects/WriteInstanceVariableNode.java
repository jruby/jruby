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
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class WriteInstanceVariableNode extends RubyNode {

    private final String name;

    @Child private RubyNode receiver;
    @Child private RubyNode rhs;
    @Child private WriteObjectFieldNode writeNode;

    private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();

    public WriteInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, RubyNode rhs) {
        super(context, sourceSection);
        this.name = name;
        this.receiver = receiver;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object object = receiver.execute(frame);
        final Object value = rhs.execute(frame);

        if (objectProfile.profile(object instanceof DynamicObject)) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(WriteObjectFieldNodeGen.create(getContext(), name));
            }

            writeNode.execute((DynamicObject) object, value);
        } else {
            throw new RaiseException(coreLibrary().frozenError(
                    Layouts.MODULE.getFields(coreLibrary().getLogicalClass(object)).getName(), this));
        }

        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

}
