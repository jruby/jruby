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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes.RequireNode;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.util.IdUtil;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({ @NodeChild("module"), @NodeChild("name"), @NodeChild("constant") })
public abstract class GetConstantNode extends RubyNode {

    private final RestartableReadConstantNode readConstantNode;

    public GetConstantNode(RubyContext context, SourceSection sourceSection, RestartableReadConstantNode readConstantNode) {
        super(context, sourceSection);
        this.readConstantNode = readConstantNode;
    }

    public abstract Object executeGetConstant(VirtualFrame frame, Object module, String name, RubyConstant constant);

    @Specialization(guards = { "constant != null", "!constant.isAutoload()" })
    protected Object getConstant(DynamicObject module, String name, RubyConstant constant) {
        return constant.getValue();
    }

    @Specialization(guards = { "constant != null", "constant.isAutoload()" })
    protected Object autoloadConstant(VirtualFrame frame, DynamicObject module, String name, RubyConstant constant,
            @Cached("createRequireNode()") RequireNode requireNode,
            @Cached("deepCopyReadConstantNode()") RestartableReadConstantNode readConstantNode) {

        final DynamicObject path = (DynamicObject) constant.getValue();

        // The autoload constant must only be removed if everything succeeds.
        // We remove it first to allow lookup to ignore it and add it back if there was a failure.
        ModuleNodes.getFields(constant.getDeclaringModule()).removeConstant(this, name);
        try {
            requireNode.require(path);
            return readConstantNode.readConstant(frame, module, name);
        } catch (RaiseException e) {
            ModuleNodes.getFields(constant.getDeclaringModule()).setAutoloadConstant(this, name, path);
            throw e;
        }
    }

    @Specialization(guards = "constant == null")
    protected Object missingConstant(VirtualFrame frame, DynamicObject module, String name, Object constant,
            @Cached("isValidConstantName(name)") boolean isValidConstantName,
            @Cached("createConstMissingNode()") CallDispatchHeadNode constMissingNode,
            @Cached("getSymbol(name)") DynamicObject symbolName) {
        if (!isValidConstantName) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().nameError(String.format("wrong constant name %s", name), name, this));
        }
        return constMissingNode.call(frame, module, "const_missing", null, symbolName);
    }

    protected RequireNode createRequireNode() {
        return KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), null);
    }

    protected RestartableReadConstantNode deepCopyReadConstantNode() {
        return (RestartableReadConstantNode) readConstantNode.deepCopy();
    }

    protected boolean isValidConstantName(String name) {
        return IdUtil.isValidConstantName19(name);
    }

    protected CallDispatchHeadNode createConstMissingNode() {
        return DispatchHeadNodeFactory.createMethodCall(getContext());
    }

}
