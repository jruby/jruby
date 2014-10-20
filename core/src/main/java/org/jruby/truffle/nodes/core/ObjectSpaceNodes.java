/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.math.*;
import java.util.Collection;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "ObjectSpace")
public abstract class ObjectSpaceNodes {

    @CoreMethod(names = "_id2ref", isModuleFunction = true, minArgs = 1, maxArgs = 1)
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

            if (ObjectIDOperations.isNil(id)) {
                return getContext().getCoreLibrary().getNilObject();
            } else if (ObjectIDOperations.isTrue(id)) {
                return true;
            } else if (ObjectIDOperations.isFalse(id)) {
                return false;
            } else if (ObjectIDOperations.isFixnum(id)) {
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

    }

    @CoreMethod(names = "each_object", isModuleFunction = true, needsBlock = true, minArgs = 0, maxArgs = 1)
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

    @CoreMethod(names = "define_finalizer", isModuleFunction = true, minArgs = 2, maxArgs = 2)
    public abstract static class DefineFinalizerNode extends CoreMethodNode {

        public DefineFinalizerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DefineFinalizerNode(DefineFinalizerNode prev) {
            super(prev);
        }

        @Specialization
        public RubyProc defineFinalizer(Object object, RubyProc finalizer) {
            notDesignedForCompilation();

            getContext().getObjectSpaceManager().defineFinalizer((RubyBasicObject) object, finalizer);
            return finalizer;
        }
    }

    @CoreMethod(names = "garbage_collect", isModuleFunction = true, maxArgs = 0)
    public abstract static class GarbageCollectNode extends GCNodes.GarbageCollectNode {
        public GarbageCollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GarbageCollectNode(GarbageCollectNode prev) {
            super(prev);
        }
    }

    @CoreMethod(names = "undefine_finalizer", isModuleFunction = true, minArgs = 1, maxArgs = 1)
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
