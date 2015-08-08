/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes.RequireNode;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.util.IdUtil;

@NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("constant") })
public abstract class GetConstantNode extends RubyNode {

    public GetConstantNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeGetConstant(VirtualFrame frame, Object module, String name, RubyConstant constant);

    @Specialization(guards = { "constant != null", "!constant.isAutoload()" })
    protected Object getConstant(RubyBasicObject module, String name, RubyConstant constant) {
        return constant.getValue();
    }

    @Specialization(guards = "constant == null")
    protected Object missingConstant(VirtualFrame frame, RubyBasicObject module, String name, Object constant,
            @Cached("isValidConstantName(name)") boolean isValidConstantName,
            @Cached("createConstMissingNode()") CallDispatchHeadNode constMissingNode,
            @Cached("getSymbol(name)") RubyBasicObject symbolName) {
        if (!isValidConstantName) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), name, this));
        }
        return constMissingNode.call(frame, module, "const_missing", null, symbolName);
    }

    protected boolean isValidConstantName(String name) {
        return IdUtil.isValidConstantName19(name);
    }

    protected CallDispatchHeadNode createConstMissingNode() {
        return DispatchHeadNodeFactory.createMethodCall(getContext());
    }

}
