/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;

import java.util.concurrent.atomic.AtomicReference;

@CoreClass("Rubinius::AtomicReference")
public abstract class AtomicReferenceNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, new AtomicReference<Object>(nil()));
        }

    }

    @CoreMethod(names = "get")
    public abstract static class GetNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object get(DynamicObject self) {
            return Layouts.ATOMIC_REFERENCE.getValue(self);
        }
    }

    @CoreMethod(names = "set", required = 1)
    public abstract static class SetNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object set(DynamicObject self, Object value) {
            Layouts.ATOMIC_REFERENCE.setValue(self, value);
            return value;
        }
    }

    @CoreMethod(names = "compare_and_set", required = 2)
    public abstract static class CompareAndSetNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean compareAndSet(DynamicObject self, Object expectedValue, Object value) {
            return Layouts.ATOMIC_REFERENCE.compareAndSetValue(self, expectedValue, value);
        }
    }

    @CoreMethod(names = "get_and_set", required = 1)
    public abstract static class GetAndSetNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object getAndSet(DynamicObject self, Object value) {
            return Layouts.ATOMIC_REFERENCE.getAndSetValue(self, value);
        }
    }

}
