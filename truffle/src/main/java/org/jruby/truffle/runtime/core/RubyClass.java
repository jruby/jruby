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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;

@Deprecated
public class RubyClass extends RubyModule {

    // TODO(CS): is this compilation final needed? Is it a problem for correctness?
    @CompilationFinal
    public Allocator allocator;

    public RubyClass(RubyContext context, RubyBasicObject classClass, RubyBasicObject superclass, RubyModuleModel model, Allocator allocator) {
        super(context, classClass, model);
    }

}
