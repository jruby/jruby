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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgsNode;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.methods.CanBindMethodToModuleNode;
import org.jruby.truffle.nodes.methods.CanBindMethodToModuleNodeGen;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.InternalMethod;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    @org.jruby.truffle.om.dsl.api.Layout
    public interface UnboundMethodLayout {

        DynamicObject createUnboundMethod(RubyBasicObject origin, InternalMethod method);

        boolean isUnboundMethod(DynamicObject object);

        RubyBasicObject getOrigin(DynamicObject object);
        InternalMethod getMethod(DynamicObject object);

    }

    public static final UnboundMethodLayout UNBOUND_METHOD_LAYOUT = UnboundMethodLayoutImpl.INSTANCE;

    public static RubyBasicObject createUnboundMethod(RubyBasicObject rubyClass, RubyBasicObject origin, InternalMethod method) {
        return BasicObjectNodes.createRubyBasicObject(rubyClass, UNBOUND_METHOD_LAYOUT.createUnboundMethod(origin, method));
    }

    public static RubyBasicObject getOrigin(RubyBasicObject method) {
        return UNBOUND_METHOD_LAYOUT.getOrigin(BasicObjectNodes.getDynamicObject(method));
    }

    public static InternalMethod getMethod(RubyBasicObject method) {
        return UNBOUND_METHOD_LAYOUT.getMethod(BasicObjectNodes.getDynamicObject(method));
    }

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyUnboundMethod(other)")
        boolean equal(RubyBasicObject self, RubyBasicObject other) {
            return getMethod(self) == getMethod(other) && getOrigin(self) == getOrigin(other);
        }

        @Specialization(guards = "!isRubyUnboundMethod(other)")
        boolean equal(RubyBasicObject self, Object other) {
            return false;
        }

    }

    @CoreMethod(names = "arity")
    public abstract static class ArityNode extends CoreMethodArrayArgumentsNode {

        public ArityNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int arity(RubyBasicObject method) {
            return getMethod(method).getSharedMethodInfo().getArity().getArityNumber();
        }

    }

    @CoreMethod(names = "bind", required = 1)
    public abstract static class BindNode extends CoreMethodArrayArgumentsNode {

        @Child private MetaClassNode metaClassNode;
        @Child private CanBindMethodToModuleNode canBindMethodToModuleNode;

        public BindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            metaClassNode = MetaClassNodeGen.create(context, sourceSection, null);
            canBindMethodToModuleNode = CanBindMethodToModuleNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public RubyBasicObject bind(VirtualFrame frame, RubyBasicObject unboundMethod, Object object) {
            final RubyBasicObject objectMetaClass = metaClass(frame, object);

            if (!canBindMethodToModuleNode.executeCanBindMethodToModule(frame, getMethod(unboundMethod), objectMetaClass)) {
                CompilerDirectives.transferToInterpreter();
                final RubyBasicObject declaringModule = getMethod(unboundMethod).getDeclaringModule();
                if (RubyGuards.isRubyClass(declaringModule) && ModuleNodes.getModel(declaringModule).isSingleton()) {
                    throw new RaiseException(getContext().getCoreLibrary().typeError(
                            "singleton method called for a different object", this));
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeError(
                            "bind argument must be an instance of " + ModuleNodes.getModel(declaringModule).getName(), this));
                }
            }

            return MethodNodes.createMethod(getContext().getCoreLibrary().getMethodClass(), object, getMethod(unboundMethod));
        }

        protected RubyBasicObject metaClass(VirtualFrame frame, Object object) {
            return metaClassNode.executeMetaClass(frame, object);
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject name(RubyBasicObject unboundMethod) {
            return getSymbol(getMethod(unboundMethod).getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        public OriginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject origin(RubyBasicObject unboundMethod) {
            return getOrigin(unboundMethod);
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject owner(RubyBasicObject unboundMethod) {
            return getMethod(unboundMethod).getDeclaringModule();
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject parameters(RubyBasicObject method) {
            final ArgsNode argsNode = getMethod(method).getSharedMethodInfo().getParseTree().findFirstChild(ArgsNode.class);

            final ArgumentDescriptor[] argsDesc = Helpers.argsNodeToArgumentDescriptors(argsNode);

            return getContext().toTruffle(Helpers.argumentDescriptorsToParameters(getContext().getRuntime(),
                    argsDesc, true));
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object sourceLocation(RubyBasicObject unboundMethod) {
            SourceSection sourceSection = getMethod(unboundMethod).getSharedMethodInfo().getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return nil();
            } else {
                RubyBasicObject file = createString(sourceSection.getSource().getName());
                return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

}
