/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.bool;

import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.RubyNode;

@CoreClass("TrueClass")
public abstract class TrueClassNodes {

    @CoreMethod(names = "&", needsSelf = false, required = 1)
    public abstract static class AndNode extends UnaryCoreMethodNode {

        @CreateCast("operand")
        public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeGen.create(operand);
        }

        @Specialization
        public boolean and(boolean other) {
            return other;
        }
    }

    @CoreMethod(names = "|", needsSelf = false, required = 1)
    public abstract static class OrNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean or(Object other) {
            return true;
        }
    }

    @CoreMethod(names = "^", needsSelf = false, required = 1)
    public abstract static class XorNode extends UnaryCoreMethodNode {

        @CreateCast("operand")
        public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeGen.create(operand);
        }

        @Specialization
        public boolean xor(boolean other) {
            return !other;
        }
    }

}
