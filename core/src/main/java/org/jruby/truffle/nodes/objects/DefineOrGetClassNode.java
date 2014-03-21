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
 * Define a new class, or get the existing one of the same name.
 */
public class DefineOrGetClassNode extends RubyNode {

    private final String name;
    @Child protected RubyNode parentModule;
    @Child protected RubyNode superClass;

    public DefineOrGetClassNode(RubyContext context, SourceSection sourceSection, String name, RubyNode parentModule, RubyNode superClass) {
        super(context, sourceSection);
        this.name = name;
        this.parentModule = parentModule;
        this.superClass = superClass;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();

        final RubyContext context = getContext();

        RubyModule parentModuleObject;

        try {
            parentModuleObject = parentModule.executeRubyModule(frame);
        } catch (UnexpectedResultException e) {
            throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(e.getResult().toString(), "module"));
        }

        // Look for a current definition of the class, or create a new one

        final Object constantValue = parentModuleObject.lookupConstant(name);

        RubyClass definingClass;

        if (constantValue == null) {
            final RubyClass superClassObject = (RubyClass) superClass.execute(frame);

            if (superClassObject instanceof RubyException.RubyExceptionClass) {
                definingClass = new RubyException.RubyExceptionClass(superClassObject, name);
            } else if (superClassObject instanceof RubyString.RubyStringClass) {
                definingClass = new RubyString.RubyStringClass(superClassObject);
            } else {
                definingClass = new RubyClass(parentModuleObject, superClassObject, name);
            }

            parentModuleObject.setConstant(name, definingClass);
            parentModuleObject.getSingletonClass().setConstant(name, definingClass);
        } else {
            if (constantValue instanceof RubyClass) {
                definingClass = (RubyClass) constantValue;
            } else {
                throw new RaiseException(context.getCoreLibrary().typeErrorIsNotA(constantValue.toString(), "class"));
            }
        }

        return definingClass;
    }
}
