/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Reads the internal metaclass of an object.
 */
@NodeChild(value="object", type=RubyNode.class)
public abstract class MetaClassNode extends RubyNode {

    public MetaClassNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeMetaClass(VirtualFrame frame, Object value);

    @Specialization(guards = "value")
    protected DynamicObject singletonClassTrue(boolean value) {
        return getContext().getCoreLibrary().getTrueClass();
    }

    @Specialization(guards = "!value")
    protected DynamicObject singletonClassFalse(boolean value) {
        return getContext().getCoreLibrary().getFalseClass();
    }

    @Specialization
    protected DynamicObject singletonClass(int value) {
        return getContext().getCoreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject singletonClass(long value) {
        return getContext().getCoreLibrary().getFixnumClass();
    }

    @Specialization
    protected DynamicObject singletonClass(double value) {
        return getContext().getCoreLibrary().getFloatClass();
    }

    @Specialization
    protected DynamicObject singletonClass(DynamicObject object) {
        return BasicObjectNodes.BASIC_OBJECT_LAYOUT.getMetaClass(object);
    }

}
