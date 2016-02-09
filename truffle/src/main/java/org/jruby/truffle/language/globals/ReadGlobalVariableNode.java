/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.ReadHeadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadHeadObjectFieldNodeGen;
import org.jruby.truffle.RubyContext;

public class ReadGlobalVariableNode extends RubyNode {

    private final DynamicObject globalVariablesObject;

    @Child private ReadHeadObjectFieldNode readNode;

    public ReadGlobalVariableNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.globalVariablesObject = context.getCoreLibrary().getGlobalVariablesObject();
        readNode = ReadHeadObjectFieldNodeGen.create(getContext(), name, nil());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readNode.execute(globalVariablesObject);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (readNode.getName().equals("$~") || readNode.getName().equals("$!") || readNode.execute(globalVariablesObject) != nil()) {
            return create7BitString("global-variable", UTF8Encoding.INSTANCE);
        } else {
            return nil();
        }
    }

}
