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

import com.oracle.truffle.api.source.*;
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
    @Child protected RubyNode lexicalParentModule;

    public DefineOrGetModuleNode(RubyContext context, SourceSection sourceSection, String name, RubyNode lexicalParentModule) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParentModule;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyContext context = getContext();

        // Look for a current definition of the module, or create a new one

        RubyModule lexicalParentModuleObject;

        try {
            lexicalParentModuleObject = lexicalParentModule.executeRubyModule(frame);
        } catch (UnexpectedResultException e) {
            throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(e.getResult().toString(), "module", this));
        }

        final RubyConstant constant = lexicalParentModuleObject.getConstants().get(name);

        RubyModule definingModule;

        if (constant == null) {
            definingModule = new RubyModule(context.getCoreLibrary().getModuleClass(), lexicalParentModuleObject, name);
            lexicalParentModuleObject.setConstant(this, name, definingModule);
        } else {
            Object constantValue = constant.getValue();
            if (!(constantValue instanceof RubyModule) || !((RubyModule) constantValue).isOnlyAModule()) {
                throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(name, "module", this));
            }

            definingModule = (RubyModule) constantValue;
        }

        return definingModule;
    }

}
