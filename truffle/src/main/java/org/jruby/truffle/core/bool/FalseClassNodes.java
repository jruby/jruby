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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.RubyNode;

@CoreClass(name = "FalseClass")
public abstract class FalseClassNodes {

    @CoreMethod(unsafeNeedsAudit = true, names = "&", needsSelf = false, required = 1)
    public abstract static class AndNode extends UnaryCoreMethodNode {

        public AndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean and(Object other) {
            return false;
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = { "|", "^" }, needsSelf = false, required = 1)
    public abstract static class OrXorNode extends UnaryCoreMethodNode {

        public OrXorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeGen.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean orXor(boolean other) {
            return other;
        }

    }

}
