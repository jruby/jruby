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
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.MetaClassNode;
import org.jruby.truffle.nodes.objects.MetaClassNodeGen;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.util.EnumSet;

@CoreClass(name = "UnboundMethod")
public abstract class UnboundMethodNodes {

    public static class MethodType extends BasicObjectType {

    }

    public static final MethodType UNBOUND_METHOD_TYPE = new MethodType();

    private static final HiddenKey ORIGIN_IDENTIFIER = new HiddenKey("origin");
    public static final Property ORIGIN_PROPERTY;

    private static final HiddenKey METHOD_IDENTIFIER = new HiddenKey("method");
    public static final Property METHOD_PROPERTY;

    private static final DynamicObjectFactory UNBOUND_METHOD_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        ORIGIN_PROPERTY = Property.create(ORIGIN_IDENTIFIER, allocator.locationForType(RubyModule.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        METHOD_PROPERTY = Property.create(METHOD_IDENTIFIER, allocator.locationForType(InternalMethod.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);

        final Shape shape = RubyBasicObject.LAYOUT.createShape(UNBOUND_METHOD_TYPE)
                .addProperty(ORIGIN_PROPERTY)
                .addProperty(METHOD_PROPERTY);

        UNBOUND_METHOD_FACTORY = shape.createFactory();
    }

    public static RubyBasicObject createUnboundMethod(RubyClass rubyClass, RubyModule origin, InternalMethod method) {
        return new RubyBasicObject(rubyClass, UNBOUND_METHOD_FACTORY.newInstance(origin, method));
    }

    public static RubyModule getOrigin(RubyBasicObject method) {
        assert method.getDynamicObject().getShape().hasProperty(ORIGIN_IDENTIFIER);
        return (RubyModule) ORIGIN_PROPERTY.get(method.getDynamicObject(), true);
    }

    public static InternalMethod getMethod(RubyBasicObject method) {
        assert method.getDynamicObject().getShape().hasProperty(METHOD_IDENTIFIER);
        return (InternalMethod) METHOD_PROPERTY.get(method.getDynamicObject(), true);
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
        public RubyBasicObject bind(VirtualFrame frame, RubyBasicObject unboundMethod, Object object) {
            CompilerDirectives.transferToInterpreter();

            RubyModule module = getMethod(unboundMethod).getDeclaringModule();
            // the (redundant) instanceof is to satisfy FindBugs with the following cast
            if (module instanceof RubyClass && !ModuleOperations.canBindMethodTo(module, metaClass(frame, object))) {
                CompilerDirectives.transferToInterpreter();
                if (((RubyClass) module).isSingleton()) {
                    throw new RaiseException(getContext().getCoreLibrary().typeError("singleton method called for a different object", this));
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().typeError("bind argument must be an instance of " + module.getName(), this));
                }
            }

            return MethodNodes.createMethod(getContext().getCoreLibrary().getMethodClass(), object, getMethod(unboundMethod));
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol name(RubyBasicObject unboundMethod) {
            return getContext().getSymbol(getMethod(unboundMethod).getName());
        }

    }

    // TODO: We should have an additional method for this but we need to access it for #inspect.
    @CoreMethod(names = "origin", visibility = Visibility.PRIVATE)
    public abstract static class OriginNode extends CoreMethodArrayArgumentsNode {

        public OriginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule origin(RubyBasicObject unboundMethod) {
            return getOrigin(unboundMethod);
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule owner(RubyBasicObject unboundMethod) {
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

            return (RubyArray) getContext().toTruffle(Helpers.argumentDescriptorsToParameters(getContext().getRuntime(),
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
