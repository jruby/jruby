/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.objects.ObjectGraph;

import java.util.Set;

@CoreClass("Truffle::ObjSpace")
public abstract class ObjSpaceNodes {

    @CoreMethod(names = "memsize_of", isModuleFunction = true, required = 1)
    public abstract static class MemsizeOfNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isRubyArray(object)")
        public int memsizeOfArray(DynamicObject object) {
            return memsizeOfObject(object) + Layouts.ARRAY.getSize(object);
        }

        @Specialization(guards = "isRubyHash(object)")
        public int memsizeOfHash(DynamicObject object) {
            return memsizeOfObject(object) + Layouts.HASH.getSize(object);
        }

        @Specialization(guards = "isRubyString(object)")
        public int memsizeOfString(DynamicObject object) {
            return memsizeOfObject(object) + StringOperations.rope(object).byteLength();
        }

        @Specialization(guards = "isRubyMatchData(object)")
        public int memsizeOfMatchData(DynamicObject object) {
            return memsizeOfObject(object) + Layouts.MATCH_DATA.getValues(object).length;
        }

        @Specialization(guards = {
                "!isNil(object)",
                "!isRubyArray(object)",
                "!isRubyHash(object)",
                "!isRubyString(object)",
                "!isRubyMatchData(object)"
        })
        public int memsizeOfObject(DynamicObject object) {
            return 1 + object.getShape().getPropertyListInternal(false).size();
        }

        @Specialization(guards = "!isDynamicObject(object)")
        public int memsize(Object object) {
            return 0;
        }

    }

    @CoreMethod(names = "adjacent_objects", isModuleFunction = true, required = 1)
    public abstract static class AdjacentObjectsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject adjacentObjects(DynamicObject object) {
            final Set<DynamicObject> objects = ObjectGraph.getAdjacentObjects(object);
            return createArray(objects.toArray(), objects.size());
        }

    }

    @CoreMethod(names = "root_objects", isModuleFunction = true)
    public abstract static class RootObjectsNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject rootObjects() {
            final Set<DynamicObject> objects = ObjectGraph.stopAndGetRootObjects(this, getContext());
            return createArray(objects.toArray(), objects.size());
        }

    }

    @CoreMethod(names = "trace_allocations_start", isModuleFunction = true)
    public abstract static class TraceAllocationsStartNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject traceAllocationsStart() {
            getContext().getObjectSpaceManager().traceAllocationsStart();
            return nil();
        }

    }

    @CoreMethod(names = "trace_allocations_stop", isModuleFunction = true)
    public abstract static class TraceAllocationsStopNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject traceAllocationsStop() {
            getContext().getObjectSpaceManager().traceAllocationsStop();
            return nil();
        }

    }

}
