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
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;

/**
 * Represents the Ruby {@code Module} class.
 */
@Deprecated
public class RubyModule extends RubyBasicObject {

    public final RubyModuleModel model;

    public RubyModule(RubyContext context, RubyBasicObject selfClass, RubyBasicObject lexicalParent, String name, Node currentNode) {
        this(context, selfClass, lexicalParent, name, currentNode, false, null);
    }

    public RubyModule(RubyContext context, RubyBasicObject selfClass, RubyBasicObject lexicalParent, String name, Node currentNode, boolean isSingleton, RubyBasicObject attached) {
        super(context, selfClass);

        assert lexicalParent == null || RubyGuards.isRubyModule(lexicalParent);

        model = new RubyModuleModel(this, context, lexicalParent, name, new CyclicAssumption(name + " is unmodified"), isSingleton, attached);

        if (lexicalParent == null) { // bootstrap or anonymous module
            ModuleNodes.getModel(this).name = ModuleNodes.getModel(this).givenBaseName;
        } else {
            ModuleNodes.getModel(this).getAdoptedByLexicalParent(lexicalParent, name, currentNode);
        }
    }
    
    public RubyContext getContext() {
        return ModuleNodes.getModel(this).getContext();
    }

    @Override
    public String toString() {
        return ModuleNodes.getModel(this).toString();
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        ModuleNodes.getModel(this).visitObjectGraphChildren(visitor);
    }

}
