/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects.instancevariables;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.objects.*;

@NodeInfo(shortName = "@missing")
public class ReadMissingInstanceVariableNode extends ReadSpecializedInstanceVariableNode {

    public ReadMissingInstanceVariableNode(SourceSection sourceSection, String name, RubyNode receiver, ObjectLayout objectLayout, RubyContext context) {
        super(context, sourceSection, name, receiver, objectLayout);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyBasicObject receiverObject = (RubyBasicObject) receiver.execute(frame);

        if (!receiverObject.getObjectLayout().contains(objectLayout)) {
            CompilerDirectives.transferToInterpreter();
            replace(respecialize(receiverObject));
            return receiverObject.getInstanceVariable(name);
        }

        return NilPlaceholder.INSTANCE;
    }
}
