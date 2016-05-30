/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

public class ReadLiteralConstantNode extends RubyNode {

    @Child RubyNode moduleNode;
    private final String name;

    @Child LookupConstantNode lookupConstantNode;
    @Child GetConstantNode getConstantNode;

    public ReadLiteralConstantNode(RubyContext context, SourceSection sourceSection, RubyNode moduleNode, String name) {
        super(context, sourceSection);
        this.moduleNode = moduleNode;
        this.name = name;
        this.lookupConstantNode = LookupConstantNodeGen.create(false, false);
        this.getConstantNode = GetConstantNode.create();
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

}
