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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class DefineClassNode extends RubyNode {

    protected final String name;

    @Child private RubyNode superClassNode;
    @Child private RubyNode lexicalParentModule;

    @Child LookupForExistingModuleNode lookupForExistingModuleNode;
    @Child CallDispatchHeadNode inheritedNode;

    private final ConditionProfile needToDefineProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    public DefineClassNode(RubyContext context, SourceSection sourceSection, String name,
                           RubyNode lexicalParent, RubyNode superClass) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParent;
        this.superClassNode = superClass;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object lexicalParentObject = lexicalParentModule.execute(frame);

        if (!RubyGuards.isRubyModule(lexicalParentObject)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorIsNotA(lexicalParentObject, "module", this));
        }

        final DynamicObject lexicalParentModule = (DynamicObject) lexicalParentObject;
        final DynamicObject superClass = executeSuperClass(frame);
        final RubyConstant constant = lookupForExistingModule(frame, name, lexicalParentModule);

        final DynamicObject definedClass;

        if (needToDefineProfile.profile(constant == null)) {
            definedClass = ClassNodes.createInitializedRubyClass(getContext(), lexicalParentModule, superClass, name);
            callInherited(frame, superClass, definedClass);
        } else {
            if (!RubyGuards.isRubyClass(constant.getValue())) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorIsNotA(constant.getValue(), "class", this));
            }

            definedClass = (DynamicObject) constant.getValue();

            final DynamicObject currentSuperClass = ClassNodes.getSuperClass(definedClass);

            if (currentSuperClass != superClass
                    && superClass != coreLibrary().getObjectClass()) { // bug-compat with MRI https://bugs.ruby-lang.org/issues/12367
                errorProfile.enter();
                throw new RaiseException(coreExceptions().superclassMismatch(
                        Layouts.MODULE.getFields(definedClass).getName(), this));
            }
        }

        return definedClass;
    }

    private DynamicObject executeSuperClass(VirtualFrame frame) {
        final Object superClassObject = superClassNode.execute(frame);

        if (!RubyGuards.isRubyClass(superClassObject)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeError("superclass must be a Class", this));
        }

        final DynamicObject superClass = (DynamicObject) superClassObject;

        if (Layouts.CLASS.getIsSingleton(superClass)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeError("can't make subclass of virtual class", this));
        }

        return superClass;
    }

    private void callInherited(VirtualFrame frame, DynamicObject superClass, DynamicObject childClass) {
        if (inheritedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
        }
        inheritedNode.call(frame, superClass, "inherited", childClass);
    }

    private RubyConstant lookupForExistingModule(VirtualFrame frame, String name, DynamicObject lexicalParent) {
        if (lookupForExistingModuleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookupForExistingModuleNode = insert(LookupForExistingModuleNodeGen.create(null, null));
        }
        return lookupForExistingModuleNode.executeLookupForExistingModule(frame, name, lexicalParent);
    }

}
