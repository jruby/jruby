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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        boolean equal(RubyUnboundMethod self, RubyUnboundMethod other) {
            return self.getMethod() == other.getMethod() && self.getOrigin() == other.getOrigin();
        }

        @Specialization(guards = "!isRubyUnboundMethod(other)")
        boolean equal(RubyUnboundMethod self, Object other) {
            return false;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(RubyUnboundMethod method) {
            return method.getMethod().getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private RubyClass metaClass(VirtualFrame frame, Object object) {
            if (metaClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                metaClassNode = insert(MetaClassNodeGen.create(getContext(), getSourceSection(), null));
            }
            return metaClassNode.executeMetaClass(frame, object);
        }

        @Specialization
        public RubyMethod bind(VirtualFrame frame, RubyUnboundMethod unboundMethod, Object object) {
            notDesignedForCompilation();
            RubyModule module = unboundMethod.getMethod().getDeclaringModule();
            // the (redundant) instanceof is to satisfy FindBugs with the following cast
            if (module instanceof RubyClass && !ModuleOperations.canBindMethodTo(module, metaClass(frame, object))) {
                CompilerDirectives.transferToInterpreter();
                if (((RubyClass) module).isSingleton()) {
                    throw new RaiseException(getContext().getCoreLibrary().typeError("singleton method called for a different object", this));
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeError("bind argument must be an instance of " + module.getName(), this));
                }
            }

            return new RubyMethod(getContext().getCoreLibrary().getMethodClass(), object, unboundMethod.getMethod());
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol name(RubyUnboundMethod unboundMethod) {
            notDesignedForCompilation();

            return getContext().getSymbol(unboundMethod.getMethod().getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        public OriginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule origin(RubyUnboundMethod unboundMethod) {
            return unboundMethod.getOrigin();
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule owner(RubyUnboundMethod unboundMethod) {
            return unboundMethod.getMethod().getDeclaringModule();
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sourceLocation(RubyUnboundMethod unboundMethod) {
            notDesignedForCompilation();

            SourceSection sourceSection = unboundMethod.getMethod().getSharedMethodInfo().getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return nil();
            } else {
                RubyString file = getContext().makeString(sourceSection.getSource().getName());
                return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

}
