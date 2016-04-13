/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToPathNode extends RubyNode {

    public ToPathNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization(guards = "isRubyString(path)")
    public DynamicObject coerceRubyString(DynamicObject path) {
        return path;
    }

    @Specialization(guards = "!isRubyString(object)")
    public DynamicObject coerceObject(VirtualFrame frame, Object object) {
        return (DynamicObject) ruby("Rubinius::Type.coerce_to_path(object)", "object", object);
    }

}
