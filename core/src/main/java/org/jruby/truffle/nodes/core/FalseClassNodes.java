/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "FalseClass")
public abstract class FalseClassNodes {

    @CoreMethod(names = "&", needsSelf = false, required = 1)
    public abstract static class AndNode extends CoreMethodNode {

        public AndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AndNode(AndNode prev) {
            super(prev);
        }

        @Specialization
        public boolean and(boolean other) {
            return false;
        }

        @Specialization
        public boolean and(RubyObject other) {
            return false;
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
        public boolean or(boolean other) {
            return other;
        }

        @Specialization
        public boolean or(RubyNilClass other) {
            return false;
        }

        @Specialization(guards = "isNotNil")
        public boolean or(RubyObject other) {
            return true;
        }

        static boolean isNotNil(RubyObject o) {
            return ! (o instanceof RubyNilClass);
        }
    }

    @CoreMethod(names = "^", needsSelf = false, required = 1)
    public abstract static class XorNode extends CoreMethodNode {

        public XorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public XorNode(XorNode prev) {
            super(prev);
        }

        @Specialization
        public boolean xor(boolean other) {
            return false ^ other;
        }

        @Specialization
        public boolean xor(RubyNilClass other) {
            return false;
        }

        @Specialization(guards = "isNotNil")
        public boolean xor(RubyObject other) {
            return true;
        }

        static boolean isNotNil(RubyObject o) {
            return ! (o instanceof RubyNilClass);
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, needsSelf = false)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS() {
            return getContext().makeString("false");
        }

    }

}
