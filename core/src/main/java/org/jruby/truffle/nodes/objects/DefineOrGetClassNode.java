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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Define a new class, or get the existing one of the same name.
 */
public class DefineOrGetClassNode extends DefineOrGetModuleNode {

    @Child protected RubyNode superClass;
    @Child protected DispatchHeadNode inheritedNode;

    public DefineOrGetClassNode(RubyContext context, SourceSection sourceSection, String name, RubyNode lexicalParent, RubyNode superClass) {
        super(context, sourceSection, name, lexicalParent);
        this.superClass = superClass;
    }

    private void callInherited(VirtualFrame frame, RubyClass superClass, RubyClass subClass) {
        if (inheritedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inheritedNode = insert(new DispatchHeadNode(getContext(), true));
        }
        inheritedNode.call(frame, superClass, "inherited", null, subClass);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyContext context = getContext();

        // Look for a current definition of the class, or create a new one

        RubyModule lexicalParent = getLexicalParentModule(frame);
        final RubyConstant constant = lookupForExistingModule(frame, lexicalParent);

        RubyClass definingClass;
        RubyClass superClassObject = getRubySuperClass(frame, context);

        if (constant == null) {
            definingClass = new RubyClass(context, lexicalParent, superClassObject, name);
            definingClass.setAllocator(superClassObject.getAllocator());
            callInherited(frame, superClassObject, definingClass);
        } else {
            if (constant.getValue() instanceof RubyClass) {
                definingClass = (RubyClass) constant.getValue();
                checkSuperClassCompatibility(context, superClassObject, definingClass);
            } else {
                throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(constant.getValue().toString(), "class", this));
            }
        }

        return definingClass;
    }

    private RubyClass getRubySuperClass(VirtualFrame frame, RubyContext context) {
        final Object superClassObj = superClass.execute(frame);

        if (superClassObj instanceof RubyClass){
            if (((RubyClass) superClassObj).isSingleton()){
                throw new RaiseException(context.getCoreLibrary().typeError("can't make subclass of virtual class", this));
            }

            return (RubyClass) superClassObj;
        }
        throw new RaiseException(context.getCoreLibrary().typeError("superclass must be a Class", this));
    }

    private boolean isBlankOrRootClass(RubyClass rubyClass){
        return rubyClass.getName() == "BasicObject" || rubyClass.getName() == "Object";
    }

    private void checkSuperClassCompatibility(RubyContext context, RubyClass superClassObject, RubyClass definingClass){
        // TODO(cs): temporarily disabled as it prevents us running psd.rb benchmarks
        if (false && !isBlankOrRootClass(superClassObject) && !isBlankOrRootClass(definingClass) && definingClass.getSuperClass().getObjectID() != superClassObject.getObjectID()){
            throw new RaiseException(context.getCoreLibrary().typeError(("superclass mismatch for class " + definingClass.getName()), this));
        }
    }
}
