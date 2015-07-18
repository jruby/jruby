/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import org.jruby.truffle.runtime.ModuleChain;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Module} class.
 */
public class RubyModule extends RubyBasicObject implements ModuleChain {

    public final RubyModuleModel model;

    public RubyModule(RubyContext context, RubyClass selfClass, RubyModule lexicalParent, String name, Node currentNode) {
        this(context, selfClass, lexicalParent, name, currentNode, false, null);
    }

    public RubyModule(RubyContext context, RubyClass selfClass, RubyModule lexicalParent, String name, Node currentNode, boolean isSingleton, RubyModule attached) {
        super(context, selfClass);
        model = new RubyModuleModel(this, context, lexicalParent, name, new CyclicAssumption(name + " is unmodified"), isSingleton, attached);

        if (lexicalParent == null) { // bootstrap or anonymous module
            model.name = model.givenBaseName;
        } else {
            model.getAdoptedByLexicalParent(lexicalParent, name, currentNode);
        }
    }

    @Override
    public void insertAfter(RubyModule module) {
        model.insertAfter(module);
    }

    @Override
    public RubyContext getContext() {
        return model.getContext();
    }

    @Override
    public String toString() {
        return model.toString();
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        model.visitObjectGraphChildren(visitor);
    }

    @Override
    public ModuleChain getParentModule() {
        return model.getParentModule();
    }

    @Override
    public RubyModule getActualModule() {
        return model.getActualModule();
    }

}
