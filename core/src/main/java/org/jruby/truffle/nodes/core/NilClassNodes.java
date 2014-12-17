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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;

import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@CoreClass(name = "NilClass")
public abstract class NilClassNodes {

    @CoreMethod(names = "inspect", needsSelf = false)
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect() {
            return getContext().makeString("nil");
        }
    }

    @CoreMethod(names = "nil?", needsSelf = false)
    public abstract static class NilNode extends CoreMethodNode {

        public NilNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NilNode(NilNode prev) {
            super(prev);
        }

        @Specialization
        public boolean nil() {
            return true;
        }
    }

    @CoreMethod(names = "to_a", needsSelf = false)
    public abstract static class ToANode extends CoreMethodNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToANode(ToANode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray toA() {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }
    }

    @CoreMethod(names = "to_i", needsSelf = false)
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
        }

        @Specialization
        public int toI() {
            return 0;
        }
    }

    @CoreMethod(names = "to_f", needsSelf = false)
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF() {
            return 0.0f;
        }
    }

    @CoreMethod(names = "to_s", needsSelf = false)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS() {
            return getContext().makeString("");
        }
    }

    @CoreMethod(names = "to_h", needsSelf = false)
    public abstract static class ToHNode extends CoreMethodNode {

        public ToHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToHNode(ToHNode prev) {
            super(prev);
        }

        @Specialization
        public RubyHash toH() {
            return new RubyHash(getContext().getCoreLibrary().getHashClass(), null, getContext().getCoreLibrary().getNilObject(), null, 0, null);
        }
    }

    @CoreMethod(names = "dup", needsSelf = false)
    public abstract static class DupNode extends CoreMethodNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DupNode(DupNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString dup() {
            throw new RaiseException(getContext().getCoreLibrary().typeError("can't dup NilClass", this));
        }
    }

    @CoreMethod(names = "&", needsSelf = false, required = 1)
    public abstract static class AndNode extends CoreMethodNode {

        public AndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AndNode(AndNode prev) {
            super(prev);
        }

        @Specialization
        public boolean and(Object other) {
            return false;
        }
    }

    @CoreMethod(names = { "|", "^" }, needsSelf = false, required = 1)
    public abstract static class OrXorNode extends UnaryCoreMethodNode {

        public OrXorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OrXorNode(OrXorNode prev) {
            super(prev);
        }

        @CreateCast("operand") public RubyNode createCast(RubyNode operand) {
            return BooleanCastNodeFactory.create(getContext(), getSourceSection(), operand);
        }

        @Specialization
        public boolean orXor(boolean other) {
            return other;
        }
    }
}
