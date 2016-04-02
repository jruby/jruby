/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.method;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.arguments.ArgumentDescriptorUtils;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNode;
import org.jruby.truffle.language.methods.CanBindMethodToModuleNodeGen;
import org.jruby.truffle.language.objects.MetaClassNode;
import org.jruby.truffle.language.objects.MetaClassNodeGen;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    @CoreMethod(unsafeNeedsAudit = true, names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyUnboundMethod(other)")
        boolean equal(DynamicObject self, DynamicObject other) {
            return Layouts.UNBOUND_METHOD.getMethod(self) == Layouts.UNBOUND_METHOD.getMethod(other) && Layouts.UNBOUND_METHOD.getOrigin(self) == Layouts.UNBOUND_METHOD.getOrigin(other);
        }

        @Specialization(guards = "!isRubyUnboundMethod(other)")
        boolean equal(DynamicObject self, Object other) {
            return false;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(DynamicObject method) {
            return Layouts.UNBOUND_METHOD.getMethod(method).getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private CanBindMethodToModuleNode canBindMethodToModuleNode;

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
            canBindMethodToModuleNode = CanBindMethodToModuleNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject bind(DynamicObject unboundMethod, Object object) {
            final DynamicObject objectMetaClass = metaClass(object);

            if (!canBindMethodToModuleNode.executeCanBindMethodToModule(Layouts.UNBOUND_METHOD.getMethod(unboundMethod), objectMetaClass)) {
                CompilerDirectives.transferToInterpreter();
                final DynamicObject declaringModule = Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getDeclaringModule();
                if (RubyGuards.isRubyClass(declaringModule) && Layouts.CLASS.getIsSingleton(declaringModule)) {
                    throw new RaiseException(coreLibrary().typeError(
                            "singleton method called for a different object", this));
                } else {
                    throw new RaiseException(coreLibrary().typeError(
                            "bind argument must be an instance of " + Layouts.MODULE.getFields(declaringModule).getName(), this));
                }
            }

            return Layouts.METHOD.createMethod(coreLibrary().getMethodFactory(), object, Layouts.UNBOUND_METHOD.getMethod(unboundMethod));
        }

        protected DynamicObject metaClass(Object object) {
            return metaClassNode.executeMetaClass(object);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject name(DynamicObject unboundMethod) {
            return getSymbol(Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(unsafeNeedsAudit = true, names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        public OriginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject origin(DynamicObject unboundMethod) {
            return Layouts.UNBOUND_METHOD.getOrigin(unboundMethod);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject owner(DynamicObject unboundMethod) {
            return Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getDeclaringModule();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject parameters(DynamicObject method) {
            final ArgumentDescriptor[] argsDesc = Layouts.UNBOUND_METHOD.getMethod(method).getSharedMethodInfo().getArgumentDescriptors();

            return ArgumentDescriptorUtils.argumentDescriptorsToParameters(getContext(), argsDesc, true);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object sourceLocation(DynamicObject unboundMethod) {
            SourceSection sourceSection = Layouts.UNBOUND_METHOD.getMethod(unboundMethod).getSharedMethodInfo().getSourceSection();

            if (sourceSection.getSource() == null) {
                return nil();
            } else {
                DynamicObject file = createString(StringOperations.encodeRope(sourceSection.getSource().getName(), UTF8Encoding.INSTANCE));
                Object[] objects = new Object[]{file, sourceSection.getStartLine()};
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
            }
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
