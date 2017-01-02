/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.numeric.FixnumNodesFactory;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.dispatch.RubyCallNodeParameters;
import org.jruby.truffle.language.methods.InternalMethod;

public class CoreMethods {

    private final RubyContext context;

    private final InternalMethod fixnumPlus;
    private final InternalMethod fixnumMinus;
    private final InternalMethod fixnumMul;

    public CoreMethods(RubyContext context) {
        this.context = context;
        fixnumPlus = getMethod(context.getCoreLibrary().getFixnumClass(), "+");
        fixnumMinus = getMethod(context.getCoreLibrary().getFixnumClass(), "-");
        fixnumMul = getMethod(context.getCoreLibrary().getFixnumClass(), "*");
    }

    private InternalMethod getMethod(DynamicObject module, String name) {
        InternalMethod method = Layouts.MODULE.getFields(module).getMethod(name);
        if (method == null) {
            throw new AssertionError();
        }
        return method;
    }

    public RubyNode createCallNode(Source source, RubyCallNodeParameters callParameters) {
        if (!context.getOptions().BASICOPS_INLINE || callParameters.getBlock() != null || callParameters.isSplatted() || callParameters.isSafeNavigation()) {
            return new RubyCallNode(callParameters);
        }

        int n = 1 /* self */ + callParameters.getArguments().length;

        if (n == 2) {
            switch (callParameters.getMethodName()) {
            case "+":
                return InlinedCoreMethodNode.inlineBuiltin(context, source, callParameters, fixnumPlus, FixnumNodesFactory.AddNodeFactory.getInstance());
            case "-":
                return InlinedCoreMethodNode.inlineBuiltin(context, source, callParameters, fixnumMinus, FixnumNodesFactory.SubNodeFactory.getInstance());
            case "*":
                return InlinedCoreMethodNode.inlineBuiltin(context, source, callParameters, fixnumMul, FixnumNodesFactory.MulNodeFactory.getInstance());
            default:
            }
        }

        return new RubyCallNode(callParameters);
    }


}
