/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import jnr.ffi.Pointer;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;

import java.util.EnumSet;

public abstract class PointerNodes {

    public static final Pointer NULL_POINTER = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().newOpaquePointer(0);

    private static final HiddenKey POINTER_IDENTIFIER = new HiddenKey("pointer");
    private static final Property POINTER_PROPERTY;
    private static final DynamicObjectFactory POINTER_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        POINTER_PROPERTY = Property.create(POINTER_IDENTIFIER, allocator.locationForType(Pointer.class, EnumSet.of(LocationModifier.NonNull)), 0);
        POINTER_FACTORY = RubyBasicObject.EMPTY_SHAPE.addProperty(POINTER_PROPERTY).createFactory();
    }

    public static class PointerAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createPointer(rubyClass, NULL_POINTER);
        }
    }

    public static RubyBasicObject createPointer(RubyClass rubyClass, Pointer pointer) {
        if (pointer == null) {
            pointer = NULL_POINTER;
        }

        return new RubyBasicObject(rubyClass, POINTER_FACTORY.newInstance(pointer));
    }

    public static void setPointer(RubyBasicObject pointer, Pointer newPointer) {
        assert newPointer != null;
        assert pointer.getDynamicObject().getShape().hasProperty(POINTER_IDENTIFIER);

        try {
            POINTER_PROPERTY.set(pointer.getDynamicObject(), newPointer, pointer.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static Pointer getPointer(RubyBasicObject pointer) {
        assert pointer.getDynamicObject().getShape().hasProperty(POINTER_IDENTIFIER);
        return (Pointer) POINTER_PROPERTY.get(pointer.getDynamicObject(), true);
    }

}
