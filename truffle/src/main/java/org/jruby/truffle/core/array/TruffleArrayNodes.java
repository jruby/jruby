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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.getStore;
import static org.jruby.truffle.core.array.ArrayHelpers.setStoreAndSize;

@CoreClass("Truffle::Array")
public class TruffleArrayNodes {

    @CoreMethod(names = "take_ownership_of_store", onSingleton = true, required = 2)
    public abstract static class TakeOwnershipOfStoreNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "array == other")
        public DynamicObject takeOwnershipOfStoreNoOp(DynamicObject array, DynamicObject other) {
            return array;
        }

        @Specialization(guards = { "array != other", "isRubyArray(other)" })
        public DynamicObject takeOwnershipOfStore(DynamicObject array, DynamicObject other) {
            final int size = getSize(other);
            final Object store = getStore(other);
            setStoreAndSize(array, store, size);
            setStoreAndSize(other, null, 0);

            return array;
        }

    }

}
