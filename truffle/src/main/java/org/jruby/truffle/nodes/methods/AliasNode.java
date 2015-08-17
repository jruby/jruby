/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.nodes.objects.SingletonClassNodeGen;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value="module", type=RubyNode.class)
public abstract class AliasNode extends RubyNode {

    @Child private SingletonClassNode singletonClassNode;

    final String newName;
    final String oldName;

    public AliasNode(RubyContext context, SourceSection sourceSection, String newName, String oldName) {
        super(context, sourceSection);
        this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
        this.newName = newName;
        this.oldName = oldName;
    }

    @Specialization(guards = "isRubyModule(module)")
    public Object alias(DynamicObject module) {
        ModuleNodes.MODULE_LAYOUT.getFields(module).alias(this, newName, oldName);
        return module;
    }

    // TODO (eregon, 10 May 2015): we should only have the module case as the child should be the default definee
    @Specialization(guards = "!isRubyModule(object)")
    public Object alias(VirtualFrame frame, Object object) {
        ModuleNodes.MODULE_LAYOUT.getFields(singletonClassNode.executeSingletonClass(frame, object)).alias(this, newName, oldName);
        return object;
    }

}
