/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class ReturnEnumeratorIfNoBlockNode extends RubyNode {

    private final String methodName;
    @Child private RubyNode method;
    @Child private CallDispatchHeadNode toEnumNode;
    @CompilationFinal private DynamicObject methodSymbol;
    private final ConditionProfile noBlockProfile = ConditionProfile.createBinaryProfile();

    public ReturnEnumeratorIfNoBlockNode(String methodName, RubyNode method) {
        this.methodName = methodName;
        this.method = method;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject block = RubyArguments.getBlock(frame);

        if (noBlockProfile.profile(block == null)) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            if (methodSymbol == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                methodSymbol = getSymbol(methodName);
            }

            final Object[] arguments = ArrayUtils.unshift(RubyArguments.getArguments(frame), methodSymbol);
            return toEnumNode.call(frame, RubyArguments.getSelf(frame), "to_enum", arguments);
        } else {
            return method.execute(frame);
        }
    }

}