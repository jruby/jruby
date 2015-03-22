/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.core.RubyProc;

import java.util.Arrays;

public class ReturnEnumeratorIfNoBlockNode extends RubyNode {

    @Child private RubyNode method;
    @Child private CallDispatchHeadNode toEnumNode;
    private final String methodName;
    private final ConditionProfile noBlockProfile = ConditionProfile.createBinaryProfile();

    public ReturnEnumeratorIfNoBlockNode(String methodName, RubyNode method) {
        super(method.getContext(), method.getEncapsulatingSourceSection());
        this.method = method;
        this.methodName = methodName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyProc block = RubyArguments.getBlock(frame.getArguments());

        if (noBlockProfile.profile(block == null)) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, RubyArguments.getSelf(frame.getArguments()), "to_enum", null, RubyArguments.extractUserArgumentsWithUnshift(getContext().getSymbolTable().getSymbol(methodName), frame.getArguments()));

        } else {

            return method.execute(frame);

        }
    }

}