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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.ast.NilNode;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;

import java.util.ArrayList;

/**
 * Open a module and execute a method in it - probably to define new methods.
 */
public class OpenModuleNode extends RubyNode {

    @Child protected RubyNode definingModule;
    @Child protected MethodDefinitionNode definitionMethod;

    public OpenModuleNode(RubyContext context, SourceSection sourceSection, RubyNode definingModule, MethodDefinitionNode definitionMethod) {
        super(context, sourceSection);
        this.definingModule = adoptChild(definingModule);
        this.definitionMethod = adoptChild(definitionMethod);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();

        // Call the definition method with the module as self - there's no return value

        final RubyModule module = (RubyModule) definingModule.execute(frame);
        definitionMethod.executeMethod(frame).call(frame.pack(), module, null);

        return module;
    }

}
