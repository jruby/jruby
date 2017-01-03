/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.getStore;

@CoreClass("Truffle::Array")
public class TruffleArrayNodes {

    @CoreMethod(names = "steal_storage", onSingleton = true, required = 2)
    @ImportStatic(ArrayGuards.class)
    public abstract static class StealStorageNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "array == other")
        public DynamicObject stealStorageNoOp(DynamicObject array, DynamicObject other) {
            return array;
        }

        @Specialization(guards = {"array != other", "strategy.matches(array)", "otherStrategy.matches(other)"}, limit = "ARRAY_STRATEGIES")
        public DynamicObject stealStorage(DynamicObject array, DynamicObject other,
                        @Cached("of(array)") ArrayStrategy strategy,
                        @Cached("of(other)") ArrayStrategy otherStrategy) {
            final int size = getSize(other);
            final Object store = getStore(other);
            strategy.setStoreAndSize(array, store, size);
            otherStrategy.setStoreAndSize(other, null, 0);

            return array;
        }

    }

}
