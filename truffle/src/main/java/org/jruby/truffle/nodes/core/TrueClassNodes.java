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

import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "TrueClass")
public abstract class TrueClassNodes {

    @CoreMethod(names = "&", needsSelf = false, required = 1)
    public abstract static class AndNode extends UnaryCoreMethodNode {

        public AndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AndNode(AndNode prev) {
            super(prev);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeFactory.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean and(boolean other) {
            return other;
        }
    }

    @CoreMethod(names = "|", needsSelf = false, required = 1)
    public abstract static class OrNode extends CoreMethodNode {

        public OrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OrNode(OrNode prev) {
            super(prev);
        }

        @Specialization
        public boolean or(Object other) {
            return true;
        }
    }

    @CoreMethod(names = "^", needsSelf = false, required = 1)
    public abstract static class XorNode extends UnaryCoreMethodNode {

        public XorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public XorNode(XorNode prev) {
            super(prev);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeFactory.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean xor(boolean other) {
            return !other;
        }
    }

}
