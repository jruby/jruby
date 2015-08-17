/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.KernelNodes;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

/**
 * Define a new module, or get the existing one of the same name.
 */
public class DefineOrGetModuleNode extends RubyNode {

    protected final String name;
    @Child private RubyNode lexicalParentModule;
    @Child private KernelNodes.RequireNode requireNode;

    public DefineOrGetModuleNode(RubyContext context, SourceSection sourceSection, String name, RubyNode lexicalParent) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParent;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // Look for a current definition of the module, or create a new one

        DynamicObject lexicalParent = getLexicalParentModule(frame);
        final RubyConstant constant = lookupForExistingModule(lexicalParent);

        DynamicObject definingModule;

        if (constant == null) {
            definingModule = ModuleNodes.createRubyModule(getContext(), getContext().getCoreLibrary().getModuleClass(), lexicalParent, name, this);
        } else {
            Object module = constant.getValue();
            if (!(RubyGuards.isRubyModule(module)) || !(RubyGuards.isRubyModule(ModuleNodes.MODULE_LAYOUT.getFields((DynamicObject) module).rubyModuleObject) && !RubyGuards.isRubyClass(ModuleNodes.MODULE_LAYOUT.getFields((DynamicObject) module).rubyModuleObject))) {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(name, "module", this));
            }
            definingModule = (DynamicObject) module;
        }

        return definingModule;
    }

    protected DynamicObject getLexicalParentModule(VirtualFrame frame) {
        final Object lexicalParent = lexicalParentModule.execute(frame);;

        if (!RubyGuards.isRubyModule(lexicalParent)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(lexicalParent.toString(), "module", this));
        }

        return (DynamicObject) lexicalParent;
    }

    @TruffleBoundary
    protected RubyConstant lookupForExistingModule(DynamicObject lexicalParent) {
        RubyConstant constant = ModuleNodes.MODULE_LAYOUT.getFields(lexicalParent).getConstants().get(name);

        final DynamicObject objectClass = getContext().getCoreLibrary().getObjectClass();

        if (constant == null && lexicalParent == objectClass) {
            for (DynamicObject included : ModuleNodes.MODULE_LAYOUT.getFields(objectClass).prependedAndIncludedModules()) {
                constant = ModuleNodes.MODULE_LAYOUT.getFields(included).getConstants().get(name);
                if (constant != null) {
                    break;
                }
            }
        }

        if (constant != null && !constant.isVisibleTo(getContext(), LexicalScope.NONE, lexicalParent)) {
            throw new RaiseException(getContext().getCoreLibrary().nameErrorPrivateConstant(lexicalParent, name, this));
        }

        // If a constant already exists with this class/module name and it's an autoload module, we have to trigger
        // the autoload behavior before proceeding.
        if ((constant != null) && constant.isAutoload()) {
            if (requireNode == null) {
                CompilerDirectives.transferToInterpreter();
                requireNode = insert(KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), null));
            }

            // We know that we're redefining this constant as we're defining a class/module with that name.  We remove
            // the constant here rather than just overwrite it in order to prevent autoload loops in either the require
            // call or the recursive execute call.
            ModuleNodes.MODULE_LAYOUT.getFields(lexicalParent).removeConstant(this, name);

            requireNode.require((DynamicObject) constant.getValue());

            return lookupForExistingModule(lexicalParent);
        }

        return constant;
    }

}
