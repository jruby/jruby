/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleNodes;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

@NodeChild(value = "lexicalParentModule", type = RubyNode.class)
public abstract class DefineModuleNode extends RubyNode {

    private final String name;

    @Child LookupForExistingModuleNode lookupForExistingModuleNode;

    private final ConditionProfile needToDefineProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    public DefineModuleNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);
        this.name = name;
    }

    @Specialization(guards = "isRubyModule(lexicalParentModule)")
    public Object defineModule(VirtualFrame frame, DynamicObject lexicalParentModule) {
        final RubyConstant constant = lookupForExistingModule(frame, name, lexicalParentModule);

        final DynamicObject definingModule;

        if (needToDefineProfile.profile(constant == null)) {
            definingModule = ModuleNodes.createModule(getContext(), coreLibrary().getModuleClass(),
                    lexicalParentModule, name, this);
        } else {
            final Object constantValue = constant.getValue();

            if (!RubyGuards.isRubyModule(constantValue) || RubyGuards.isRubyClass(constantValue)) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorIsNotA(name, "module", this));
            }

            definingModule = (DynamicObject) constantValue;
        }

        return definingModule;
    }

    @Specialization(guards = "!isRubyModule(lexicalParentObject)")
    public Object defineModuleWrongParent(VirtualFrame frame, Object lexicalParentObject) {
        throw new RaiseException(coreExceptions().typeErrorIsNotA(lexicalParentObject, "module", this));
    }

    private RubyConstant lookupForExistingModule(VirtualFrame frame, String name, DynamicObject lexicalParent) {
        if (lookupForExistingModuleNode == null) {
            CompilerDirectives.transferToInterpreter();
            lookupForExistingModuleNode = insert(LookupForExistingModuleNodeGen.create(null, null));
        }
        return lookupForExistingModuleNode.executeLookupForExistingModule(frame, name, lexicalParent);
    }

}
