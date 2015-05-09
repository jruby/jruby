/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;

/**
 * Reads the class of an object.
 */
@NodeChild(value="object", type=RubyNode.class)
public abstract class ClassNode extends RubyNode {

    public ClassNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyClass executeGetClass(VirtualFrame frame, Object value);

    @Specialization(guards = "isTrue(value)")
    protected RubyClass getClassTrue(boolean value) {
        return getContext().getCoreLibrary().getTrueClass();
    }

    @Specialization(guards = "!isTrue(value)")
    protected RubyClass getClassFalse(boolean value) {
        return getContext().getCoreLibrary().getFalseClass();
    }

    @Specialization
    protected RubyClass getClass(int value) {
        return getContext().getCoreLibrary().getFixnumClass();
    }

    @Specialization
    protected RubyClass getClass(long value) {
        return getContext().getCoreLibrary().getFixnumClass();
    }

    @Specialization
    protected RubyClass getClass(double value) {
        return getContext().getCoreLibrary().getFloatClass();
    }

    @Specialization
    protected RubyClass getClass(RubyBasicObject object) {
        return object.getLogicalClass();
    }

}
