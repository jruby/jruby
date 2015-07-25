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

    public RubyModule(RubyContext context, RubyBasicObject selfClass, RubyBasicObject lexicalParent, String name, boolean isSingleton, RubyBasicObject attached) {
        super(context, selfClass);
        model = new RubyModuleModel(context, lexicalParent, name, isSingleton, attached);
        model.rubyModuleObject = this;
    }

}
