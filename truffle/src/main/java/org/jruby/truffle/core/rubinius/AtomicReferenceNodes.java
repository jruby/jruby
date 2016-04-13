/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import java.util.concurrent.atomic.AtomicReference;

@CoreClass(name = "Rubinius::AtomicReference")
public abstract class AtomicReferenceNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, new AtomicReference<Object>(nil()));
        }

    }

    @CoreMethod(names = "get")
    public abstract static class GetNode extends CoreMethodArrayArgumentsNode {

        public GetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object get(DynamicObject self) {
            return Layouts.ATOMIC_REFERENCE.getValue(self);
        }
    }

    @CoreMethod(names = "set", required = 1)
    public abstract static class SetNode extends CoreMethodArrayArgumentsNode {

        public SetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object set(DynamicObject self, Object value) {
            Layouts.ATOMIC_REFERENCE.setValue(self, value);
            return value;
        }
    }

    @CoreMethod(names = "compare_and_set", required = 2)
    public abstract static class CompareAndSetNode extends CoreMethodArrayArgumentsNode {

        public CompareAndSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean compareAndSet(DynamicObject self, Object expectedValue, Object value) {
            return Layouts.ATOMIC_REFERENCE.compareAndSetValue(self, expectedValue, value);
        }
    }

    @CoreMethod(names = "get_and_set", required = 1)
    public abstract static class GetAndSetNode extends CoreMethodArrayArgumentsNode {

        public GetAndSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getAndSet(DynamicObject self, Object value) {
            return Layouts.ATOMIC_REFERENCE.getAndSetValue(self, value);
        }
    }

}
