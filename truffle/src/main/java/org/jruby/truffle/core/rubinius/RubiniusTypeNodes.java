/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;

@CoreClass("Rubinius::Type")
public abstract class RubiniusTypeNodes {

    @CoreMethod(names = "each_ancestor", onSingleton = true, required = 1, needsBlock = true)
    public abstract static class EachAncestorNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isRubyModule(module)")
        public DynamicObject eachAncestor(VirtualFrame frame, DynamicObject module, DynamicObject block) {
            for (DynamicObject ancestor : Layouts.MODULE.getFields(module).ancestors()) {
                yield(frame, block, ancestor);
            }
            return nil();
        }

    }

}
