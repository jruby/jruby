/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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

    protected final String name;
    @Child protected RubyNode lexicalParentModule;

    public DefineOrGetModuleNode(RubyContext context, SourceSection sourceSection, String name, RubyNode lexicalParentModule) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParentModule;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        // Look for a current definition of the module, or create a new one

        RubyModule lexicalParent = getLexicalParentModule(frame);
        final RubyConstant constant = lookupForExistingModule(frame, lexicalParent);

        RubyModule definingModule;

        if (constant == null) {
            definingModule = new RubyModule(getContext().getCoreLibrary().getModuleClass(), lexicalParent, name);
            lexicalParent.setConstant(this, name, definingModule);
        } else {
            Object module = constant.getValue();
            if (!(module instanceof RubyModule) || !((RubyModule) module).isOnlyAModule()) {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(name, "module", this));
            }
            definingModule = (RubyModule) module;
        }

        return definingModule;
    }

    protected RubyModule getLexicalParentModule(VirtualFrame frame) {
        RubyModule lexicalParent;

        try {
            lexicalParent = lexicalParentModule.executeRubyModule(frame);
        } catch (UnexpectedResultException e) {
            throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(e.getResult().toString(), "module", this));
        }

        return lexicalParent;
    }

    protected RubyConstant lookupForExistingModule(VirtualFrame frame, RubyModule lexicalParent) {
        RubyConstant constant = lexicalParent.getConstants().get(name);

        if (constant != null && !constant.isVisibleTo(null, lexicalParent)) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(lexicalParent, name, this));
        }

        return constant;
    }

}
