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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.ObjectIDOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;

import java.util.Collection;

@CoreClass(name = "ObjectSpace")
public abstract class ObjectSpaceNodes {

    @CoreMethod(names = "_id2ref", isModuleFunction = true, required = 1)
    public abstract static class ID2RefNode extends CoreMethodNode {

        public ID2RefNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ID2RefNode(ID2RefNode prev) {
            super(prev);
        }

        @Specialization
        public Object id2Ref(int id) {
            return id2Ref((long) id);
        }

        @Specialization
        public Object id2Ref(long id) {
            notDesignedForCompilation();

            if (id == ObjectIDOperations.NIL) {
                return getContext().getCoreLibrary().getNilObject();
            } else if (id == ObjectIDOperations.TRUE) {
                return true;
            } else if (id == ObjectIDOperations.FALSE) {
                return false;
            } else if (ObjectIDOperations.isSmallFixnumID(id)) {
                return ObjectIDOperations.toFixnum(id);
            } else {
                final Object object = getContext().getObjectSpaceManager().collectLiveObjects().get(id);

                if (object == null) {
                    return getContext().getCoreLibrary().getNilObject();
                } else {
                    return object;
                }
            }
        }

        @Specialization(guards = "isLargeFixnumID")
        public Object id2RefLargeFixnum(RubyBignum id) {
            return ObjectIDOperations.toFixnum(id);
        }

        @Specialization(guards = "isFloatID")
        public double id2RefFloat(RubyBignum id) {
            return ObjectIDOperations.toFloat(id);
        }

        protected boolean isLargeFixnumID(RubyBignum id) {
            return ObjectIDOperations.isLargeFixnumID(id);
        }

        protected boolean isFloatID(RubyBignum id) {
            return ObjectIDOperations.isFloatID(id);
        }

    }

    @CoreMethod(names = "each_object", isModuleFunction = true, needsBlock = true, optional = 1)
    public abstract static class EachObjectNode extends YieldingCoreMethodNode {

        public EachObjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachObjectNode(EachObjectNode prev) {
            super(prev);
        }

        @Specialization
        public int eachObject(VirtualFrame frame, @SuppressWarnings("unused") UndefinedPlaceholder ofClass, RubyProc block) {
            notDesignedForCompilation();

            final Collection<RubyBasicObject> liveObjects = getContext().getObjectSpaceManager().collectLiveObjects().values();

            for (RubyBasicObject object : liveObjects) {
                yield(frame, block, object);
            }

            return liveObjects.size();
        }

        @Specialization
        public int eachObject(VirtualFrame frame, RubyClass ofClass, RubyProc block) {
            notDesignedForCompilation();

            int count = 0;

            for (RubyBasicObject object : getContext().getObjectSpaceManager().collectLiveObjects().values()) {
                if (ModuleOperations.assignableTo(object.getLogicalClass(), ofClass)) {
                    yield(frame, block, object);
                    count++;
                }
            }

            return count;
        }

    }

    @CoreMethod(names = "define_finalizer", isModuleFunction = true, required = 2)
    public abstract static class DefineFinalizerNode extends CoreMethodNode {

        public DefineFinalizerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefineFinalizerNode(DefineFinalizerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray defineFinalizer(Object object, RubyProc finalizer) {
            notDesignedForCompilation();

            getContext().getObjectSpaceManager().defineFinalizer((RubyBasicObject) object, finalizer);
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), 0, finalizer);
        }
    }

    @CoreMethod(names = "undefine_finalizer", isModuleFunction = true, required = 1)
    public abstract static class UndefineFinalizerNode extends CoreMethodNode {

        public UndefineFinalizerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UndefineFinalizerNode(UndefineFinalizerNode prev) {
            super(prev);
        }

        @Specialization
        public Object undefineFinalizer(Object object) {
            notDesignedForCompilation();

            getContext().getObjectSpaceManager().undefineFinalizer((RubyBasicObject) object);
            return object;
        }
    }

}
