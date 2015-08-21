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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.RespondToNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.object.ObjectGraph;
import org.jruby.truffle.runtime.object.ObjectGraphVisitor;
import org.jruby.truffle.runtime.object.ObjectIDOperations;
import org.jruby.truffle.runtime.object.StopVisitingObjectsException;
import org.jruby.util.Memo;

@CoreClass(name = "ObjectSpace")
public abstract class ObjectSpaceNodes {

    @CoreMethod(names = "_id2ref", isModuleFunction = true, required = 1)
    public abstract static class ID2RefNode extends CoreMethodArrayArgumentsNode {

        public ID2RefNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object id2Ref(int id) {
            return id2Ref((long) id);
        }

        @TruffleBoundary
        @Specialization
        public Object id2Ref(final long id) {
            if (id == ObjectIDOperations.NIL) {
                return nil();
            } else if (id == ObjectIDOperations.TRUE) {
                return true;
            } else if (id == ObjectIDOperations.FALSE) {
                return false;
            } else if (ObjectIDOperations.isSmallFixnumID(id)) {
                return ObjectIDOperations.toFixnum(id);
            } else {
                final Memo<Object> result = new Memo<Object>(nil());

                new ObjectGraph(getContext()).visitObjects(new ObjectGraphVisitor() {

                    @Override
                    public boolean visit(DynamicObject object) throws StopVisitingObjectsException {
                        if (ObjectIDOperations.verySlowGetObjectID(object) == id) {
                            result.set(object);
                            throw new StopVisitingObjectsException();
                        }

                        return true;
                    }

                });

                return result.get();
            }
        }

        @Specialization(guards = {"isRubyBignum(id)", "isLargeFixnumID(id)"})
        public Object id2RefLargeFixnum(DynamicObject id) {
            return Layouts.BIGNUM.getValue(id).longValue();
        }

        @Specialization(guards = {"isRubyBignum(id)", "isFloatID(id)"})
        public double id2RefFloat(DynamicObject id) {
            return Double.longBitsToDouble(Layouts.BIGNUM.getValue(id).longValue());
        }

        protected boolean isLargeFixnumID(DynamicObject id) {
            return ObjectIDOperations.isLargeFixnumID(Layouts.BIGNUM.getValue(id));
        }

        protected boolean isFloatID(DynamicObject id) {
            return ObjectIDOperations.isFloatID(Layouts.BIGNUM.getValue(id));
        }

    }

    @CoreMethod(names = "each_object", isModuleFunction = true, needsBlock = true, optional = 1, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachObjectNode extends YieldingCoreMethodNode {

        public EachObjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public int eachObject(VirtualFrame frame, NotProvided ofClass, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            int count = 0;

            for (DynamicObject object : new ObjectGraph(getContext()).getObjects()) {
                if (!isHidden(object)) {
                    yield(frame, block, object);
                    count++;
                }
            }

            return count;
        }

        @Specialization(guards = {"isRubyClass(ofClass)", "isRubyProc(block)"})
        public int eachObject(VirtualFrame frame, DynamicObject ofClass, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            int count = 0;

            for (DynamicObject object : new ObjectGraph(getContext()).getObjects()) {
                if (!isHidden(object) && ModuleOperations.assignableTo(Layouts.BASIC_OBJECT.getLogicalClass(object), ofClass)) {
                    yield(frame, block, object);
                    count++;
                }
            }

            return count;
        }

        private boolean isHidden(DynamicObject object) {
            return RubyGuards.isRubyClass(object) && Layouts.CLASS.getIsSingleton(object);
        }

    }

    @CoreMethod(names = "define_finalizer", isModuleFunction = true, required = 2)
    public abstract static class DefineFinalizerNode extends CoreMethodArrayArgumentsNode {

        @Child private RespondToNode respondToNode;

        public DefineFinalizerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            respondToNode = new RespondToNode(getContext(), getSourceSection(), null, "call");
        }

        @Specialization
        public DynamicObject defineFinalizer(VirtualFrame frame, Object object, Object finalizer) {
            if (respondToNode.executeBoolean(frame, finalizer)) {
                registerFinalizer(object, finalizer);
                DynamicObject arrayClass = getContext().getCoreLibrary().getArrayClass();
                Object[] objects = new Object[]{0, finalizer};
                return ArrayNodes.createGeneralArray(arrayClass, ArrayNodes.storeFromObjects(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(arrayClass)).getContext(), objects), objects.length);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorWrongArgumentType(finalizer, "callable", this));
            }
        }

        @TruffleBoundary
        private void registerFinalizer(Object object, Object finalizer) {
            getContext().getObjectSpaceManager().defineFinalizer((DynamicObject) object, finalizer);
        }
    }

    @CoreMethod(names = "undefine_finalizer", isModuleFunction = true, required = 1)
    public abstract static class UndefineFinalizerNode extends CoreMethodArrayArgumentsNode {

        public UndefineFinalizerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object undefineFinalizer(Object object) {
            getContext().getObjectSpaceManager().undefineFinalizer((DynamicObject) object);
            return object;
        }
    }

}
