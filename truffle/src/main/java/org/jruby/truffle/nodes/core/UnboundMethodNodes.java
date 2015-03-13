/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyMethod;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.core.RubyUnboundMethod;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ArityNode(ArityNode prev) {
            super(prev);
        }

        @Specialization
        public int arity(RubyUnboundMethod method) {
            return method.getMethod().getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodNode {

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BindNode(BindNode prev) {
            super(prev);
        }

        @Specialization
        public RubyMethod bind(RubyUnboundMethod unboundMethod, Object object) {
            return new RubyMethod(getContext().getCoreLibrary().getMethodClass(), object, unboundMethod.getMethod());
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NameNode(NameNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol name(RubyUnboundMethod unboundMethod) {
            notDesignedForCompilation();

            return getContext().newSymbol(unboundMethod.getMethod().getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodNode {

        public OriginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OriginNode(OriginNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule origin(RubyUnboundMethod unboundMethod) {
            return unboundMethod.getOrigin();
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OwnerNode(OwnerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyModule owner(RubyUnboundMethod unboundMethod) {
            return unboundMethod.getMethod().getDeclaringModule();
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SourceLocationNode(SourceLocationNode prev) {
            super(prev);
        }

        @Specialization
        public Object sourceLocation(RubyUnboundMethod unboundMethod) {
            notDesignedForCompilation();

            SourceSection sourceSection = unboundMethod.getMethod().getSharedMethodInfo().getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                RubyString file = getContext().makeString(sourceSection.getSource().getName());
                return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

}
