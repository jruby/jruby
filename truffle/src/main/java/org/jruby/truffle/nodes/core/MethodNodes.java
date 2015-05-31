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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgsNode;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.truffle.nodes.core.BasicObjectNodes.ReferenceEqualNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.ClassNode;
import org.jruby.truffle.nodes.objects.ClassNodeGen;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.util.EnumSet;

@CoreClass(name = "Method")
public abstract class MethodNodes {

    public static class MethodType extends BasicObjectType {

    }

    public static final MethodType METHOD_TYPE = new MethodType();

    private static final HiddenKey RECEIVER_IDENTIFIER = new HiddenKey("receiver");
    public static final Property RECEIVER_PROPERTY;

    private static final HiddenKey METHOD_IDENTIFIER = new HiddenKey("method");
    public static final Property METHOD_PROPERTY;

    private static final DynamicObjectFactory METHOD_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        RECEIVER_PROPERTY = Property.create(RECEIVER_IDENTIFIER, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        METHOD_PROPERTY = Property.create(METHOD_IDENTIFIER, allocator.locationForType(InternalMethod.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);

        final Shape shape = RubyBasicObject.LAYOUT.createShape(METHOD_TYPE)
                .addProperty(RECEIVER_PROPERTY)
                .addProperty(METHOD_PROPERTY);

        METHOD_FACTORY = shape.createFactory();
    }

    public static RubyBasicObject createMethod(RubyClass rubyClass, Object receiver, InternalMethod method) {
        return new RubyBasicObject(rubyClass, METHOD_FACTORY.newInstance(receiver, method));
    }

    public static Object getReceiver(RubyBasicObject method) {
        assert method.getDynamicObject().getShape().hasProperty(RECEIVER_IDENTIFIER);
        return RECEIVER_PROPERTY.get(method.getDynamicObject(), true);
    }

    public static InternalMethod getMethod(RubyBasicObject method) {
        assert method.getDynamicObject().getShape().hasProperty(METHOD_IDENTIFIER);
        return (InternalMethod) METHOD_PROPERTY.get(method.getDynamicObject(), true);
    }

    @CoreMethod(names = { "==", "eql?" }, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child protected ReferenceEqualNode referenceEqualNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected boolean areSame(VirtualFrame frame, Object left, Object right) {
            if (referenceEqualNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                referenceEqualNode = insert(BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
            return referenceEqualNode.executeReferenceEqual(frame, left, right);
        }

        @Specialization(guards = "isRubyMethod(b)")
        public boolean equal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return areSame(frame, getReceiver(a), getReceiver(b)) && getMethod(a) == getMethod(b);
        }

        @Specialization(guards = "!isRubyMethod(b)")
        public boolean equal(RubyBasicObject a, Object b) {
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

    @CoreMethod(names = "call", needsBlock = true, argumentsAsArray = true)
    public abstract static class CallNode extends CoreMethodArrayArgumentsNode {

        @Child private IndirectCallNode callNode;

        public CallNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        @Specialization
        public Object call(VirtualFrame frame, RubyBasicObject method, Object[] arguments, NotProvided block) {
            return doCall(frame, method, arguments, null);
        }

        @Specialization
        public Object doCall(VirtualFrame frame, RubyBasicObject method, Object[] arguments, RubyProc block) {
            // TODO(CS 11-Jan-15) should use a cache and DirectCallNode here so that we can inline - but it's
            // incompatible with our current dispatch chain.

            final InternalMethod internalMethod = getMethod(method);

            return callNode.call(frame, getMethod(method).getCallTarget(), RubyArguments.pack(
                    internalMethod,
                    internalMethod.getDeclarationFrame(),
                    getReceiver(method),
                    block,
                    arguments));
        }

    }

    @CoreMethod(names = "name")
    public abstract static class NameNode extends CoreMethodArrayArgumentsNode {

        public NameNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol name(RubyBasicObject method) {
            CompilerDirectives.transferToInterpreter();

            return getContext().getSymbol(getMethod(method).getName());
        }

    }

    @CoreMethod(names = "owner")
    public abstract static class OwnerNode extends CoreMethodArrayArgumentsNode {

        public OwnerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule owner(RubyBasicObject method) {
            return getMethod(method).getDeclaringModule();
        }

    }

    @CoreMethod(names = "parameters")
    public abstract static class ParametersNode extends CoreMethodArrayArgumentsNode {

        public ParametersNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyArray parameters(RubyBasicObject method) {
            final ArgsNode argsNode = getMethod(method).getSharedMethodInfo().getParseTree().findFirstChild(ArgsNode.class);

            final ArgumentDescriptor[] argsDesc = Helpers.argsNodeToArgumentDescriptors(argsNode);

            return (RubyArray) getContext().toTruffle(Helpers.argumentDescriptorsToParameters(getContext().getRuntime(),
                    argsDesc, true));
        }

    }

    @CoreMethod(names = "receiver")
    public abstract static class ReceiverNode extends CoreMethodArrayArgumentsNode {

        public ReceiverNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object receiver(RubyBasicObject method) {
            return getReceiver(method);
        }

    }

    @CoreMethod(names = "source_location")
    public abstract static class SourceLocationNode extends CoreMethodArrayArgumentsNode {

        public SourceLocationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sourceLocation(RubyBasicObject method) {
            CompilerDirectives.transferToInterpreter();

            SourceSection sourceSection = getMethod(method).getSharedMethodInfo().getSourceSection();

            if (sourceSection instanceof NullSourceSection) {
                return nil();
            } else {
                RubyString file = createString(sourceSection.getSource().getName());
                return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(),
                        file, sourceSection.getStartLine());
            }
        }

    }

    @CoreMethod(names = "unbind")
    public abstract static class UnbindNode extends CoreMethodArrayArgumentsNode {

        @Child private ClassNode classNode;

        public UnbindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            classNode = ClassNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyBasicObject unbind(VirtualFrame frame, RubyBasicObject method) {
            CompilerDirectives.transferToInterpreter();

            RubyClass receiverClass = classNode.executeGetClass(frame, getReceiver(method));
            return UnboundMethodNodes.createUnboundMethod(getContext().getCoreLibrary().getUnboundMethodClass(), receiverClass, getMethod(method));
        }

    }

    @CoreMethod(names = "to_proc")
    public abstract static class ToProcNode extends CoreMethodArrayArgumentsNode {

        public ToProcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyProc toProc(RubyBasicObject methodObject) {
            final InternalMethod method = getMethod(methodObject);

            return new RubyProc(
                    getContext().getCoreLibrary().getProcClass(),
                    RubyProc.Type.LAMBDA,
                    method.getSharedMethodInfo(),
                    method.getCallTarget(),
                    method.getCallTarget(),
                    method.getCallTarget(),
                    method.getDeclarationFrame(),
                    method,
                    getReceiver(methodObject),
                    null);
        }

    }

}
