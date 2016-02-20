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
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class WriteInstanceVariableNode extends RubyNode {

    @Child private RubyNode receiver;
    @Child private RubyNode rhs;
    @Child private WriteObjectFieldNode writeNode;

    public WriteInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, RubyNode rhs) {
        super(context, sourceSection);
        this.receiver = receiver;
        this.rhs = rhs;
        writeNode = WriteObjectFieldNodeGen.create(getContext(), name);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object object = receiver.execute(frame);
        final Object value = rhs.execute(frame);

        if (object instanceof DynamicObject) {
            writeNode.execute((DynamicObject) object, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().frozenError(Layouts.MODULE.getFields(coreLibrary().getLogicalClass(object)).getName(), this));
        }

        return value;
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return create7BitString("assignment", UTF8Encoding.INSTANCE);
    }

}
