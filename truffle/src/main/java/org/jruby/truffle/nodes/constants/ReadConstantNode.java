/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

@NodeChildren({ @NodeChild("module"), @NodeChild("name") })
public abstract class ReadConstantNode extends RubyNode {

    @Child protected LookupConstantNode lookupConstantNode;
    @Child private GetConstantNode getConstantNode;

    @Child private RequireNode requireNode;

    public ReadConstantNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.lookupConstantNode = LookupConstantNodeGen.create(context, sourceSection, lexicalScope, null, null);
        this.getConstantNode = GetConstantNodeGen.create(context, sourceSection, null, null, null);
    }

    public abstract RubyNode getName();
    public abstract RubyNode getModule();

    public abstract Object executeReadConstant(VirtualFrame frame, Object module, String name);

    @Specialization
    public Object readConstant(VirtualFrame frame, Object module, String name) {
        final RubyConstant constant = lookupConstantNode.executeLookupConstant(frame, module, name);

        if (constant != null && constant.isAutoload()) {
            CompilerDirectives.transferToInterpreter();
            return autoload(frame, module, name, constant);
        }

        return getConstantNode.executeGetConstant(frame, module, name, constant);
    }

    protected Object autoload(VirtualFrame frame, Object module, String name, RubyConstant constant) {

        final RubyBasicObject path = (RubyBasicObject) constant.getValue();

        // The autoload constant must only be removed if everything succeeds.
        // We remove it first to allow lookup to ignore it and add it back if there was a failure.
        ModuleNodes.getModel(constant.getDeclaringModule()).removeConstant(this, name);
        try {
            require(path);
            return readConstant(frame, module, name);
        } catch (RaiseException e) {
            ModuleNodes.getModel(constant.getDeclaringModule()).setAutoloadConstant(this, name, path);
            throw e;
        }
    }

    private boolean require(RubyBasicObject feature) {
        if (requireNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            requireNode = insert(KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), null));
        }
        return requireNode.require(feature);
    }

}
