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
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.language.RubyConstant;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class DefineClassNode extends RubyNode {

    protected final String name;

    @Child private RubyNode superClass;
    @Child private CallDispatchHeadNode inheritedNode;
    @Child private RubyNode lexicalParentModule;
    @Child private IndirectCallNode indirectCallNode;

    private final ConditionProfile needToDefineProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    public DefineClassNode(RubyContext context, SourceSection sourceSection, String name,
                           RubyNode lexicalParent, RubyNode superClass) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParent;
        this.superClass = superClass;
        indirectCallNode = IndirectCallNode.create();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object lexicalParentObject = lexicalParentModule.execute(frame);;

        if (!RubyGuards.isRubyModule(lexicalParentObject)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeErrorIsNotA(lexicalParentObject, "module", this));
        }

        DynamicObject lexicalParentModule = (DynamicObject) lexicalParentObject;

        final RubyConstant constant = DefineModuleNode.lookupForExistingModule(
                frame, getContext(), name, lexicalParentModule, indirectCallNode);

        final DynamicObject definingClass;
        final Object superClassObject = superClass.execute(frame);

        if (!RubyGuards.isRubyClass(superClassObject)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeError("superclass must be a Class", this));
        }

        final DynamicObject superClassModule = (DynamicObject) superClassObject;

        if (Layouts.CLASS.getIsSingleton(superClassModule)) {
            errorProfile.enter();
            throw new RaiseException(coreExceptions().typeError("can't make subclass of virtual class", this));
        }

        if (needToDefineProfile.profile(constant == null)) {
            definingClass = ClassNodes.createInitializedRubyClass(getContext(), lexicalParentModule, superClassModule, name);

            if (inheritedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inheritedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }

            inheritedNode.call(frame, superClassModule, "inherited", null, definingClass);
        } else {
            if (!RubyGuards.isRubyClass(constant.getValue())) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorIsNotA(constant.getValue(), "class", this));
            }

            definingClass = (DynamicObject) constant.getValue();

            if (!isBlankOrRootClass(superClassModule) && !isBlankOrRootClass(definingClass)
                    && ClassNodes.getSuperClass(definingClass) != superClassModule) {
                errorProfile.enter();

                throw new RaiseException(coreExceptions().superclassMismatch(
                        Layouts.MODULE.getFields(definingClass).getName(), this));
            }
        }

        return definingClass;
    }

    private boolean isBlankOrRootClass(DynamicObject rubyClass) {
        return rubyClass == coreLibrary().getBasicObjectClass() || rubyClass == coreLibrary().getObjectClass();
    }

}
