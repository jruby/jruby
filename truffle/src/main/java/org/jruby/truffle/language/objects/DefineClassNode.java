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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.language.LexicalScope;
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
    @Child private KernelNodes.RequireNode requireNode;

    public DefineClassNode(RubyContext context, SourceSection sourceSection, String name, RubyNode lexicalParent, RubyNode superClass) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalParentModule = lexicalParent;
        this.superClass = superClass;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final RubyContext context = getContext();

        // Look for a current definition of the class, or create a new one

        final Object lexicalParent1 = lexicalParentModule.execute(frame);;

        if (!RubyGuards.isRubyModule(lexicalParent1)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().typeErrorIsNotA(lexicalParent1.toString(), "module", this));
        }

        DynamicObject lexicalParent = (DynamicObject) lexicalParent1;
        final RubyConstant constant = lookupForExistingModule(lexicalParent);

        DynamicObject definingClass;
        final Object superClassObj = superClass.execute(frame);

        if (!RubyGuards.isRubyClass(superClassObj)) {
            throw new RaiseException(context.getCoreLibrary().typeError("superclass must be a Class", this));
        }
        if (Layouts.CLASS.getIsSingleton((DynamicObject) superClassObj)) {
            throw new RaiseException(context.getCoreLibrary().typeError("can't make subclass of virtual class", this));
        }

        DynamicObject superClassObject = (DynamicObject) superClassObj;

        if (constant == null) {
            definingClass = ClassNodes.createRubyClass(context, lexicalParent, superClassObject, name);
            assert RubyGuards.isRubyClass(superClassObject);
            assert RubyGuards.isRubyClass(definingClass);

            if (inheritedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inheritedNode = insert(DispatchHeadNodeFactory.createMethodCallOnSelf(getContext()));
            }
            inheritedNode.call(frame, superClassObject, "inherited", null, definingClass);
        } else {
            if (RubyGuards.isRubyClass(constant.getValue())) {
                definingClass = (DynamicObject) constant.getValue();
                assert RubyGuards.isRubyClass(superClassObject);
                assert RubyGuards.isRubyClass(definingClass);

                if (!isBlankOrRootClass(superClassObject) && !isBlankOrRootClass(definingClass) && ClassNodes.getSuperClass(definingClass) != superClassObject) {
                    throw new RaiseException(context.getCoreLibrary().typeError("superclass mismatch for class " + Layouts.MODULE.getFields(definingClass).getName(), this));
                }
            } else {
                throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(constant.getValue().toString(), "class", this));
            }
        }

        return definingClass;
    }

    private boolean isBlankOrRootClass(DynamicObject rubyClass) {
        assert RubyGuards.isRubyClass(rubyClass);
        return rubyClass == coreLibrary().getBasicObjectClass() || rubyClass == coreLibrary().getObjectClass();
    }

    @CompilerDirectives.TruffleBoundary
    protected RubyConstant lookupForExistingModule(DynamicObject lexicalParent) {
        RubyConstant constant = Layouts.MODULE.getFields(lexicalParent).getConstant(name);

        final DynamicObject objectClass = coreLibrary().getObjectClass();

        if (constant == null && lexicalParent == objectClass) {
            for (DynamicObject included : Layouts.MODULE.getFields(objectClass).prependedAndIncludedModules()) {
                constant = Layouts.MODULE.getFields(included).getConstant(name);
                if (constant != null) {
                    break;
                }
            }
        }

        if (constant != null && !constant.isVisibleTo(getContext(), LexicalScope.NONE, lexicalParent)) {
            throw new RaiseException(coreLibrary().nameErrorPrivateConstant(lexicalParent, name, this));
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
            Layouts.MODULE.getFields(lexicalParent).removeConstant(getContext(), this, name);

            requireNode.require((DynamicObject) constant.getValue());

            return lookupForExistingModule(lexicalParent);
        }

        return constant;
    }
}
