/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.coerce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToPathNode extends RubyNode {

    public ToPathNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyString executeRubyString(VirtualFrame frame, Object object);

    @Specialization(guards = "isRubyString(path)")
    public RubyBasicObject coerceRubyString(RubyBasicObject path) {
        return path;
    }

    @Specialization(guards = "!isRubyString(object)")
    public RubyBasicObject coerceObject(VirtualFrame frame, Object object) {
        return (RubyBasicObject) ruby(frame, "Rubinius::Type.coerce_to_path(object)", "object", object);
    }

}
