/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.CoreClass;
import org.jruby.truffle.nodes.core.CoreMethod;
import org.jruby.truffle.nodes.core.CoreMethodNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;

@CoreClass(name = "Type")
public abstract class TypeNodes {
    @CoreMethod(names = "object_kind_of?", onSingleton = true)
    public abstract static class ObjKindOfPNode extends CoreMethodNode {
        public ObjKindOfPNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ObjKindOfPNode(ObjKindOfPNode prev) {
            super(prev);
        }

        @Specialization
        public boolean obj_kind_of_p(RubyBasicObject obj, RubyClass cls) {
            return obj.getLogicalClass() == cls;
        }
    }
}
