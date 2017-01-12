/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

/** Read a literal constant on a given module: MOD::CONST */
public class ReadConstantNode extends RubyNode {

    private final String name;

    @Child private RubyNode moduleNode;
    @Child private LookupConstantNode lookupConstantNode = LookupConstantNodeGen.create(false, false);
    @Child private GetConstantNode getConstantNode = GetConstantNode.create();

    public ReadConstantNode(RubyNode moduleNode, String name) {
        this.name = name;
        this.moduleNode = moduleNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object module = moduleNode.execute(frame);

        final RubyConstant constant = lookupConstantNode.lookupConstant(frame, module, name);

        return getConstantNode.executeGetConstant(frame, module, name, constant, lookupConstantNode);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        // TODO (eregon, 17 May 2016): We execute moduleNode twice here but we both want to make sure the LHS is defined and get the result value.
        // Possible solution: have a isDefinedAndReturnValue()?
        Object isModuleDefined = moduleNode.isDefined(frame);
        if (isModuleDefined == nil()) {
            return nil();
        }

        final Object module = moduleNode.execute(frame);
        if (!RubyGuards.isRubyModule(module)) {
            return nil();
        }

        final RubyConstant constant;
        try {
            constant = lookupConstantNode.lookupConstant(frame, module, name);
        } catch (RaiseException e) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNameErrorClass()) {
                // private constant
                return nil();
            }
            throw e;
        }

        if (constant == null) {
            return nil();
        } else {
            return create7BitString("constant", UTF8Encoding.INSTANCE);
        }
    }

    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteConstantNode(name, NodeUtil.cloneNode(moduleNode), rhs);
    }

}
