/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.methods;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.arguments.RubyArguments;

/**
 * Define a method from a method literal (def mymethod ... end).
 * That is, store the definition of a method and when executed
 * produce the executable object that results.
 */
public class MethodDefinitionNode extends RubyNode {

    private final String name;
    private final SharedMethodInfo sharedMethodInfo;
    private final CallTarget callTarget;

    @Child private GetDefaultDefineeNode getDefaultDefineeNode;

    public MethodDefinitionNode(String name, SharedMethodInfo sharedMethodInfo, CallTarget callTarget) {
        this.name = name;
        this.sharedMethodInfo = sharedMethodInfo;
        this.callTarget = callTarget;
    }

    public InternalMethod executeMethod(VirtualFrame frame) {
        final DynamicObject dummyModule = coreLibrary().getObjectClass();
        final Visibility dummyVisibility = Visibility.PUBLIC;

        final DynamicObject capturedDefaultDefinee;
        if (RubyArguments.getDeclarationContext(frame) == DeclarationContext.INSTANCE_EVAL) {
            if (getDefaultDefineeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDefaultDefineeNode = insert(new GetDefaultDefineeNode());
            }
            capturedDefaultDefinee = getDefaultDefineeNode.execute(frame);
        } else {
            capturedDefaultDefinee = null;
        }

        final LexicalScope lexicalScope = RubyArguments.getMethod(frame).getLexicalScope();
        return new InternalMethod(getContext(), sharedMethodInfo, lexicalScope, name, dummyModule, dummyVisibility, false, null, callTarget,
                        null, capturedDefaultDefinee);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeMethod(frame);
    }

}
