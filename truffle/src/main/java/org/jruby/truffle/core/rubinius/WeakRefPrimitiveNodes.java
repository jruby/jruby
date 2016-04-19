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
import org.jruby.truffle.core.Layouts;

import java.lang.ref.WeakReference;

public abstract class WeakRefPrimitiveNodes {

    @RubiniusPrimitive(name = "weakref_new", needsSelf = false)
    public static abstract class WeakRefNewPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject weakRefNew(Object object) {
            return Layouts.WEAK_REF_LAYOUT.createWeakRef(coreLibrary().getWeakRefFactory(), new WeakReference<Object>(object));
        }

    }

    @RubiniusPrimitive(name = "weakref_set_object")
    public static abstract class WeakRefSetObjectPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public Object weakRefSetObject(DynamicObject weakRef, Object object) {
            Layouts.WEAK_REF_LAYOUT.setReference(weakRef, new WeakReference<Object>(object));
            return object;
        }

    }

    @RubiniusPrimitive(name = "weakref_object")
    public static abstract class WeakRefObjectPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public Object weakRefObject(DynamicObject weakRef) {
            return Layouts.WEAK_REF_LAYOUT.getReference(weakRef).get();
        }

    }

}
