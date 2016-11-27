/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.numeric.FixnumNodesFactory;
import org.jruby.truffle.Options;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.dispatch.RubyCallNodeParameters;
import org.jruby.truffle.language.methods.InternalMethod;

public class CoreMethods {

    private final InternalMethod fixnumPlus;
    private final InternalMethod fixnumMinus;
    private final InternalMethod fixnumMul;

    public CoreMethods(CoreLibrary coreLibrary) {
        fixnumPlus = getMethod(coreLibrary.getFixnumClass(), "+");
        fixnumMinus = getMethod(coreLibrary.getFixnumClass(), "-");
        fixnumMul = getMethod(coreLibrary.getFixnumClass(), "*");
    }

    private InternalMethod getMethod(DynamicObject module, String name) {
        InternalMethod method = Layouts.MODULE.getFields(module).getMethod(name);
        if (method == null) {
            throw new AssertionError();
        }
        return method;
    }

    public RubyNode createCallNode(RubyCallNodeParameters callParameters) {
        if (!Options.BASICOPS_INLINE || callParameters.getBlock() != null || callParameters.isSplatted() || callParameters.isSafeNavigation()) {
            return new RubyCallNode(callParameters);
        }

        int n = 1 /* self */ + callParameters.getArguments().length;

        if (n == 2) {
            switch (callParameters.getMethodName()) {
            case "+":
                return InlinedCoreMethodNode.inlineBuiltin(callParameters, fixnumPlus, FixnumNodesFactory.AddNodeFactory.getInstance());
            case "-":
                return InlinedCoreMethodNode.inlineBuiltin(callParameters, fixnumMinus, FixnumNodesFactory.SubNodeFactory.getInstance());
            case "*":
                return InlinedCoreMethodNode.inlineBuiltin(callParameters, fixnumMul, FixnumNodesFactory.MulNodeFactory.getInstance());
            default:
            }
        }

        return new RubyCallNode(callParameters);
    }


}
