/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Define a new module, or get the existing one of the same name.
 */
public class DefineOrGetModuleNode extends RubyNode {

    private final String name;
    @Child protected RubyNode parentModule;

    public DefineOrGetModuleNode(RubyContext context, SourceSection sourceSection, String name, RubyNode parentModule) {
        super(context, sourceSection);
        this.name = name;
        this.parentModule = adoptChild(parentModule);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();

        final RubyContext context = getContext();

        // Look for a current definition of the module, or create a new one

        RubyModule parentModuleObject;

        try {
            parentModuleObject = parentModule.executeRubyModule(frame);
        } catch (UnexpectedResultException e) {
            throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(e.getResult().toString(), "module"));
        }

        final Object constantValue = parentModuleObject.lookupConstant(name);

        RubyModule definingModule;

        if (constantValue == null) {
            definingModule = new RubyModule(context.getCoreLibrary().getModuleClass(), parentModuleObject, name);
            parentModuleObject.setConstant(name, definingModule);
            parentModuleObject.getSingletonClass().setConstant(name, definingModule);
        } else {
            if (constantValue instanceof RubyModule) {
                definingModule = (RubyModule) constantValue;
            } else {
                throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(name, "module"));
            }
        }

        return definingModule;
    }

}
