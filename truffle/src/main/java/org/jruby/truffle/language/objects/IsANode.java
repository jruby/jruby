/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChildren({
        @NodeChild("instance"),
        @NodeChild("module")
})
public abstract class IsANode extends RubyNode {

    @Child private MetaClassNode metaClassNode;

    public abstract boolean executeIsA(Object self, DynamicObject module);

    @Specialization(
            limit = "getCacheLimit()",
            guards = {
                    "isRubyModule(cachedModule)",
                    "getMetaClass(self) == cachedMetaClass",
                    "module == cachedModule"
            },
            assumptions = "getUnmodifiedAssumption(cachedModule)")
    public boolean isACached(Object self,
            DynamicObject module,
            @Cached("getMetaClass(self)") DynamicObject cachedMetaClass,
            @Cached("module") DynamicObject cachedModule,
            @Cached("isA(cachedMetaClass, cachedModule)") boolean result) {
        return result;
    }

    public Assumption getUnmodifiedAssumption(DynamicObject module) {
        return Layouts.MODULE.getFields(module).getUnmodifiedAssumption();
    }

    @Specialization(guards = "isRubyModule(module)", contains = "isACached")
    public boolean isAUncached(Object self, DynamicObject module) {
        return isA(getMetaClass(self), module);
    }

    @Specialization(guards = "!isRubyModule(module)")
    public boolean isATypeError(Object self, DynamicObject module) {
        throw new RaiseException(coreExceptions().typeError("class or module required", this));
    }

    @TruffleBoundary
    protected boolean isA(DynamicObject metaClass, DynamicObject module) {
        return ModuleOperations.assignableTo(metaClass, module);
    }

    protected DynamicObject getMetaClass(Object object) {
        if (metaClassNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            metaClassNode = insert(MetaClassNodeGen.create(null));
        }

        return metaClassNode.executeMetaClass(object);
    }

    protected int getCacheLimit() {
        return getContext().getOptions().IS_A_CACHE;
    }

}